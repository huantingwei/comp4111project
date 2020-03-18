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
import comp4111project.QueryManager;

public class LogoutRequestHandler implements HttpRequestHandler {

	@Override
	public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {

		// TODO: better way to get token?
		String url = request.getRequestLine().getUri();
		String token = url.substring(url.indexOf("token=")+6);
		
		// user has logged in (valid token)
		String usr = QueryManager.getInstance().getUserFromToken(token);
        if(usr!=null) {
        	QueryManager.getInstance().removeUserAndToken(token);
        	response.setStatusCode(HttpStatus.SC_OK);
        }
        // user has not logged in (invalid token)
        else {
        	response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
        }
	}
}