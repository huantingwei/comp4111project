package comp4111project.Handlers;

import java.io.IOException;
import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import comp4111project.DBConnection;
import comp4111project.QueryManager;
import comp4111project.Model.TransactionManager;

public class TransactionRequestHandler implements HttpRequestHandler {

	private final String TX = "Transaction";
	private final String TX_OP = "Operation";
	private final String TX_ACT = "Action";
	private final String TX_BKID = "Book";
	
	ConcurrentHashMap<String, Object> txData; 
	
	@Override
	public void handle(HttpRequest request, HttpResponse response, HttpContext context)
			throws HttpException, IOException {

		//TODO: what if the token contains "token="?	
//		String url = request.getRequestLine().getUri();
//		String token = URLDecoder.decode(url.substring(url.indexOf("token=")+6), "UTF-8");
//		if(!QueryManager.getInstance().validateToken(token)) {
//			response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
//			return;
//		}
		// parse transaction request body		
		if (request instanceof HttpEntityEnclosingRequest) {
			HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
			String content = EntityUtils.toString(entity, Consts.UTF_8);
			ObjectMapper mapper = new ObjectMapper();
		    txData = mapper.readValue(content, ConcurrentHashMap.class);		
		}
		else {
			txData = null;
		}
		
		
		switch(request.getRequestLine().getMethod()) {
		
		// request transaction id or
		// commit or cancel a transaction
		
		case("POST"):
			// commit or cancel a transaction
			if(txData!=null) {
				int txID = (int)txData.get(TX);
				int result;
				
				switch((String) txData.get(TX_OP)) {
				case("commit"):
					result = TransactionManager.getInstance().commitTx(txID);
					break;
					
				case("cancel"):
					result = TransactionManager.getInstance().cancelTx(txID);
					break;
				default:
					result = -1;
					break;
				}
				
				if(result == 1) response.setStatusCode(HttpStatus.SC_OK);
				else { response.setStatusCode(HttpStatus.SC_BAD_REQUEST); }
				
			}
			// request transaction id
			else {
				int txID = TransactionManager.getInstance().createTx();
				if(txID!=-1) {
					response.setStatusCode(HttpStatus.SC_OK);
					ObjectMapper mapper = new ObjectMapper();
					ObjectNode responseObject = mapper.createObjectNode();
	                responseObject.put(TX, txID);
	                response.setEntity(
	                        new StringEntity(responseObject.toString(),
	                                ContentType.APPLICATION_JSON)
	                );
				}
				else{ response.setStatusCode(HttpStatus.SC_BAD_REQUEST); }
			}
			break;
			
		// prepare operation
		case("PUT"):
			int result = TransactionManager.getInstance().addActionToTx((int)txData.get(TX), (String)txData.get(TX_ACT), (int)txData.get(TX_BKID));
			if(result == 1) response.setStatusCode(HttpStatus.SC_OK);
			else { response.setStatusCode(HttpStatus.SC_BAD_REQUEST); }			
			break;
		
		default:
			response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
			break;
		}
	}

}
