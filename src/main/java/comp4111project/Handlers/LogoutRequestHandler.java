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
		Future<Boolean> loggedin = Executors.newSingleThreadExecutor().submit(() -> TokenManager.getInstance().validateToken(token));
    try {
			if(loggedin.get()) {
        Executors.newSingleThreadExecutor().execute(() -> TokenManager.getInstance().removeUserAndToken(token));
        response.setStatusCode(HttpStatus.SC_OK);
			}
      else{
        response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
      }
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}

	}
}