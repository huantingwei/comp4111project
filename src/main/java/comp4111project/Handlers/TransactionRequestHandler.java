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

import comp4111project.TokenManager;
import comp4111project.TransactionManager;

public class TransactionRequestHandler implements HttpRequestHandler {

	private final String TX = "Transaction";
	private final String TX_OP = "Operation";
	private final String TX_ACT = "Action";
	private final String TX_BKID = "Book";
	
	ConcurrentHashMap<String, Object> txData = null;
	
	@Override
	public void handle(HttpRequest request, HttpResponse response, HttpContext context)
			throws HttpException, IOException {
		
		ConcurrentHashMap<String, Object> txData = null;
		
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
		
		// parse transaction request body
		// verify if empty
		if (request instanceof HttpEntityEnclosingRequest) {
			HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
			String content = EntityUtils.toString(entity, Consts.UTF_8);
			try {
				if(!content.equals("")) {
					ObjectMapper mapper = new ObjectMapper();
					txData = mapper.readValue(content, ConcurrentHashMap.class);
				}
			} catch (Exception e) {
				e.printStackTrace();
				response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_BAD_REQUEST);
			}
		}
		
		switch(request.getRequestLine().getMethod()) {

			// request transaction id or
			// commit or cancel a transaction
			case("POST"):
				// commit or cancel a transaction
				if(txData != null) {
					long txID = ((Number)txData.get(TX)).longValue();
					Future<Integer> resultFuture = null;

					switch((String) txData.get(TX_OP)) {
						case("commit"):
							resultFuture = Executors.newSingleThreadExecutor().submit(() -> TransactionManager.getInstance().commitTx(txID));
							break;

						case("cancel"):
							resultFuture = Executors.newSingleThreadExecutor().submit(() -> TransactionManager.getInstance().cancelTx(txID));
							break;
						default:
							break;
					}
					
					try {
						if(resultFuture!=null) {
							//TODO: what happen if .get() doesn't return anything?
							if(resultFuture.get() == 1) { 
								response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_OK);
							} else {
								response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_BAD_REQUEST);
							}
						} else {
							response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_BAD_REQUEST);
						}
					} catch (InterruptedException | ExecutionException e) {
						e.printStackTrace();
						response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_BAD_REQUEST);
					}
				}
			
				// request transaction id
				else {
					long txID = -1;
					Future<Long> txIDFuture = Executors.newSingleThreadExecutor().submit(() -> TransactionManager.getInstance().createTx());
					try {
						txID = txIDFuture.get();
					} catch (InterruptedException | ExecutionException e) {
						e.printStackTrace();
						response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_BAD_REQUEST);
					}
					
					if(txID!=-1) {
						response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_OK);
						ObjectMapper mapper = new ObjectMapper();
						ObjectNode responseObject = mapper.createObjectNode();
						responseObject.put(TX, txID);
						response.setEntity(
								new StringEntity(responseObject.toString(),
										ContentType.APPLICATION_JSON)
						);
					}
					else{ response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_BAD_REQUEST); }
				}
				break;

			// prepare operation
			case("PUT"):
				long txID = ((Number)txData.get(TX)).longValue();
				String txAct = (String)txData.get(TX_ACT);
				long txBkID = ((Number)txData.get(TX_BKID)).longValue();
				
				Future<Integer> result = Executors.newSingleThreadExecutor().submit(() -> TransactionManager.getInstance().addActionToTx(txID, txAct, txBkID));
				try {
					if(result.get() == 1) {
						response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_OK);
					} else {
						response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_BAD_REQUEST);
					}
				} catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
					response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_BAD_REQUEST);
				}
				break;
			// invalid method
			default:
				response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_BAD_REQUEST);
				break;
		}

	}

}
