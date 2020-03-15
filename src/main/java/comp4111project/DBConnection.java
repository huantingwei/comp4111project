package comp4111project;

import comp4111project.Model.Book;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;


public class DBConnection {
	private Connection con;
    private Statement st;
    private ResultSet rs;
    final private int port = 3306;
    final private String instance = "comp4111project";
    final private String username = "root";
    final private String password = "root";
     
    public DBConnection() {
        try {
        	try {
        	    Class.forName("com.mysql.jdbc.Driver");
        	}
        	catch(ClassNotFoundException e) {
        	    System.out.println(e);
        	}
        	String url = "jdbc:mysql://localhost:" + port + "/" + instance;
            con = DriverManager.getConnection(url, username, password);
            st = con.createStatement();
             
        }catch(SQLException ex){
            System.out.println("Error: " + ex);
        }
    }

    /**
     * 3 columns: iduser(primary key), username, password
     */
    public void getAllUser() {
        try {
            String query = "select * from user";
            rs = st.executeQuery(query);
            System.out.println("Records for Database");
            while(rs.next()) {
                int id = rs.getInt("iduser");
                String username = rs.getString("username");
                String password = rs.getString("password");
                System.out.println("id="+id+" name="+username+" password="+password);
            }
        }catch(Exception ex) {
            System.out.println(ex);
        }
    }
    
    public void insertUser() {
    	try {
    		String query = "insert into user (iduser, username, password)" + "values (?, ?, ?)";
    		
    		for(int i=0; i<10; i++) {
    		      PreparedStatement preparedStmt = con.prepareStatement(query);
    		      preparedStmt.setInt (1, i);
    		      preparedStmt.setString (2, "user"+i);
    		      preparedStmt.setString (3, "passwd"+i);
    		      // execute the preparedstatement
    		      preparedStmt.execute();
    		}	
    	} catch (Exception e){
    	      System.err.println("Got an exception!");
    	      System.err.println(e.getMessage());
    	}
    }

    public Vector getBooks(ConcurrentHashMap<String, String> queryPairs) {
        Vector<Book> books = new Vector<Book>();
        try {
            String query = "SELECT * FROM book WHERE";
            for (Map.Entry<String, String> entry : queryPairs.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                if(key.equals("author")) {
                    query += " " + key + " LIKE" + " '%" + value + "%'" + " AND";
                } else if(key.equals("id") || key.equals("title") || key.equals("publisher") || key.equals("year")) {
                    query += " " + key + " =" + " '" + value +"'" + " AND";
                }
            }
            query = query.substring(0, query.length() - 3);
            if(queryPairs.containsKey("sortby")) {
                query += " " + "ORDER BY " + queryPairs.get("sortby");
            }
            if(queryPairs.containsKey("order")) {
                query += " " + queryPairs.get("order");
            }
            if(queryPairs.containsKey("limit")) {
                query += " LIMIT " + queryPairs.get("limit");
            }

            System.out.println(query);
            rs = st.executeQuery(query);

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
        } catch (Exception e) {
            System.err.println("Got an exception!");
            System.err.println(e.getMessage());
        }
        return books;
    }
}
