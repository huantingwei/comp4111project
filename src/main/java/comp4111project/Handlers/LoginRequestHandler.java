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
		
		System.out.println("Loggin request");
		
		// parse the request body to Map<String, String> inputData
		if (request instanceof HttpEntityEnclosingRequest) {
			HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
			String userContent = EntityUtils.toString(entity, Consts.UTF_8);
			ObjectMapper mapper = new ObjectMapper();
			ConcurrentHashMap<String, Object> user = mapper.readValue(userContent, ConcurrentHashMap.class);

			Future<Integer> successLoginFuture = Executors.newSingleThreadExecutor().submit(() -> QueryManager.getInstance().loginUser(user));
			try {
				switch (successLoginFuture.get()) {
					case 1:
						response.setStatusCode(HttpStatus.SC_OK);
						String username = (String) user.get("Username");
						String newToken = TokenManager.getInstance().generateNewToken(username);
						ObjectNode responseObject = new ObjectMapper().createObjectNode();
							responseObject.put("Token", newToken);
							response.setEntity(
									new StringEntity(responseObject.toString(),
											ContentType.APPLICATION_JSON));
            
						Executors.newSingleThreadExecutor().execute(() -> TokenManager.getInstance().addUserAndToken(username, newToken));
						break;
					case -1:
						response.setStatusCode(HttpStatus.SC_CONFLICT);
						break;
					case -2:
						response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
						break;
					default:
						response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
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
	}

}
