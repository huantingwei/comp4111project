package comp4111project;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Locale;
import java.util.Map;

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

public class LogoutRequestHandler implements HttpRequestHandler {
		
	@Override
	public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
		
		String method = request.getRequestLine().getMethod().toUpperCase(Locale.ROOT);
		if(!method.equals("GET")) throw new MethodNotSupportedException(method + "method not supported");
		String url = request.getRequestLine().getUri();
		
		String token = url.substring(url.indexOf("token=")+6); 
		
		/**
		 * if the user has logged in (valid token)
		 * response: 200 OK 
		 */
        if(BookManagementServer.tokenToUser.get(token)!=null) {
        	response.setStatusCode(HttpStatus.SC_OK);
        }
        /**
		 * if the user has NOT logged in (invalid token)
		 * response: 400 BAD REQUEST 
		 */
        else {
        	response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
        }
	}
}