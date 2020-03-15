package comp4111project.Handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.JSONPObject;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import comp4111project.DBConnection;
import comp4111project.Model.Book;
import org.apache.http.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

public class TestHandler implements HttpRequestHandler {

    @Override
    public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
        String hi = "";



//        if (request instanceof HttpEntityEnclosingRequest) {
//            HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
////            byte[] entityContent = EntityUtils.toByteArray(entity);
//            String testContent = EntityUtils.toString(entity, Consts.UTF_8);
//            hi = testContent;
//            System.out.println(testContent);
////            System.out.println("Incoming entity content (bytes): " + entityContent.length);
//        }


    }
}
