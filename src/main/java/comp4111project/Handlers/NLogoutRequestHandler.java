package comp4111project.Handlers;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.io.UnsupportedEncodingException;

import java.util.concurrent.ConcurrentHashMap;
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
		new Thread() {
			@Override
			public void run() {
					final HttpResponse response = httpExchange.getResponse();
					try {
						handleInternal(request, response, context);
					} catch (HttpException | IOException e) {
						//System.out.println("exception in logout handle()");
						//e.printStackTrace();
					}
					httpExchange.submitResponse(new BasicAsyncResponseProducer(response));
			}
		}.start();

	}
	
	private void handleInternal(final HttpRequest request,
            final HttpResponse response,
            final HttpContext context) throws HttpException, IOException {

		if(!request.getRequestLine().getMethod().equals("GET")) {
			response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_BAD_REQUEST);
			return;
		}
        
		String token = getTokenFromURI(request.getRequestLine().getUri());
		// Future<String> tokenFuture = Executors.newSingleThreadExecutor().submit(() -> TokenManager.getInstance().getTokenFromURI(request.getRequestLine().getUri()));
		Future<Boolean> loggedin = Executors.newSingleThreadExecutor().submit(() -> TokenManager.getInstance().validateToken(token));
		
		try {
			if(loggedin.get()) {
				Future<Boolean> futureRemoveToken = Executors.newSingleThreadExecutor().submit(() -> TokenManager.getInstance().removeUserAndToken(token));
				if(futureRemoveToken.get()) {
					response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_OK);
				}
				else {
					//System.out.println("not removed");
					response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_BAD_REQUEST);
				}
			}
			else{
				//System.out.println("not logged in yet");
				response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_BAD_REQUEST);
			}
		} catch (InterruptedException | ExecutionException e) {
			//e.printStackTrace();
			//System.out.println("exception in future loggedin");
			response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_BAD_REQUEST);
		}
	}
	
	private String getTokenFromURI(String url) {
		String token = null;
    	try {
    		URI uri = new URI(url);
			ConcurrentHashMap<String, String> query_pairs = new ConcurrentHashMap<String, String>();
	        String query = uri.getQuery();
	        String[] pairs = query.split("&");
	        for (String pair : pairs) {
	            int idx = pair.indexOf("=");
	            query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
	        }
	        token = query_pairs.get("token");
    	} catch (URISyntaxException | UnsupportedEncodingException e) {
			e.printStackTrace();
		}
        return token;
	}
}