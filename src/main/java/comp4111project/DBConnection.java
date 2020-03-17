package comp4111project;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class DBConnection implements DBSource {
    private Properties props;
    private String URL;
    private String USER;
    private String PASSWD;
    private int max; // maximum number of connections in the connection pool
    private List<Connection> connections;

    public DBConnection() throws IOException, ClassNotFoundException {
        this("jdbc.properties");
    }
    
    
    public DBConnection(String configFile) throws IOException, ClassNotFoundException {
        props = new Properties();
        props.load(new FileInputStream(configFile));
		
		URL = props.getProperty("comp4111project.url");
		USER = props.getProperty("comp4111project.user");
		PASSWD = props.getProperty("comp4111project.password");

        max = Integer.parseInt(props.getProperty("comp4111project.poolmax"));
        Class.forName(props.getProperty("comp4111project.driver"));
        
        connections = new ArrayList<Connection>();
    }

    public void createDatabase(String dbName) throws SQLException {
    	Connection conn = getConnection();
    	
    	String query = "CREATE DATABASE ?";
    	PreparedStatement stmt = conn.prepareStatement(query);
    	stmt.setString(1, dbName);
    	stmt.executeUpdate();
    }
    /**
     * @return Connection, retrieved from the connection pool
     */
    public synchronized Connection getConnection() throws SQLException {
        if(connections.size() == 0) {
            return DriverManager.getConnection(URL, USER, PASSWD);
        }
        else {
            int lastIndex = connections.size() - 1;
            return connections.remove(lastIndex);
        }
    }
    /**
     * When closing the connection, add it to the connection pool for reuse
     */
    public synchronized void closeConnection(Connection conn) throws SQLException {
        if(connections.size() == max) {
            conn.close();
        }
        else {
            connections.add(conn);
        }
    }
    
    public void insertUser() {
    	try {
    		String query = "insert into user (iduser, username, password)" + "values (?, ?, ?)";
    		
    		for(int i=0; i<10; i++) {
    		      PreparedStatement preparedStmt = getConnection().prepareStatement(query);
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
