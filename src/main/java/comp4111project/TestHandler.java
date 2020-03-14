package comp4111project;

import org.apache.http.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

public class TestHandler implements HttpRequestHandler {

    DBConnection dbConnection;

    public TestHandler(DBConnection test) {
        this.dbConnection = test;
        System.out.println("this is constructor bitch");
    }

    @Override
    public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {
        System.out.println(request.getRequestLine());
        System.out.println(request);

        String hi = "";

        if (request instanceof HttpEntityEnclosingRequest) {
            HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
//            byte[] entityContent = EntityUtils.toByteArray(entity);
            String testContent = EntityUtils.toString(entity, Consts.UTF_8);
            hi = testContent;
            System.out.println(testContent);
//            System.out.println("Incoming entity content (bytes): " + entityContent.length);
        }

        dbConnection.getAllUser();

        response.setStatusCode(HttpStatus.SC_OK);
        response.setEntity(
                new StringEntity( hi,
                        ContentType.TEXT_PLAIN));
    }

}
