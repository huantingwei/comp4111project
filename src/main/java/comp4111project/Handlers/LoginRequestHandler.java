//package comp4111project.Handlers;
//
//import java.io.BufferedReader;
//import java.io.ByteArrayOutputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.security.SecureRandom;
//import java.sql.Connection;
//import java.sql.PreparedStatement;
//import java.sql.ResultSet;
//import java.sql.SQLException;
//import java.sql.Statement;
//import java.util.Base64;
//import java.util.HashMap;
//import java.util.Locale;
//import java.util.Map;
//
//import org.apache.http.Header;
//import org.apache.http.HttpEntity;
//import org.apache.http.HttpEntityEnclosingRequest;
//import org.apache.http.HttpException;
//import org.apache.http.HttpRequest;
//import org.apache.http.HttpResponse;
//import org.apache.http.HttpStatus;
//import org.apache.http.MethodNotSupportedException;
//import org.apache.http.RequestLine;
//import org.apache.http.entity.ContentType;
//import org.apache.http.entity.StringEntity;
//import org.apache.http.protocol.HttpContext;
//import org.apache.http.protocol.HttpRequestHandler;
//
//import com.fasterxml.jackson.core.JsonParseException;
//import com.fasterxml.jackson.databind.JsonMappingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//
//import comp4111project.BookManagementServer;
//import comp4111project.JsonParser;
//
//public class LoginRequestHandler implements HttpRequestHandler {
//
//	private String USERTABLE = "user";
//	private String USERNAME = "username";
//	private String PASSWORD = "password";
//
//	private Map<String,Object> reqData;
//    private Connection conn;
//
//	public LoginRequestHandler() {
//		super();
//	};
//
//	@Override
//	public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
//
//		String method = request.getRequestLine().getMethod().toUpperCase(Locale.ROOT);
//		if(!method.equals("POST")) throw new MethodNotSupportedException(method + "method not supported");
//
//		// parse the request body to Map<String, String> inputData
//		if(request instanceof HttpEntityEnclosingRequest) {
//			HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
//			if (entity != null) {
//				InputStream instream = entity.getContent();
//			    try {
//			    	reqData = new JsonParser().readToMap(instream);
//			    } catch (Exception e){
//			    	e.printStackTrace();
//			    } finally {
//			        instream.close();
//			    }
//			}
//			else {
//				System.out.println("This request does not have any body data.");
//			}
//		}
//
//		// establishing db connection
//		try {
//			conn = BookManagementServer.DB.getConnection();
//
//			String usr = (String) reqData.get("Username");
//			String pwd = (String) reqData.get("Password");
//
//			String checkUserQuery =
//					"SELECT 1 FROM " + USERTABLE  + " WHERE "+  USERNAME + "=" + " ? " + "AND " + PASSWORD + "=" + " ? ";
//			PreparedStatement stmt = conn.prepareStatement(checkUserQuery);
//			stmt.setString(1,  usr);
//			stmt.setString(2,  pwd);
//			ResultSet rs = stmt.executeQuery();
//
//			/**
//			 * if username and password is correct
//			 */
//			if(rs.next()) {
//				/**
//				 * if this user is not logging in right now
//				 */
//				if(BookManagementServer.USER_TOKEN.get(usr)==null) {
//                	response.setStatusCode(HttpStatus.SC_OK);
//                	String newToken = generateNewToken(usr);
//                	String rsp = "{ \"Token\" : \"" + newToken + "\" }";
//
//                	response.setEntity(
//                            new StringEntity(rsp,
//                                    ContentType.APPLICATION_JSON));
//                }
//                else {
//                	response.setStatusCode(HttpStatus.SC_CONFLICT);
//                }
//			}
//
//			/**
//			 *  if either the user name or password is incorrect
//			 */
//			else{
//				response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
//			}
//			rs.close();
//			BookManagementServer.DB.closeConnection(conn);
//
//		} catch (SQLException e) {
//			e.printStackTrace();
//		}
//
//
//	}
//
//	private String generateNewToken(String usr) {
//		/**
//		 * assume no duplicate username
//		 * generate token by base64 encode the username
//		 */
//		//SecureRandom secureRandom = new SecureRandom();
//		Base64.Encoder base64Encoder = Base64.getUrlEncoder();
//		//byte[] randomBytes = new byte[24]
//		//secureRandom.nextBytes(randomBytes);
//		String newToken = base64Encoder.encodeToString(usr.getBytes());
//
//    	BookManagementServer.USER_TOKEN.put(usr, newToken);
//    	BookManagementServer.TOKEN_USER.put(newToken, usr);
//    	return newToken;
//	}
//}
