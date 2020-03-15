package comp4111project.Handlers;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.JSONPObject;
import org.apache.http.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.util.EntityUtils;

public class ManageBookRequestHandler implements HttpRequestHandler {

	@Override
	public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
		System.out.println("Manageing Book");
		System.out.println(request.getRequestLine().getMethod());

		switch (request.getRequestLine().getMethod()) {
			case("PUT"):
				if (request instanceof HttpEntityEnclosingRequest) {
					HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
					String testContent = EntityUtils.toString(entity, Consts.UTF_8);
					ObjectMapper mapper = new ObjectMapper();
					ConcurrentHashMap<String, Boolean> isReturningBook = mapper.readValue(testContent, ConcurrentHashMap.class);

					String fullPath = request.getRequestLine().getUri();
					String[] paths = fullPath.split("/");
					String bookID = paths[paths.length - 1];

					if(isReturningBook.get("Available")) {
						System.out.println("this is returning request");
					} else {
						LoanBook(response, bookID);
					}
				}
				break;
			case("DELETE"):
				break;
			default:
				response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
				response.setEntity(
						new StringEntity("Bad Request",
								ContentType.TEXT_PLAIN)
				);
				break;
		}
	}

	public void LoanBook(HttpResponse response, String bookID) {
		response.setEntity(
				new StringEntity("Loaning a book",
						ContentType.TEXT_PLAIN)
		);
	}
}
