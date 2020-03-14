package comp4111project.Handlers;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import comp4111project.BookManagementServer;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.RequestLine;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class LoginRequestHandler implements HttpRequestHandler {
	
	private int BUFFER_SIZE = 16384;
	
	private String userTable = "user";
	private String unameCol = "username";
	private String pwdCol = "password";
	
	private Map<String,String> reqData;
    private Connection conn;
	private Statement stmt;
	private ResultSet rs;

	
	public LoginRequestHandler() {
		super();
	};
	
	@Override
	public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
		
		String method = request.getRequestLine().getMethod().toUpperCase(Locale.ROOT);
		if(!method.equals("POST")) throw new MethodNotSupportedException(method + "method not supported");
		
		// parse the request body to Map<String, String> inputData
		if(request instanceof HttpEntityEnclosingRequest) {
			HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
			if (entity != null) {
				InputStream instream = entity.getContent();
			    try {
			    	reqData = readInputToMap(instream);
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
			conn = BookManagementServer.db.getConnection();
			stmt = conn.createStatement();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		String usr = "'" + reqData.get("Username") + "'";
		String pwd = "'" + reqData.get("Password") + "'";
		
		String query = 
			//"IF EXISTS ( "
				"SELECT 1 FROM " + userTable  + " WHERE "+  unameCol + "=" + usr + " AND " + pwdCol + "=" + pwd + ";";
		try {
			rs = stmt.executeQuery(query);
			while(rs.next()) {
				// if not logged in yet

				if(BookManagementServer.userToToken.get(usr)==null) {
                	
                	response.setStatusCode(HttpStatus.SC_OK);
                	
                	// generate token
                	// TODO: generate httpsession token
                	// generating a unique
                	String newToken = generateNewToken(usr);
                	String rsp = "{ \"Token\" : \"" + newToken + "\" }";
                	
                	response.setEntity(
                            new StringEntity(rsp,
                                    ContentType.APPLICATION_JSON));
                	return;
                }
                
                // if already logged in
                // response: 409 Conflict
                // TODO: how to check logged in?
                else {
                	response.setStatusCode(HttpStatus.SC_CONFLICT);
                	return;
                }
                
			}
			
			// if either the user name or password is incorrect
			response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
			return;
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	private String readInputToString(InputStream instream) throws IOException {
    	InputStreamReader insReader = new InputStreamReader(instream);
    	BufferedReader reader = new BufferedReader(insReader);
        StringBuffer sb = new StringBuffer();
        String str;
        while((str = reader.readLine())!= null){
           sb.append(str);
        }
        // System.out.println(sb.toString());
        return sb.toString();
	}
	
	private Map<String, String> readInputToMap(InputStream instream) throws JsonParseException, JsonMappingException, IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    	int nRead;
    	byte[] data = new byte[BUFFER_SIZE];
		while ((nRead = instream.read(data, 0, data.length)) != -1) {
			buffer.write(data, 0, nRead);
		}
    	ObjectMapper objectMapper = new ObjectMapper();
    	// System.out.println("Map is: " + inputData);
    	return objectMapper.readValue(data, HashMap.class);
    	
	}
	
	private String generateNewToken(String usr) {
		// this_assumes_no_duplicate_user
		String newToken = usr + "hi";
    	BookManagementServer.userToToken.put(usr, newToken);
    	BookManagementServer.tokenToUser.put(newToken, usr);
    	return newToken;
	}
}
