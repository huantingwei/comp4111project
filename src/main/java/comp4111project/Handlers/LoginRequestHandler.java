package comp4111project.Handlers;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.http.Consts;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.RequestLine;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import comp4111project.BookManagementServer;
import comp4111project.QueryManager;

public class LoginRequestHandler implements HttpRequestHandler {

	@Override
	public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {

		// parse the request body to Map<String, String> inputData
		if (request instanceof HttpEntityEnclosingRequest) {
			HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
			String userContent = EntityUtils.toString(entity, Consts.UTF_8);
			ObjectMapper mapper = new ObjectMapper();
			ConcurrentHashMap<String, Object> user = mapper.readValue(userContent, ConcurrentHashMap.class);
			
			Future<Integer> sucessLoginFuture = Executors.newSingleThreadExecutor().submit(() -> QueryManager.getInstance().loginUser(user));
			try {
				switch (sucessLoginFuture.get()) {
					case 1:
						response.setStatusCode(HttpStatus.SC_OK);
						String username = (String) user.get("Username");
						String newToken = generateNewToken(username);
						String rsp = "{ \"Token\" : \"" + newToken + "\" }";
						response.setEntity(
								new StringEntity(rsp,
										ContentType.APPLICATION_JSON));
						Executors.newSingleThreadExecutor().execute(() -> QueryManager.getInstance().addUserAndToken(username, newToken));
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
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}
	}

	private String generateNewToken(String usr) {
		// assume no duplicate username
		Base64.Encoder base64Encoder = Base64.getUrlEncoder();
		String newToken = base64Encoder.encodeToString(usr.getBytes());

    	return newToken;
	}
}
