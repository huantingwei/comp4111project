package comp4111project;

import comp4111project.Model.Book;

import javax.xml.transform.Result;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

public class QueryManager {
    private DBConnection connectionPool;
    private final static ConcurrentHashMap<String, String> USER_TOKEN = new ConcurrentHashMap<String, String>();
    private final static ConcurrentHashMap<String, String> TOKEN_USER = new ConcurrentHashMap<String, String>();

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

    public Boolean authorizeToken() {
        return true;
    }

    public Vector getBooks(ConcurrentHashMap<String, String> queryPairs) {
        queryPairs.remove("token");
        Vector<Book> books = new Vector<Book>();
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
                        searchQuery += " " + key + " LIKE" + " '%" + value + "%'" + " AND";
                    } else if(key.equals("id") || key.equals("title") || key.equals("publisher") || key.equals("year")) {
                        searchQuery += " " + key + " =" + " '" + value +"'" + " AND";
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

    // Returns an integer depending on the status: 0 - 200 OK 1 - No book record 2 - bad request
    public int returnAndLoanBook(String bookID, Boolean isReturningBook) {
        String updateQuery;
        try {
            Connection conn = connectionPool.getConnection();
            String searchQuery = "SELECT available from book WHERE id =" + " '" + bookID + "' ";
            PreparedStatement searchBookStmt = conn.prepareStatement(searchQuery);
            ResultSet rs = searchBookStmt.executeQuery();

            if (rs.next()) {
                if(rs.getBoolean("available") == (isReturningBook ? false : true)) {
                    System.out.println("ready to be returned/loaned");
                    try {
                        if(isReturningBook) {
                            updateQuery = "UPDATE book SET available = '1' WHERE id =" + " '" + bookID + "' ";
                        } else {
                            updateQuery = "UPDATE book SET available = '0' WHERE id =" + " '" + bookID + "' ";
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
}
