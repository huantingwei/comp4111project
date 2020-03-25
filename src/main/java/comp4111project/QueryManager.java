package comp4111project;

import comp4111project.Model.Book;

import javax.xml.transform.Result;

import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class QueryManager {
    private DBConnection connectionPool;
    private final static ConcurrentHashMap<String, String> user_token = new ConcurrentHashMap<String, String>();
    private final static ConcurrentHashMap<String, String> token_user = new ConcurrentHashMap<String, String>();

	private String BOOKTABLE = "book";
	private String USERTABLE = "user";
	private String USERNAME = "Username";
	private String PASSWORD = "Password";
	private String ID = "bookid";
	private String TITLE = "Title";
	private String AUTHOR = "Author";
	private String PUBLISHER = "Publisher";
	private String YEAR = "Year";
	private String AVAILABLE = "Available";
	
    private QueryManager() {
        {
            try {
                connectionPool = new DBConnection("connection.prop");
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private static class BillPushSingleton {
        private static final QueryManager INSTANCE = new QueryManager();
    }

    public static QueryManager getInstance() {
        return BillPushSingleton.INSTANCE;
    }

    public Boolean validateToken(String token) {
        return token_user.get(token) != null;
    }

    public Boolean validateUser(String username) {
    	return user_token.get(username) != null;
    }

    public String getUserFromToken(String token) {
    	return token_user.get(token);
    }
    public void removeUserAndToken(String token) {
    	String usr = token_user.get(token);
    	token_user.remove(token);
    	user_token.remove(usr);
    }
    public void addUserAndToken(String user, String token) {
    	token_user.put(token, user);
    	user_token.put(user, token);
    }
    
    public Vector getBooks(ConcurrentHashMap<String, String> queryPairs) {
        queryPairs.remove("token");
        Vector<Book> books = new Vector<>();
        String searchQuery;
        try {
            Connection conn = connectionPool.getConnection();
            if(queryPairs.isEmpty()) {
                searchQuery = "SELECT * FROM book";
            } else {
                searchQuery = "SELECT * FROM book WHERE";
                for (ConcurrentHashMap.Entry<String, String> entry : queryPairs.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();

                    if(key.equals("author")) {
                        searchQuery += " " + capitalize(key) + " LIKE" + " '%" + value + "%'" + " AND";
                    } else if(key.equals("id") || key.equals("title") || key.equals("publisher") || key.equals("year")) {
                        searchQuery += " " + capitalize(key) + " =" + " '" + value +"'" + " AND";
                    }
                }
                searchQuery = searchQuery.substring(0, searchQuery.length() - 3);
                if(queryPairs.containsKey("sortby")) {
                    searchQuery += " " + "ORDER BY " + queryPairs.get("sortby");
                }
                if(queryPairs.containsKey("order")) {
                    searchQuery += " " + queryPairs.get("order");
                }
                if(queryPairs.containsKey("limit")) {
                    searchQuery += " LIMIT " + queryPairs.get("limit");
                }
            }

            System.out.println(searchQuery);
            PreparedStatement searchStmt = conn.prepareStatement(searchQuery);
            ResultSet rs = searchStmt.executeQuery();

            while(rs.next()) {
                String bookID = rs.getString("id");
                String title = rs.getString("title");
                String bookAuthor = rs.getString("author");
                String publisher = rs.getString("publisher");
                int year = rs.getInt("year");
                System.out.println(title);
                Book foundBook = new Book(Integer.parseInt(bookID), title, bookAuthor, publisher, year);
                books.add(foundBook);
            }
            searchStmt.close();
            rs.close();
            connectionPool.closeConnection(conn);
            return books;
        } catch (Exception e) {
            System.err.println("Got an exception!");
            System.err.println(e.getMessage());
        }

        return books;
    }

    private static String capitalize(String str) {
        if(str == null || str.isEmpty()) {
            return str;
        }

        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    // Returns an integer depending on the status: 0 - 200 OK 1 - No book record 2 - bad request
    public int returnAndLoanBook(String bookID, Boolean isReturningBook) {
        String updateQuery;
        try {
            Connection conn = connectionPool.getConnection();
            String searchQuery = "SELECT available from book WHERE bookid =" + " '" + bookID + "' ";
            PreparedStatement searchBookStmt = conn.prepareStatement(searchQuery);
            ResultSet rs = searchBookStmt.executeQuery();

            if (rs.next()) {
                if(rs.getBoolean("available") == (isReturningBook ? false : true)) {
                    System.out.println("ready to be returned/loaned");
                    try {
                        if(isReturningBook) {
                            updateQuery = "UPDATE book SET available = '1' WHERE bookid =" + " '" + bookID + "' ";
                        } else {
                            updateQuery = "UPDATE book SET available = '0' WHERE bookid =" + " '" + bookID + "' ";
                        }
                        PreparedStatement updateAvailabilityStmt = conn.prepareStatement(updateQuery);

                        int result = updateAvailabilityStmt.executeUpdate();

                        rs.close();
                        updateAvailabilityStmt.close();
                        connectionPool.closeConnection(conn);

                        if(result == 1) {
                            return 0; // OK
                        } else {
                            return 2; // Bad Request
                        }

                    } catch(Exception ex) {
                        return 2; // Bad Request
                    } finally {
                        rs.close();
                        connectionPool.closeConnection(conn);
                    }

                } else {
                    System.out.println("Book already returned/loaned");
                    return 2; // The book is already returned
                }
            } else {
                System.out.println("no record");
                return 1; // No book record
            }

        } catch(Exception ex) {
            System.out.println("error " + ex);
            return 2; // Bad Request
        }
    }
    
    /**
     * This method add a book into the database
     * @param book
     * @return newBookID if a new book can be added; -1 if book already exists; -2 if bad requests
     */
    public int addBook(ConcurrentHashMap<String, Object> book) {
    	try {
			if(bookExist(book)) {
				return -1;
			}
			// does not exist
			// add book
			else {		
				Connection conn = connectionPool.getConnection();
				String title = (String) book.get(TITLE);
				String author = (String) book.get(AUTHOR);
				String publisher = (String) book.get(PUBLISHER);
				int year = (Integer) book.get(YEAR);
				
				String addBookQuery ="INSERT INTO " + BOOKTABLE + "(" + TITLE + "," + AUTHOR + "," + PUBLISHER + "," + YEAR + "," + AVAILABLE + ")"
				        + " VALUES (?, ?, ?, ?, ?)";

				PreparedStatement insertStmt = conn.prepareStatement(addBookQuery, Statement.RETURN_GENERATED_KEYS);
				insertStmt.setString (1, title);
				insertStmt.setString (2, author);
				insertStmt.setString (3, publisher);
				insertStmt.setInt (4, year);
				insertStmt.setBoolean (5, true);

				insertStmt.executeUpdate();

				try (ResultSet generatedKeys = insertStmt.getGeneratedKeys()) {
					int newID;
					// insert successfully
		            if (generatedKeys.next()) {
		                newID = generatedKeys.getInt(1);
		            }
		            // unsuccessful insert
		            else {
		            	newID = -2;
		            }
		            generatedKeys.close();
	                insertStmt.close();
	                connectionPool.closeConnection(conn);
	                return newID;
		        }
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
    	// bad request
    	return -2;
    }

    public Boolean deleteBook(int id) {
    	
    	if(bookExist(id)) {
    		try {
				Connection conn = connectionPool.getConnection();
				String deleteBookQuery ="DELETE FROM " + BOOKTABLE + " WHERE " + ID + "= ?";
		    	PreparedStatement deleteBookStmt = conn.prepareStatement(deleteBookQuery);
		    	deleteBookStmt.setInt (1, id);
		    	deleteBookStmt.executeUpdate();
		    	deleteBookStmt.close();
		    	connectionPool.closeConnection(conn);
		    	return true;
		    	
			} catch (SQLException e) {
				e.printStackTrace();
			}
    		return false;
    	}
    	else
    		return false;
    }
    
    public int loginUser(ConcurrentHashMap<String, Object> user) {
    	String usr = (String) user.get("Username");
		String pwd = (String) user.get("Password");
		
		// if user has already logged in
    	if(validateUser(usr)) {
    		return -1;
    	}
    	else {
    		try {
				Connection conn = connectionPool.getConnection();
				
				String checkUserQuery = "SELECT 1 FROM " + USERTABLE  + " WHERE "
										+  USERNAME + "=" + " ? " + "AND " + PASSWORD + "=" + " ? ";
				PreparedStatement stmt = conn.prepareStatement(checkUserQuery);
				stmt.setString(1, usr);
				stmt.setString(2, pwd);
				ResultSet rs = stmt.executeQuery();
				
				int result;
				// correct username and password
				if(rs.next()) {
					result = 1; 
					
				}
				// incorrect username and password
				else {
					result = -2;
				}
				rs.close();
				stmt.close();
				connectionPool.closeConnection(conn);
				return result;
				
			} catch (SQLException e) {
				e.printStackTrace();
			}
    		return -2;
    	}	
    }
    /**
     * This method checks if the book exists in the database using book id
     * @param int bookID
     * @return true if book already exists; false if no book exists or unsuccessful query
     */
    private Boolean bookExist(int bookID) {
    	try {
    		Connection conn = connectionPool.getConnection();
    		String findBookQuery =
				"SELECT * FROM " + BOOKTABLE  + " WHERE "+  ID + "=?" ;
    		PreparedStatement findBookStmt = conn.prepareStatement(findBookQuery);
    		findBookStmt.setInt (1, bookID);
    		findBookStmt.execute();
    		ResultSet rs = findBookStmt.getResultSet();
    		Boolean exist = rs.next();
    		
    		rs.close();
    		findBookStmt.close();
    		connectionPool.closeConnection(conn);
    		
    		return exist;
    		
    	} catch(SQLException e){
    		e.printStackTrace();
    	}
    	return false;
	
    }
    /**
     * This method checks if the book exists in the database using full book data
     * @param ConcurrentHashMap<String, Object> book
     * @return true if book already exists; false if no book exists or unsuccessful query
     */
    private Boolean bookExist(ConcurrentHashMap<String, Object> book) {
    	try {
			Connection conn = connectionPool.getConnection();
			
			String title = (String) book.get(TITLE);
			String author = (String) book.get(AUTHOR);
			String publisher = (String) book.get(PUBLISHER);
			int year = (Integer) book.get(YEAR);
			
			String query = "SELECT " + ID + " FROM " + BOOKTABLE + " WHERE " 
					+ TITLE + "= ? AND " + AUTHOR + "= ? AND " + PUBLISHER + "= ? AND " + YEAR + "= ?; ";
			PreparedStatement searchStmt = conn.prepareStatement(query);
			searchStmt.setString(1, title);
			searchStmt.setString(2, author);
			searchStmt.setString(3, publisher);
			searchStmt.setInt(4, year);
			ResultSet searchRs = searchStmt.executeQuery();

			// already exist
			Boolean exist = searchRs.next();
			searchStmt.close();
			searchRs.close();
			connectionPool.closeConnection(conn);
			
			return exist;
    	} catch (SQLException e) {
    		e.printStackTrace();
    	}
    	return false;
    }
}
