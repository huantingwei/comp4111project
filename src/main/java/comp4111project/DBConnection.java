package comp4111project;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class DBConnection implements DBSource {
    private Properties props;
    private String URL;
    private String USER;
    private String PASSWD;
    private int MAX_CONN; // maximum number of connections in the connection pool
    private List<Connection> connections;
    
    private String DB_NAME;
    private String USER_TB_NAME;
    private String BOOK_TB_NAME;
    private List<String> USER_TB_COL;
    private List<String> BOOK_TB_COL;
    private int INIT_NUM_USER;

    public DBConnection() throws IOException, ClassNotFoundException, SQLException {
        this("jdbc.properties");
    }
    
    /**
     * Create a database connection pool "connections" using ArrayList<Connection>
     * @param configFile
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws SQLException
     */
    public DBConnection(String configFile) throws IOException, ClassNotFoundException, SQLException {
        props = new Properties();
        props.load(new FileInputStream(configFile));
		
		URL = props.getProperty("comp4111project.url");
		USER = props.getProperty("comp4111project.user");
		PASSWD = props.getProperty("comp4111project.password");

        MAX_CONN = Integer.parseInt(props.getProperty("comp4111project.poolmax"));
        Class.forName(props.getProperty("comp4111project.driver"));
        
        connections = new ArrayList<Connection>();
        initConnection(MAX_CONN);
        
        DB_NAME = props.getProperty("comp4111.project.dbname");
        USER_TB_NAME = props.getProperty("comp4111project.usertbname");
        BOOK_TB_NAME = props.getProperty("comp4111project.booktbname");
        USER_TB_COL = Arrays.asList(props.getProperty("comp4111project.usertbcol").split("#"));
        BOOK_TB_COL = Arrays.asList(props.getProperty("comp4111project.booktbcol").split("#"));
        INIT_NUM_USER = Integer.parseInt(props.getProperty("comp4111project.initnumuser"));
        
    }
   
    
    /**
     * @return Connection, retrieved from the connection pool
     */
    public Connection getConnection() throws SQLException {
    	synchronized(connections) {
    		while(connections.isEmpty()) {
	    		try {
					this.connections.wait();
				} catch (InterruptedException e) {
					System.out.println("exception in connection.wait()");
					//e.printStackTrace();
				}
	    	}
	    	return connections.remove(0);
    	}
    }
    /**
     * When closing the connection, add it to the connection pool for reuse
     */
    public void closeConnection(Connection conn) throws SQLException {
    	synchronized(connections) {
    		connections.add(conn);
    		connections.notify();
    	}
    }
    
    private void initConnection(int numConn) throws SQLException {
        for(int i=0; i<numConn; i++) {
            connections.add(DriverManager.getConnection(URL, USER, PASSWD));	
        }
    }
    
    /**
     * Configure the database initial settings
     * @param dbName
     * @param usrTbName
     * @param usrTbCol
     * @param bkTbName
     * @throws SQLException
     */
    private void configureDatabase(String dbName, String usrTbName, List<String> usrTbCol, String bkTbName) throws SQLException {
    	createDatabase(dbName);
    	createUserTable(usrTbName, usrTbCol);
    	createBookTable(bkTbName);
    }
    
    /**
     * Create database
     * @param dbName
     * @throws SQLException
     */
    private void createDatabase(String dbName) throws SQLException {
	    Connection conn = getConnection();
    	String query = "CREATE DATABASE " + dbName + ";";
    	PreparedStatement stmt = conn.prepareStatement(query);
    	stmt.executeUpdate();
    	stmt.close();
    	closeConnection(conn);
    }
    
    /**
     * Create a table of username and password in the database
     * Columns: userid, username, password
     * Column types: int(30), varchar(100), varchar(100)
     * primary key: userid
     * @param tbName
     * @param colName
     * @throws SQLException
     */
    private void createUserTable(String tbName, List<String> colName) throws SQLException {
    	Connection conn = getConnection();
    	
    	// TODO: user string concantenation because PreparedStatement will add single quotes to names
    	// which is invalid syntax for mysql 5.7 (?)
    	// e.g. `username` , 
    	String query = "CREATE TABLE " + tbName
    			+ " ( " + colName.get(0) + " INT(30) NOT NULL, "
    			+ colName.get(1) + " VARCHAR(100) NOT NULL, "
    			+ colName.get(2) + " VARCHAR(100) NOT NULL, "
    			+ "PRIMARY KEY (" + colName.get(0) + ")"
    			+ ");" ;
    	PreparedStatement stmt = conn.prepareStatement(query);
       	stmt.executeUpdate();
       	stmt.close();
    	closeConnection(conn);
    }
    
    /**
     * Create a table of books in the database
     * colName should follow: bookid, Title, 
     * @param tbName
     * @throws SQLException
     */
    private void createBookTable(String tbName) throws SQLException {
    	Connection conn = getConnection();
    	String query = "CREATE TABLE " + tbName
    			+ " ( bookid INT(50) NOT NULL AUTO_INCREMENT, "
    			+ "Title VARCHAR(100) NOT NULL,"
    			+ "Author VARCHAR(100) NOT NULL,"
    			+ "Publisher VARCHAR(100) NOT NULL,"
    			+ "Year INT(30) NOT NULL,"
    			+ "Available TINYINT(4) NOT NULL,"
    			+ "PRIMARY KEY (bookid) );";
    	PreparedStatement stmt = conn.prepareStatement(query);
    	stmt.executeUpdate();
    	stmt.close();
    	closeConnection(conn);
    }
    
}
