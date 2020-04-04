package comp4111project.Handlers;

import java.io.IOException;
import java.net.URLDecoder;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.http.*;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import comp4111project.TokenManager;

public class LogoutRequestHandler implements HttpRequestHandler {

	@Override
	public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {

		if(!request.getRequestLine().getMethod().equals("GET")) {
			System.out.println("incorrect method");
			response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_BAD_REQUEST);
			return;
		}
		// concurrency issue?
		// String token = TokenManager.getInstance().getTokenFromURI(request.getRequestLine().getUri()); 
		
		Future<String> tokenFuture = Executors.newSingleThreadExecutor().submit(() -> TokenManager.getInstance().getTokenFromURI(request.getRequestLine().getUri()));
		try {
			String token = tokenFuture.get();
			Future<Boolean> loggedin = Executors.newSingleThreadExecutor().submit(() -> TokenManager.getInstance().validateToken(token));

			if(loggedin.get()) {
				Future<Boolean> futureRemoveToken = Executors.newSingleThreadExecutor().submit(() -> TokenManager.getInstance().removeUserAndToken(token));
				if(futureRemoveToken.get()) {
					response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_OK);
				}
				else {
					response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_BAD_REQUEST);
				}
			}
			else{
				response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_BAD_REQUEST);
			}
			
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
			response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_BAD_REQUEST);
		}

	}
}