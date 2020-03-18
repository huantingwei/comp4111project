//package comp4111project;
//
//import java.io.IOException;
//import java.io.InputStream;
//import java.net.URI;
//import java.net.URISyntaxException;
//import java.sql.Connection;
//import java.sql.PreparedStatement;
//import java.sql.ResultSet;
//import java.sql.SQLException;
//import java.sql.Statement;
//import java.util.Locale;
//import java.util.Map;
//
//import org.apache.http.HttpEntity;
//import org.apache.http.HttpEntityEnclosingRequest;
//import org.apache.http.HttpException;
//import org.apache.http.HttpRequest;
//import org.apache.http.HttpResponse;
//import org.apache.http.HttpStatus;
//import org.apache.http.HttpVersion;
//import org.apache.http.MethodNotSupportedException;
//import org.apache.http.entity.ContentType;
//import org.apache.http.entity.StringEntity;
//import org.apache.http.message.BasicHttpResponse;
//import org.apache.http.protocol.HttpContext;
//import org.apache.http.protocol.HttpRequestHandler;
//
//public class DeleteBookRequestHandler implements HttpRequestHandler {
//
//	private String BOOKTABLE = "book";
//	private String ID = "bookid";
//	private String TITLE = "Title";
//	private String AUTHOR = "Author";
//	private String PUBLISHER = "Publisher";
//	private String YEAR = "Year";
//	private String AVAILABLE = "Available";
//
//	@Override
//	public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
//		String method = request.getRequestLine().getMethod().toUpperCase(Locale.ROOT);
//		if(!method.equals("DELETE")) throw new MethodNotSupportedException(method + "method not supported");
//
//
//		try {
//			Connection conn = BookManagementServer.DB.getConnection();
//			Statement stmt = conn.createStatement();
//
//			URI uri = new URI(request.getRequestLine().getUri());
//			String path = uri.getPath();
//			String[] pairs = path.split("/");
//			Integer bookID = Integer.parseInt(pairs[-1]);
//
//			String findBookQuery =
//					"SELECT * FROM " + BOOKTABLE  + " WHERE "+  ID + "=?" ;
//			PreparedStatement findBookStmt = conn.prepareStatement(findBookQuery);
//			findBookStmt.setInt (1, bookID);
//			findBookStmt.execute();
//
//			ResultSet rs = findBookStmt.getResultSet();
//
//			/**
//			 * book exists, delete successfully
//			 */
//			if(rs.next()) {
//				System.out.println(ID+" exists, can be deleted");
//				String deleteBookQuery ="DELETE FROM " + BOOKTABLE + " WHERE " + ID + "= ?";
//
//				PreparedStatement deleteBookStmt = conn.prepareStatement(deleteBookQuery);
//				deleteBookStmt.setInt (1, bookID);
//
//				int affectedRows = deleteBookStmt.executeUpdate();
//				if (affectedRows == 0) {
//		            throw new SQLException("Failed to delete a book.");
//		        }
//				response.setStatusCode(HttpStatus.SC_OK);
//				deleteBookStmt.close();
//			}
//			/**
//			 * book doesn't exist, delete unsuccessfully
//			 */
//			else {
//				response = new BasicHttpResponse(HttpVersion.HTTP_1_1,
//					    HttpStatus.SC_BAD_REQUEST, "No book record");
//			}
//			rs.close();
//			BookManagementServer.DB.closeConnection(conn);
//
//		} catch (SQLException | URISyntaxException e) {
//			e.printStackTrace();
//		}
//
//	}
//}
