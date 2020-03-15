package comp4111project.Handlers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import comp4111project.DBConnection;
import comp4111project.Model.Book;
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

            DBConnection dbCon = new DBConnection(); // Problem
            Vector<Book> foundBooks = new Vector(dbCon.getBooks(query_pairs));

            Gson gson = new Gson();
            JsonObject jsonResponse = new JsonObject();

            jsonResponse.addProperty("FoundBooks", foundBooks.size());
            jsonResponse.add("Results", gson.toJsonTree(foundBooks));


            response.setStatusCode(HttpStatus.SC_OK);
            response.setEntity(
                    new StringEntity(jsonResponse.toString(),
                            ContentType.TEXT_PLAIN)
            );
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }
}
