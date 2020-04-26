package comp4111project.Handlers;

import java.io.IOException;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.nio.protocol.BasicAsyncRequestConsumer;
import org.apache.http.nio.protocol.BasicAsyncResponseProducer;
import org.apache.http.nio.protocol.HttpAsyncExchange;
import org.apache.http.nio.protocol.HttpAsyncRequestConsumer;
import org.apache.http.nio.protocol.HttpAsyncRequestHandler;
import org.apache.http.protocol.HttpContext;

public class NLoginRequestHandler implements HttpAsyncRequestHandler<HttpRequest> {

	public NLoginRequestHandler() {
		super();
	}

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
		
		if(!request.getRequestLine().getMethod().equals("POST")) {
			response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_BAD_REQUEST);
			return;
		}
		response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_OK);
        NStringEntity myEntity = new NStringEntity("<html><body>"
        		+ "<h1>This is a Login request</h1>"
        		+ "</body></html>",
        		ContentType.create("text/html", "UTF-8"));
        response.setEntity(myEntity);
	}
}
