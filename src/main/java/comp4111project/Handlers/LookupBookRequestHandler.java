package comp4111project.Handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import comp4111project.DBConnection;
import comp4111project.Model.Book;
import comp4111project.QueryManager;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

public class LookupBookRequestHandler implements HttpRequestHandler {

    @Override
    public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
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
    }
}