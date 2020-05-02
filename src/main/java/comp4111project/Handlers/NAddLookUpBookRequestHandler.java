package comp4111project.Handlers;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.Vector;
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
import org.apache.http.nio.protocol.BasicAsyncRequestConsumer;
import org.apache.http.nio.protocol.BasicAsyncResponseProducer;
import org.apache.http.nio.protocol.HttpAsyncExchange;
import org.apache.http.nio.protocol.HttpAsyncRequestConsumer;
import org.apache.http.nio.protocol.HttpAsyncRequestHandler;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import comp4111project.BookManagementServer;
import comp4111project.QueryManager;
import comp4111project.TokenManager;

public class NAddLookUpBookRequestHandler implements HttpAsyncRequestHandler<HttpRequest> {

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
		
		// validate token
		Future<Boolean> validateTokenFuture = Executors.newSingleThreadExecutor().submit(() ->TokenManager.getInstance().validateTokenFromURI(request.getRequestLine().getUri()));
		try {
			if(!validateTokenFuture.get()) {
				response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_BAD_REQUEST);
				return;
			}
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		} 
		
		switch (request.getRequestLine().getMethod()) {
		
			// Add
			case("POST"):
				try {
					if(request instanceof HttpEntityEnclosingRequest) {
						HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
						String bookContent = EntityUtils.toString(entity, Consts.UTF_8);
						ObjectMapper mapper = new ObjectMapper();
						ConcurrentHashMap<String, Object> newBook = mapper.readValue(bookContent, ConcurrentHashMap.class);

						try {
							// get token
							URI uri = new URI(request.getRequestLine().getUri());
							ConcurrentHashMap<String, String> query_pairs = new ConcurrentHashMap<String, String>();
							String query = uri.getQuery();
							String[] pairs = query.split("&");

							for (String pair : pairs) {
								int idx = pair.indexOf("=");
								query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
							}
							String token = query_pairs.get("token");

							Future<HttpResponse> addFuture = Executors.newSingleThreadExecutor().submit(() -> addBook(response, QueryManager.getInstance().addBook(newBook), token));
							try {
								response.setStatusCode(addFuture.get().getStatusLine().getStatusCode());
								response.setEntity(addFuture.get().getEntity());
							} catch (InterruptedException e) {
								e.printStackTrace();
								response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_BAD_REQUEST);
							} catch (ExecutionException e) {
								e.printStackTrace();
								response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_BAD_REQUEST);
							}
						} catch (URISyntaxException | UnsupportedEncodingException e) {
							e.printStackTrace();
							response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_BAD_REQUEST);
						}
					}
				} catch(Exception e) {
					e.printStackTrace();
					response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_BAD_REQUEST);
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
					try {
					  Future<Vector> future = Executors.newSingleThreadExecutor().submit(() -> QueryManager.getInstance().getBooks(query_pairs));
					  try {
						Vector foundBooks = future.get();
						if (foundBooks.isEmpty()) {
						  response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_NO_CONTENT, "No Content");
						} else {
						  ObjectMapper mapper = new ObjectMapper();
						  ObjectNode responseObject = mapper.createObjectNode();
						  responseObject.put("FoundBooks", foundBooks.size());
						  responseObject.putPOJO("Results", foundBooks);

						  response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_OK);
						  response.setEntity(
							  new StringEntity(responseObject.toString(),
								  ContentType.TEXT_PLAIN)
						  );
						}
					  } catch (InterruptedException e) {
						e.printStackTrace();
						response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_BAD_REQUEST);
					  } catch (ExecutionException e) {
						e.printStackTrace();
						response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_BAD_REQUEST);
					  }
                } catch(Exception e) {
					e.printStackTrace();
					response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_BAD_REQUEST);
                }
		        } catch (URISyntaxException e) {
		            e.printStackTrace();
					response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_BAD_REQUEST);
		        }
				break;
			default:
				response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_BAD_REQUEST);
				break;
		}
	}
	
	private HttpResponse addBook(HttpResponse response, long newBookID, String token) {
		if(newBookID < 0) {
			response.addHeader("Duplicate record:", "/books/"+ -newBookID);
			response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_CONFLICT);
		}
		else if(newBookID == 0) {
			response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_BAD_REQUEST);
		}
		else {
			response.addHeader("Location", "/books/" + newBookID);
			response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_CREATED);
            response.setEntity(
                    new StringEntity("http://" + BookManagementServer.HOST + ":" + BookManagementServer.PORT
                    		+ BookManagementServer.ROOT_DIRECTORY + "/books/" + newBookID + "?token=" + token,
                            ContentType.TEXT_PLAIN));
		}
		return response;
	}
	

}
