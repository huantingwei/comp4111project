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

public class DeleteBookRequestHandler implements HttpRequestHandler {
	
	
	@Override
	public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
		
        response.setStatusCode(HttpStatus.SC_OK);
        response.setEntity(
                new StringEntity("This is a delete book request",
                        ContentType.TEXT_PLAIN));
	}
}
