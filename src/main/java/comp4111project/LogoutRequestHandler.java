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

public class LogoutRequestHandler implements HttpRequestHandler {
	
	
	@Override
	public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
		
		// parse the request url and check token validity
		
		// if the user has logged in (valid token)
		// response: 200 OK 
        response.setStatusCode(HttpStatus.SC_OK);
        
        // if the user is has not logged in (invalid token)
        // response: 400 Bad Request
        response.setEntity(
                new StringEntity("This is a logout request",
                        ContentType.TEXT_PLAIN));
	}
}