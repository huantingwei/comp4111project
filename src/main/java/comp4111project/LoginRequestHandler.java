package comp4111project;

import java.io.IOException;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

public class LoginRequestHandler implements HttpRequestHandler {
	
	
	@Override
	public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
		
		// parse the request url
		
		// if the user name and password is correct
		// response: 200 OK + generate token
        response.setStatusCode(HttpStatus.SC_OK);
        
        // if either the user name or password is incorrect
        // response: 400 Bad Request
        
        // if the user has already logged in
        // response: 409 Conflict

        response.setEntity(
                new StringEntity("This is a login request",
                        ContentType.TEXT_PLAIN));
	}
}
