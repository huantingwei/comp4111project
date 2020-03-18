package comp4111project;

import comp4111project.Model.Book;

import javax.xml.transform.Result;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

public class QueryManager {
    private QueryManager() {

    }

    private static class BillPushSingleton {
        private static final QueryManager INSTANCE = new QueryManager();
    }

    public static QueryManager getInstance() {
        return BillPushSingleton.INSTANCE;
    }

    public Vector getBooks(ConcurrentHashMap<String, String> queryPairs) {
        Vector<Book> books = new Vector<Book>();
        try {
            Connection conn = BookManagementServer.DB.getConnection();

            String searchQuery = "SELECT * FROM book WHERE";
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

            System.out.println(searchQuery);
            PreparedStatement searchStmt = conn.prepareStatement(searchQuery);
            ResultSet rs = searchStmt.executeQuery();

            System.out.println("Book LookUp");
            while(rs.next()) {
                String bookID = rs.getString("id");
                String title = rs.getString("title");
                String bookAuthor = rs.getString("author");
                String publisher = rs.getString("publisher");
                int year = rs.getInt("year");
                Book foundBook = new Book(Integer.parseInt(bookID), title, bookAuthor, publisher, year);
                books.add(foundBook);
            }
            BookManagementServer.DB.closeConnection(conn);
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
            Connection conn = BookManagementServer.DB.getConnection();
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

                        if(result == 1) {
                            BookManagementServer.DB.closeConnection(conn);
                            return 0; // OK
                        } else {
                            BookManagementServer.DB.closeConnection(conn);
                            return 2; // Bad Request
                        }

                    } catch(Exception ex) {
                        return 2; // Bad Request
                    } finally {
                        System.out.println("Closing connection now...");
                        BookManagementServer.DB.closeConnection(conn);
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
