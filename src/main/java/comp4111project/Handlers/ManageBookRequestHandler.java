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

import comp4111project.QueryManager;
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

					loanOrReturnBook(response, QueryManager.getInstance().returnAndLoanBook(bookID, isReturningBook.get("Available")));
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

	public void loanOrReturnBook(HttpResponse response, int result) {
		switch(result) {
			case(0):
				response.setStatusCode(HttpStatus.SC_OK);
				response.setEntity(
						new StringEntity("Book Returned/Loaned Successfully",
								ContentType.TEXT_PLAIN)
				);
				break;
			case(1):
				response.setStatusCode(HttpStatus.SC_NOT_FOUND);
				response.setEntity(
						new StringEntity("No Book Record Found",
								ContentType.TEXT_PLAIN)
				);
				break;
			case(2):
				response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
				response.setEntity(
						new StringEntity("This book has already been returned/loaned",
								ContentType.TEXT_PLAIN)
				);
				break;
			default:
				break;
		}
	}
}