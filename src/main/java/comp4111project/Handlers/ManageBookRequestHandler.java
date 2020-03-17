package comp4111project.Handlers;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.JSONPObject;

import comp4111project.BookManagementServer;

import org.apache.http.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.util.EntityUtils;

public class ManageBookRequestHandler implements HttpRequestHandler {
	
	private String BOOKTABLE = "book";
	private String ID = "bookid";
	private String TITLE = "Title";
	private String AUTHOR = "Author";
	private String PUBLISHER = "Publisher";
	private String YEAR = "Year";
	private String AVAILABLE = "Available";
	
	@Override
	public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
		System.out.println("Managing Book");
		System.out.println(request.getRequestLine().getMethod());

		switch (request.getRequestLine().getMethod()) {
			case("PUT"):
				if (request instanceof HttpEntityEnclosingRequest) {
					HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
					String testContent = EntityUtils.toString(entity, Consts.UTF_8);
					ObjectMapper mapper = new ObjectMapper();
					ConcurrentHashMap<String, Boolean> isReturningBook = mapper.readValue(testContent, ConcurrentHashMap.class);

					String fullPath = request.getRequestLine().getUri();
					String[] paths = fullPath.split("/");
					String bookID = paths[paths.length - 1];

					if(isReturningBook.get("Available")) {
						System.out.println("this is returning request");
					} else {
						LoanBook(response, bookID);
					}
				}
				break;
			/**
			 * Delete a book
			 */
			case("DELETE"):
				
				try {
					Connection conn = BookManagementServer.DB.getConnection();

					URI uri = new URI(request.getRequestLine().getUri());
					String path = uri.getPath();
					String[] pairs = path.split("/");
					Integer bookID = Integer.parseInt(pairs[pairs.length-1]);
					
					String findBookQuery = 
							"SELECT * FROM " + BOOKTABLE  + " WHERE "+  ID + "=?" ;
					PreparedStatement findBookStmt = conn.prepareStatement(findBookQuery);
					findBookStmt.setInt (1, bookID);
					findBookStmt.execute();
					
					ResultSet rs = findBookStmt.getResultSet();
					
					/** 
					 * book exists, delete successfully
					 */
					if(rs.next()) {
						System.out.println(ID+" exists, can be deleted");
						String deleteBookQuery ="DELETE FROM " + BOOKTABLE + " WHERE " + ID + "= ?";

						PreparedStatement deleteBookStmt = conn.prepareStatement(deleteBookQuery);
						deleteBookStmt.setInt (1, bookID);
						
						int affectedRows = deleteBookStmt.executeUpdate();
						if (affectedRows == 0) {
							System.out.println("Failed to delete a book.");
				            throw new SQLException("Failed to delete a book.");
				        }
						response.setStatusCode(HttpStatus.SC_OK);
						deleteBookStmt.close();
					}
					/** 
					 * book doesn't exist, delete unsuccessfully
					 */
					else {
						response = new BasicHttpResponse(HttpVersion.HTTP_1_1,
							    HttpStatus.SC_BAD_REQUEST, "No book record");
					}
					rs.close();
					BookManagementServer.DB.closeConnection(conn);
						
				} catch (SQLException | URISyntaxException e) {
					e.printStackTrace();
				} 
				break;
				
			default:
				response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
				response.setEntity(
						new StringEntity("Bad Request",
								ContentType.TEXT_PLAIN)
				);
				break;
		}
	}

	public void LoanBook(HttpResponse response, String bookID) {
		response.setEntity(
				new StringEntity("Loaning a book",
						ContentType.TEXT_PLAIN)
		);
	}
}