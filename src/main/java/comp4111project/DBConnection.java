package comp4111project;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;


public class DBConnection {
	private Connection con;
    private Statement st;
    private ResultSet rs;
    final private int port = 3306;
    final private String instance = "comp4111project";
    final private String username = "root";
    final private String password = "toor";
     
    public DBConnection() {
        try {
 
            Class.forName("com.mysql.jdbc.Driver");
            con= DriverManager.getConnection("jdbc:mysql://localhost:" + port + "/"+ instance, username, password);
            st= con.createStatement();
             
        }catch(Exception ex){
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
}
