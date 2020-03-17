package comp4111project.Handlers;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import comp4111project.BookManagementServer;
import comp4111project.JsonParser;

public class AddBookRequestHandler implements HttpRequestHandler {
	
	private String BOOKTABLE = "book";
	private String ID = "bookid";
	private String TITLE = "Title";
	private String AUTHOR = "Author";
	private String PUBLISHER = "Publisher";
	private String YEAR = "Year";
	private String AVAILABLE = "Available";
	
	private Map<String, Object> reqData;
	
	public AddBookRequestHandler() {
		super();
	};
	
	@Override
	public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
		
		String method = request.getRequestLine().getMethod().toUpperCase(Locale.ROOT);
		if(!method.equals("POST")) throw new MethodNotSupportedException(method + "method not supported");
		String url = request.getRequestLine().getUri();
		String token = url.substring(url.indexOf("token=")+6); 
		
		// parse request data
		// TODO: check if missing column?
		if(request instanceof HttpEntityEnclosingRequest) {
			HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
			if (entity != null) {
				InputStream instream = entity.getContent();
			    try {
			    	reqData = new JsonParser().readToMap(instream);
			    } catch (Exception e){
			    	e.printStackTrace();
			    } finally {
			        instream.close();
			    }
			}
			else {
				System.out.println("This request does not have any body data.");
			}
		}
		
		// establishing db connection			
		try {
			Connection conn = BookManagementServer.DB.getConnection();
			String title = (String) reqData.get(TITLE);
			String query = 
					"SELECT " + ID + " FROM " + BOOKTABLE  + " WHERE "+  TITLE + "= ? ;";
			PreparedStatement stmt = conn.prepareStatement(query);
			ResultSet rs = stmt.executeQuery();
			
			// already exist
			if(rs.next()) {
				response.setStatusCode(HttpStatus.SC_CONFLICT);
				response.addHeader("Duplicate record",  "/books/" + rs.getInt(ID));
			}
			
			// does not exist
			// add book
			else {
				String addBookQuery ="INSERT INTO " + BOOKTABLE + "(" + TITLE + "," + AUTHOR + "," + PUBLISHER + "," + YEAR + "," + AVAILABLE + ")"
				        + " VALUES (?, ?, ?, ?, ?)";

				PreparedStatement preparedStmt = conn.prepareStatement(addBookQuery, Statement.RETURN_GENERATED_KEYS);
				preparedStmt.setString (1, (String) reqData.get(TITLE));
				preparedStmt.setString (2, (String) reqData.get(AUTHOR));
				preparedStmt.setString (3, (String) reqData.get(PUBLISHER));
				preparedStmt.setInt (4, (Integer) reqData.get(YEAR));
				preparedStmt.setBoolean (5, true);

				int affectedRows = preparedStmt.executeUpdate();
				if (affectedRows == 0) {
		            throw new SQLException("Failed to add a book.");
		        }

				try (ResultSet generatedKeys = preparedStmt.getGeneratedKeys()) {
		            if (generatedKeys.next()) {
		                response.setStatusCode(HttpStatus.SC_CREATED);
		                int newID = generatedKeys.getInt(1);
		                response.addHeader("Location", "/books/" + newID);
		                response.setEntity(
		                        new StringEntity("http://" + BookManagementServer.HOST + ":" + BookManagementServer.PORT 
		                        		+ BookManagementServer.ROOT_DIRECTORY + "/books/" + newID + "?token=" + token,
		                                ContentType.TEXT_PLAIN));
		            }
		            else {
		                throw new SQLException("No book ID returned");
		            }
		            generatedKeys.close();
		        }
			}
			rs.close();
			BookManagementServer.DB.closeConnection(conn);
			
		} catch (SQLException e) {
			e.printStackTrace();
		}	
	}
		
	
}
