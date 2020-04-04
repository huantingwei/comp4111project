package comp4111project.Handlers;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.http.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import comp4111project.QueryManager;
import comp4111project.TokenManager;

public class LoginRequestHandler implements HttpRequestHandler {

	@Override
	public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
		
		// TODO: redundant?
		// check method == POST
		if(!request.getRequestLine().getMethod().equals("POST")) {
			response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_BAD_REQUEST);
			return;
		}
		
		// parse the request body to Map<String, String> inputData
		if (request instanceof HttpEntityEnclosingRequest) {
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
									new StringEntity(responseObject.toString(),
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
			} catch (InterruptedException e) {
				e.printStackTrace();
				response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_BAD_REQUEST);
			} catch (ExecutionException e) {
				e.printStackTrace();
				response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_BAD_REQUEST);
			}
		}
		else {
			response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_BAD_REQUEST);
		}
	}

}
