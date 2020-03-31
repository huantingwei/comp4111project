package comp4111project;

import comp4111project.Model.Book;


import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

public class QueryManager {
	private String DBConfigFile = "connection.prop";
    private DBConnection connectionPool;
    
    
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
                connectionPool = new DBConnection(DBConfigFile);
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
    
    public Vector<Book> getBooks(ConcurrentHashMap<String, String> queryPairs) {
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
                    } else if(key.equals("id")) {
						searchQuery += " bookid LIKE" + " '%" + value + "%'" + " AND";
					} else if(key.equals("title") || key.equals("publisher") || key.equals("year")) {
                        searchQuery += " " + capitalize(key) + " =" + " '" + value +"'" + " AND";
                    }
                }
                searchQuery = searchQuery.substring(0, searchQuery.length() - 3);
                if(queryPairs.containsKey("sortby")) {
                	if(queryPairs.get("sortby").equals("id")) {
						searchQuery += " " + "ORDER BY bookid";
					} else {
						searchQuery += " " + "ORDER BY " + queryPairs.get("sortby");
					}

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
                String bookID = rs.getString(ID);
                String title = rs.getString(TITLE);
                String bookAuthor = rs.getString(AUTHOR);
                String publisher = rs.getString(PUBLISHER);
                int year = rs.getInt(YEAR);
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
            String searchQuery = "SELECT * FROM " 
            					+ BOOKTABLE + " WHERE " + ID + " = " + " '" + bookID + "' ";
            PreparedStatement searchBookStmt = conn.prepareStatement(searchQuery);
            ResultSet rs = searchBookStmt.executeQuery();

            if (rs.next()) {
                if(rs.getBoolean(AVAILABLE) == (isReturningBook ? false : true)) {
                    System.out.println("ready to be returned/loaned");
                    try {
                        if(isReturningBook) {
                            updateQuery = "UPDATE " + BOOKTABLE + " SET " + AVAILABLE + " = '1' WHERE " + ID + " = " + " '" + bookID + "' ";
                        } else {
                            updateQuery = "UPDATE " + BOOKTABLE + " SET " + AVAILABLE + " = '0' WHERE " + ID + " = " + " '" + bookID + "' ";
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
    public long addBook(ConcurrentHashMap<String, Object> book) {
    	try {
    		long existID = bookExist(book);
			if(existID != -1) {
				return -existID;
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
					long newID;
					// insert successfully
		            if (generatedKeys.next()) {
		                newID = generatedKeys.getLong(1);
		            }
		            // unsuccessful insert
		            else {
		            	newID = 0;
		            }
		            generatedKeys.close();
	                insertStmt.close();
	                connectionPool.closeConnection(conn);
	                return newID;
		        } finally {
	                insertStmt.close();
	                connectionPool.closeConnection(conn);
		        }
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
    	// bad request
    	return 0;
    }
    /**
     * This function delete the book record with specified book id
     * @param id
     * @return 1: success; -1: query fail
     */
    public int deleteBook(String id) {
    	
    	long bookID = Long.parseLong(id);
    	if(bookExist(bookID) != -1) {
    		try {
				Connection conn = connectionPool.getConnection();
				String deleteBookQuery ="DELETE FROM " + BOOKTABLE + " WHERE " + ID + "= ?";
		    	PreparedStatement deleteBookStmt = conn.prepareStatement(deleteBookQuery);
		    	deleteBookStmt.setLong (1, bookID);
		    	deleteBookStmt.executeUpdate();
		    	deleteBookStmt.close();
		    	connectionPool.closeConnection(conn);
		    	return 1;
		    	
			} catch (SQLException e) {
				e.printStackTrace();
			}
    		return -1;
    	}
    	else
    		System.out.println(Long.toString(bookID) + " book doesn't exist...");
    		return -1;
    }
    
    /**
     * This function checks if the user exist and the username and password is correct
     * @param user
     * @return 1: correct username and password, has not logged in yet
     * @return -1: user has logged in
     * @return -2: incorrect username/password or query fail
     */
    public int loginUser(ConcurrentHashMap<String, Object> user) {
    	String usr = (String) user.get("Username");
		String pwd = (String) user.get("Password");
		
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
				System.out.println("Correct username and password");
				if(TokenManager.getInstance().validateUser(usr)) {
		    		result = -1;
		    	}
				else{ result = 1; } 
			}
			// incorrect username and password
			else {
				System.out.println("Incorrect username and password");
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
    /**
     * This method checks if the book exists in the database using book id
     * @param int bookID
     * @return true: book exists
     * @return false: book does not exist or query fail
     */
    private long bookExist(long bookID) {
    	long exist = -1;
    	try {
    		Connection conn = connectionPool.getConnection();
    		String findBookQuery =
				"SELECT * FROM " + BOOKTABLE  + " WHERE "+  ID + "=?" ;
    		PreparedStatement findBookStmt = conn.prepareStatement(findBookQuery);
    		findBookStmt.setLong (1, bookID);
    		findBookStmt.execute();
    		ResultSet rs = findBookStmt.getResultSet();
    		if(rs.next()) {
    			exist = rs.getInt(ID);
    		}
    		
    		rs.close();
    		findBookStmt.close();
    		connectionPool.closeConnection(conn);
    		
    		return exist;
    		
    	} catch(SQLException e){
    		e.printStackTrace();
    	}
    	return exist;
	
    }
    /**
     * This method checks if the book exists in the database using full book data
     * @param ConcurrentHashMap<String, Object> book
     * @return true: book exists
     * @return false: book does not exist or query fail
     */
    private long bookExist(ConcurrentHashMap<String, Object> book) {
    	long exist = -1;
    	try {
			Connection conn = connectionPool.getConnection();
			
			String title = (String) book.get(TITLE);
			String author = (String) book.get(AUTHOR);
			String publisher = (String) book.get(PUBLISHER);
			int year = (Integer) book.get(YEAR);
			
			String query = "SELECT * FROM " + BOOKTABLE + " WHERE " 
					+ TITLE + "= ? AND " + AUTHOR + "= ? AND " + PUBLISHER + "= ? AND " + YEAR + "= ?; ";
			PreparedStatement searchStmt = conn.prepareStatement(query);
			searchStmt.setString(1, title);
			searchStmt.setString(2, author);
			searchStmt.setString(3, publisher);
			searchStmt.setInt(4, year);
			ResultSet searchRs = searchStmt.executeQuery();

			// already exist
			if(searchRs.next()) {
				exist = searchRs.getInt(ID);
			}
			searchStmt.close();
			searchRs.close();
			connectionPool.closeConnection(conn);
			
			return exist;
    	} catch (SQLException e) {
    		e.printStackTrace();
    	}
    	return exist;
    }
    
    
    
    /**
     * This function checks the availability of a book by its ID
     * @param bookID
     * @return 1 if book exist and available; 0 if book exist but unavailable; -1 if book doesn't exist; -2 if query fail
     */
    public int bookAvailability(long bookID) {
    	int status;
    	try {
    		Connection conn = connectionPool.getConnection();
    		String findBookQuery =
				"SELECT * FROM " + BOOKTABLE  + " WHERE "+  ID + "=?" ;
    		PreparedStatement findBookStmt = conn.prepareStatement(findBookQuery);
    		findBookStmt.setLong (1, bookID);
    		findBookStmt.execute();
    		ResultSet rs = findBookStmt.getResultSet();
    		// book exist
    		if(rs.next()) {
    			status = (rs.getBoolean(AVAILABLE)==true) ? 1 : 0; 
    		}
    		// book doesn't exist at all
    		else {
    			status = -1;
    		}
    		rs.close();
    		findBookStmt.close();
    		connectionPool.closeConnection(conn);
    		
    		return status;
    		
    	} catch(SQLException e){
    		e.printStackTrace();
    	}
    	// query failed
    	return -2;
    }
}
