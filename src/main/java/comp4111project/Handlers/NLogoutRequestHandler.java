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
import org.apache.http.nio.protocol.BasicAsyncRequestConsumer;
import org.apache.http.nio.protocol.BasicAsyncResponseProducer;
import org.apache.http.nio.protocol.HttpAsyncExchange;
import org.apache.http.nio.protocol.HttpAsyncRequestConsumer;
import org.apache.http.nio.protocol.HttpAsyncRequestHandler;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import comp4111project.TokenManager;

public class NLogoutRequestHandler implements HttpAsyncRequestHandler<HttpRequest> {

	@Override
	public HttpAsyncRequestConsumer<HttpRequest> processRequest(HttpRequest request, HttpContext context)
			throws HttpException, IOException {
		return new BasicAsyncRequestConsumer();
	}

	@Override
	public void handle(HttpRequest request, HttpAsyncExchange httpExchange, HttpContext context)
			throws HttpException, IOException {
		final HttpResponse response = httpExchange.getResponse();
        handleInternal(request, response, context);
        httpExchange.submitResponse(new BasicAsyncResponseProducer(response));
	}
	
	private void handleInternal(final HttpRequest request,
            final HttpResponse response,
            final HttpContext context) throws HttpException, IOException {

		if(!request.getRequestLine().getMethod().equals("GET")) {
			response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_BAD_REQUEST);
			return;
		}
		
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