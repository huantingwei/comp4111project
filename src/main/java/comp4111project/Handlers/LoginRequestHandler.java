package comp4111project.Handlers;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
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
			
			int successLogin = QueryManager.getInstance().loginUser(user);
			switch (successLogin) {
			case(1):
				response.setStatusCode(HttpStatus.SC_OK);
				String username = (String) user.get("Username");
				String newToken = TokenManager.getInstance().generateNewToken(username);
            	
				ObjectNode responseObject = new ObjectMapper().createObjectNode();
                responseObject.put("Token", newToken);
                response.setEntity(
                        new StringEntity(responseObject.toString(),
                                ContentType.APPLICATION_JSON));

            	TokenManager.getInstance().addUserAndToken(username, newToken);
            	break;
			case(-1):
				response.setStatusCode(HttpStatus.SC_CONFLICT);
				break;
			case(-2):
				response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
				break;
			default:
				response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
				break;
			}
				
		}
	}

}
