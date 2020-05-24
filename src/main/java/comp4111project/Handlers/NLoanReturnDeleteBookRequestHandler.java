package comp4111project.Handlers;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
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
import org.apache.http.nio.protocol.BasicAsyncRequestConsumer;
import org.apache.http.nio.protocol.BasicAsyncResponseProducer;
import org.apache.http.nio.protocol.HttpAsyncExchange;
import org.apache.http.nio.protocol.HttpAsyncRequestConsumer;
import org.apache.http.nio.protocol.HttpAsyncRequestHandler;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import comp4111project.QueryManager;
import comp4111project.TokenManager;

public class NLoanReturnDeleteBookRequestHandler implements HttpAsyncRequestHandler<HttpRequest> {

	@Override
	public HttpAsyncRequestConsumer<HttpRequest> processRequest(HttpRequest request, HttpContext context)
			throws HttpException, IOException {
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
							//System.out.println("exception in loan return");
						}
						httpExchange.submitResponse(new BasicAsyncResponseProducer(response));
			}
		}.start();
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
		
        // loan, return, or delete book
		switch (request.getRequestLine().getMethod()) {
			case("PUT"):
				try {
					if (request instanceof HttpEntityEnclosingRequest) {
						HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
						String testContent = EntityUtils.toString(entity, Consts.UTF_8);
						ObjectMapper mapper = new ObjectMapper();
						ConcurrentHashMap<String, Boolean> isReturningBook = mapper.readValue(testContent, ConcurrentHashMap.class);

						String fullPath = request.getRequestLine().getUri();
						String[] paths = fullPath.split("/");
						// get book id
						String bookID = paths[paths.length-1].split("\\?")[0];

						Future<HttpResponse> returnLoanFuture = Executors.newSingleThreadExecutor().submit(() ->
								loanOrReturnBook(response, QueryManager.getInstance().returnAndLoanBook(bookID, isReturningBook.get("Available"))));

						try {
							response.setStatusLine(returnLoanFuture.get().getStatusLine());
						} catch (InterruptedException | ExecutionException e) {
							e.printStackTrace();
							response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_BAD_REQUEST);
						}
					}
				} catch(Exception e) {
					e.printStackTrace();
					response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_BAD_REQUEST);
				}
				break;
				
			case("DELETE"):
				try {
					URI uri = new URI(request.getRequestLine().getUri());
					String path = uri.getPath();
					String[] pairs = path.split("/");
					String bookID = pairs[pairs.length-1];

					Future<HttpResponse> deleteFuture = Executors.newSingleThreadExecutor().submit(() -> deleteBook(response, QueryManager.getInstance().deleteBook(bookID)));
					try {
						response.setStatusLine(deleteFuture.get().getStatusLine());
					} catch (InterruptedException | ExecutionException e) {
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
	
	private HttpResponse deleteBook(HttpResponse response, int result) {
		if(result == 1) {
			response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_OK);
		}
		else {
			response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_NOT_FOUND, "No book record");
		}
		return response;
	}
	/**
	 * This method verifies if book is loaned or returned successfully
	 * @param response
	 * @param result
	 * @return HttpResponse
	 */
	private HttpResponse loanOrReturnBook(HttpResponse response, int result) {
		switch(result) {
			case(0):
				response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_OK);
				break;
			case(-1):
				response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_NOT_FOUND, "No book record");
				break;
			case(-2):
				response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_BAD_REQUEST);
				break;
			default:
				break;
		}
		return response;
	}


}
