package comp4111project.Handlers;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import comp4111project.BookManagementServer;
import comp4111project.QueryManager;
import comp4111project.Model.Book;

public class AddLookUpBookRequestHandler implements HttpRequestHandler {

	@Override
	public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
		System.out.println("Adding or Looking Up a Book");
		System.out.println(request.getRequestLine().getMethod());
		
		//TODO: validate token
		String url = request.getRequestLine().getUri();
		String token = url.substring(url.indexOf("token=")+6);
		if(!QueryManager.getInstance().validateToken(token)) {
			response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
			return;
		}
		
		switch (request.getRequestLine().getMethod()) {
		
			// Add
			case("POST"):
				
				if(request instanceof HttpEntityEnclosingRequest) {
					HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
					String bookContent = EntityUtils.toString(entity, Consts.UTF_8);
					ObjectMapper mapper = new ObjectMapper();
					ConcurrentHashMap<String, Object> newBook = mapper.readValue(bookContent, ConcurrentHashMap.class);
					
					addBook(response, QueryManager.getInstance().addBook(newBook), token);
				}
				
				break;
				
			// Look up
			case("GET"):
		        try {
		            URI uri = new URI(request.getRequestLine().getUri());
		            ConcurrentHashMap<String, String> query_pairs = new ConcurrentHashMap<String, String>();
		            String query = uri.getQuery();
		            String[] pairs = query.split("&");
		            for (String pair : pairs) {
		                int idx = pair.indexOf("=");
		                query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
		            }

		            for (ConcurrentHashMap.Entry<String, String> entry : query_pairs.entrySet()) {
		                System.out.println(entry.getKey() + "/" + entry.getValue());
		            }

		            Vector<Book> foundBooks = new Vector(QueryManager.getInstance().getBooks(query_pairs));

		            if (foundBooks.isEmpty()) {
		                response.setStatusCode(HttpStatus.SC_NO_CONTENT);
		                response.setEntity(
		                        new StringEntity("204 No Content",
		                                ContentType.TEXT_PLAIN)
		                );
		            } else {
		                ObjectMapper mapper = new ObjectMapper();
		                ObjectNode responseObject = mapper.createObjectNode();
		                responseObject.put("FoundBooks", foundBooks.size());
		                responseObject.putPOJO("Results", foundBooks);

		                response.setStatusCode(HttpStatus.SC_OK);
		                response.setEntity(
		                        new StringEntity(responseObject.toString(),
		                                ContentType.TEXT_PLAIN)
		                );
		            }
		        } catch (URISyntaxException e) {
		            e.printStackTrace();
		        }
			
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
	
	private void addBook(HttpResponse response, int newBookID, String token) {
		if(newBookID == -1) {
            response.setStatusCode(HttpStatus.SC_CONFLICT);
		}
		else if(newBookID == -2) {
			response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
		}
		else {
			response.addHeader("Location", "/books/" + newBookID);
            response.setStatusCode(HttpStatus.SC_CREATED);
            response.setEntity(
                    new StringEntity("http://" + BookManagementServer.HOST + ":" + BookManagementServer.PORT
                    		+ BookManagementServer.ROOT_DIRECTORY + "/books/" + newBookID + "?token=" + token,
                            ContentType.TEXT_PLAIN));
		}
	}


}
