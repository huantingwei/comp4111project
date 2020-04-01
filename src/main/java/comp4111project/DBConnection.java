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
        if(connections.size() == MAX_CONN) {
            conn.close();
        }
        else {
            connections.add(conn);
        }
    }
    
    /**
     * Configure the database initial settings
     * @param dbName
     * @param usrTbName
     * @param usrTbCol
     * @param bkTbName
     * @param initNumUser
     * @throws SQLException
     */
    private void configureDatabase(String dbName, String usrTbName, List<String> usrTbCol, String bkTbName, int initNumUser) throws SQLException {
    	createDatabase(dbName);
    	createUserTable(usrTbName, usrTbCol);
    	createBookTable(bkTbName);
    	initUser(usrTbName, usrTbCol, initNumUser);
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
    }
    
    /**
     * initialize users in the database
     * @param usrTbName
     * @param usrTbCol
     * @param numOfUser
     */
    public void initUser(String usrTbName, List<String> usrTbCol, int numOfUser) {
    	
    	System.out.println("Start initializing " + Integer.toString(numOfUser) + " users.");     

    	try {
    		Connection conn = getConnection();
    		String query = "INSERT INTO " + usrTbName + " ("
    				+ usrTbCol.get(0) +","
    				+ usrTbCol.get(1) +","
    				+ usrTbCol.get(2) + ")" 
    				+ " VALUES (?, ?, ?);" ;
    		PreparedStatement stmt = conn.prepareStatement(query);
    		// user00001 ~ user10000
    		int count = 0;
    		int prefixZero = (int) Math.log10(numOfUser);
    		
    		for(int i=1; i<=numOfUser; i++) {
    			// username/password = user/passwd + 00..0 + user number
    			String usr = "user";
    			String pwd = "pass";

    			int zeros = prefixZero - (int) Math.log10(i);
    			for(int z=0; z<zeros; z++) {
    				usr = usr.concat("0");
    				pwd = pwd.concat("0");
    			}
    			usr = usr.concat(Integer.toString(i));
    			pwd = pwd.concat(Integer.toString(i));
    			
    			stmt.setInt(1,  i);
    			stmt.setString(2, usr);
    			stmt.setString(3, pwd);
    			
    			stmt.addBatch();
    			count++;
    			if(count % 100 == 0 || count == numOfUser) {
    				stmt.executeBatch();
    			}
    		}
    		stmt.executeBatch();
    		closeConnection(conn);
    		System.out.println("Finished initializing " + Integer.toString(numOfUser) + "users.");
    	} catch (Exception e){
    	      System.err.println("Got an exception!");
    	      System.err.println(e.getMessage());
    	}
    	
    }
}
