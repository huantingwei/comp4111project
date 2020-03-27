package comp4111project.Handlers;

import java.io.IOException;
import java.net.URLDecoder;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import comp4111project.TokenManager;

public class LogoutRequestHandler implements HttpRequestHandler {

	@Override
	public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {

		System.out.println("Logout request");
		
		String token = TokenManager.getInstance().getTokenFromURI(request.getRequestLine().getUri());
		Boolean loggedin = TokenManager.getInstance().validateToken(token);
		
		// user has logged in (valid token)
        if(loggedin) {
        	TokenManager.getInstance().removeUserAndToken(token);
        	response.setStatusCode(HttpStatus.SC_OK);
        }
        // user has not logged in (invalid token)
        else {
        	response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
        }
	}
}