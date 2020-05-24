package comp4111project.Handlers;

import java.io.IOException;

import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.ParseException;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.nio.protocol.BasicAsyncRequestConsumer;
import org.apache.http.nio.protocol.BasicAsyncResponseProducer;
import org.apache.http.nio.protocol.HttpAsyncExchange;
import org.apache.http.nio.protocol.HttpAsyncRequestConsumer;
import org.apache.http.nio.protocol.HttpAsyncRequestHandler;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
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
		new Thread() {
			@Override
			public void run() {
						final HttpResponse response = httpExchange.getResponse();
						try {
							handleInternal(request, response, context);
						} catch (HttpException | IOException e) {
							//System.out.println("exception in login handle()");
						}
						httpExchange.submitResponse(new BasicAsyncResponseProducer(response));
			}
		}.start();

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
				String userContent = null;
				try {
					userContent = EntityUtils.toString(entity, Consts.UTF_8);
				} catch (ParseException | IOException e1) {
					e1.printStackTrace();
					response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_BAD_REQUEST);
					return;
				}
				ObjectMapper mapper = new ObjectMapper();
				ConcurrentHashMap<String, Object> user = null;
				try {
					user = mapper.readValue(userContent, ConcurrentHashMap.class);
				} catch (JsonProcessingException e1) {
					e1.printStackTrace();
					response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_BAD_REQUEST);
					return;
				}
				
				int successLogin = QueryManager.getInstance().loginUser(user);
				// final ConcurrentHashMap<String, Object> finalUser = user; 
				
				// check if username and password is correct and if user has already logged in
				
				switch (successLogin) {
					// successful login
					case 1:
						String username = (String) user.get("Username");
						String newToken = generateToken(username);

						// Future<Boolean> futureAddToken = Executors.newSingleThreadExecutor().submit(() -> TokenManager.getInstance().addUserAndToken(username, newToken));
						boolean addToken = TokenManager.getInstance().addUserAndToken(username, newToken);
						if(addToken) {
							ObjectNode responseObject = new ObjectMapper().createObjectNode();
							responseObject.put("Token", newToken);
							response.setEntity(
									new NStringEntity(responseObject.toString(),
											ContentType.APPLICATION_JSON));
							response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_OK);
						}
						else {
							//System.out.println("unsuccessful addtoken");
							response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_BAD_REQUEST);
						}
						break;
					// this user has already logged in
					case -1:
						//System.out.println("conflict user");
						response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_CONFLICT);
						break;
					// incorrect username or password; or query fail
					case -2:
						//System.out.println("wrong user");
						response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_BAD_REQUEST);
						break;
					default:
						//System.out.println("default");
						response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_BAD_REQUEST);
						break;
				}
				return;
			} else {
				//System.out.println("not http entity");
				response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_BAD_REQUEST);
				return;
			}	

	}
	
	private String generateToken(String username) {
		Base64.Encoder base64Encoder = Base64.getUrlEncoder();
		String newToken = base64Encoder.encodeToString(username.getBytes());
    	return newToken;
	}
}
