package comp4111project.Handlers;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.nio.protocol.BasicAsyncRequestConsumer;
import org.apache.http.nio.protocol.BasicAsyncResponseProducer;
import org.apache.http.nio.protocol.HttpAsyncExchange;
import org.apache.http.nio.protocol.HttpAsyncRequestConsumer;
import org.apache.http.nio.protocol.HttpAsyncRequestHandler;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import comp4111project.QueryManager;
import comp4111project.TokenManager;

public class NLoginRequestHandler implements HttpAsyncRequestHandler<HttpRequest> {

	public NLoginRequestHandler() {
		super();
	}

	@Override
	public HttpAsyncRequestConsumer<HttpRequest> processRequest(HttpRequest request, HttpContext context)
			throws HttpException, IOException {
		// Buffer request content in memory for simplicity
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
		
		if(!request.getRequestLine().getMethod().equals("POST")) {
			response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_BAD_REQUEST);
			return;
		}
		if(request instanceof HttpEntityEnclosingRequest) {
			HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
			String userContent = EntityUtils.toString(entity, Consts.UTF_8);
			ObjectMapper mapper = new ObjectMapper();
			ConcurrentHashMap<String, Object> user = mapper.readValue(userContent, ConcurrentHashMap.class);
			
			// check if username and password is correct and if user has already logged in
			Future<Integer> successLoginFuture = Executors.newSingleThreadExecutor().submit(() -> QueryManager.getInstance().loginUser(user));
			
			try {
				switch (successLoginFuture.get()) {
					// successful login
					case 1:
						response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_OK);
						String username = (String) user.get("Username");
						String newToken = TokenManager.getInstance().generateNewToken(username);

						Future<Boolean> futureAddToken = Executors.newSingleThreadExecutor().submit(() -> TokenManager.getInstance().addUserAndToken(username, newToken));
						if(futureAddToken.get()) {
							ObjectNode responseObject = new ObjectMapper().createObjectNode();
							responseObject.put("Token", newToken);
							response.setEntity(
									new NStringEntity(responseObject.toString(),
											ContentType.APPLICATION_JSON));
						}
						break;
					// this user has already logged in
					case -1:
						response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_CONFLICT);
						break;
					// incorrect username or password; or query fail
					case -2:
						response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_BAD_REQUEST);
						break;
					default:
						response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_BAD_REQUEST);
						break;
				}
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
				response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_BAD_REQUEST);
			}
		}
		else {
			response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_BAD_REQUEST);
		}
//		
//		response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_OK);
//        NStringEntity myEntity = new NStringEntity("<html><body>"
//        		+ "<h1>This is a Login request</h1>"
//        		+ "</body></html>",
//        		ContentType.create("text/html", "UTF-8"));
//        response.setEntity(myEntity);
	}
}
