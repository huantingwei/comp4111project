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


import org.apache.http.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import comp4111project.BookManagementServer;
import comp4111project.QueryManager;
import comp4111project.TokenManager;
import comp4111project.Model.Book;

public class AddLookUpBookRequestHandler implements HttpRequestHandler {

	@Override
	public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
		System.out.println("Adding or Looking Up a Book");
		System.out.println(request.getRequestLine().getMethod());
		
		Future<Boolean> validateTokenFuture = Executors.newSingleThreadExecutor().submit(() ->TokenManager.getInstance().validateTokenFromURI(request.getRequestLine().getUri()));
		try {
			if(!validateTokenFuture.get()) {
				response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
				return;
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		
		switch (request.getRequestLine().getMethod()) {
		
			// Add
			case("POST"):
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
            } catch (ExecutionException e) {
              e.printStackTrace();
            }
					} catch (URISyntaxException | UnsupportedEncodingException e) {
						e.printStackTrace();
					}
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

                      response.setStatusCode(HttpStatus.SC_OK);
                      response.setEntity(
                          new StringEntity(responseObject.toString(),
                              ContentType.TEXT_PLAIN)
                      );
                    }
                  } catch (InterruptedException e) {
                    e.printStackTrace();
                  } catch (ExecutionException e) {
                    e.printStackTrace();
                  }
                } catch(Exception e) {

                }
		        } catch (URISyntaxException e) {
		            e.printStackTrace();
		        }

				break;
			
			default:
				response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_BAD_REQUEST);
				break;
		}
	}
	
	private HttpResponse addBook(HttpResponse response, int newBookID, String token) {
		if(newBookID < 0) {
			response.addHeader("Duplicate record:", "/books/"+Integer.toString(-newBookID));
            response.setStatusCode(HttpStatus.SC_CONFLICT);
		}
		else if(newBookID == 0) {
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
		return response;
	}


}
