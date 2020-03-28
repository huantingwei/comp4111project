package comp4111project.Handlers;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import comp4111project.QueryManager;
import comp4111project.TokenManager;

import org.apache.http.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.util.EntityUtils;

public class LoanReturnDeleteBookRequestHandler implements HttpRequestHandler {
	
	@Override
	public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
		System.out.println("Loan, return, delete book");

		// validate token
		if(!TokenManager.getInstance().validateTokenFromURI(request.getRequestLine().getUri())) {
			response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
			return;
		}
        
		switch (request.getRequestLine().getMethod()) {
			case("PUT"):
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
					} catch (InterruptedException e) {
						e.printStackTrace();
					} catch (ExecutionException e) {
						e.printStackTrace();
					}

				}
				break;
			case("DELETE"):
				try {
					URI uri = new URI(request.getRequestLine().getUri());
					String path = uri.getPath();
					String[] pairs = path.split("/");
					Integer bookID = Integer.parseInt(pairs[pairs.length-1]);

					Future<HttpResponse> deleteFuture = Executors.newSingleThreadExecutor().submit(() -> deleteBook(response, QueryManager.getInstance().deleteBook(bookID)));
					try {
						response.setStatusLine(deleteFuture.get().getStatusLine());
					} catch (InterruptedException e) {
						e.printStackTrace();
					} catch (ExecutionException e) {
						e.printStackTrace();
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
	

	private HttpResponse deleteBook(HttpResponse response, Boolean result) {
		if(result) {
			response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_OK);
		}
		else {
			response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_NOT_FOUND, "No book record");
		}
		return response;
	}
	
	public HttpResponse loanOrReturnBook(HttpResponse response, int result) {
		switch(result) {
			case(0):
				response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, "Successfully returned/loaned");
				break;
			case(1):
				response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_NOT_FOUND, "No book record");
				break;
			case(2):
				response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_BAD_REQUEST, "Already returned/loaned");
				break;
			default:
				break;
		}
		return response;
	}
}