package comp4111project.Handlers;

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

import comp4111project.BookManagementServer;

public class LogoutRequestHandler implements HttpRequestHandler {
		
	@Override
	public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
		
		String method = request.getRequestLine().getMethod().toUpperCase(Locale.ROOT);
		if(!method.equals("GET")) throw new MethodNotSupportedException(method + "method not supported");
		String url = request.getRequestLine().getUri();
		
		String token = url.substring(url.indexOf("token=")+6); 
		System.out.println("Token:"+token);
		
		/**
		 * if the user has logged in (valid token)
		 * response: 200 OK 
		 */
		
		String usr = BookManagementServer.TOKEN_USER.get(token);
        if(usr!=null) {
        	BookManagementServer.TOKEN_USER.remove(token);
        	BookManagementServer.USER_TOKEN.remove(usr);
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