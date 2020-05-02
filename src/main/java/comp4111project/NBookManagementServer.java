package comp4111project;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

import org.apache.http.Consts;
import org.apache.http.ExceptionLogger;
import org.apache.http.HttpConnection;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.nio.bootstrap.HttpServer;
import org.apache.http.impl.nio.bootstrap.ServerBootstrap;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.entity.NFileEntity;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.nio.protocol.BasicAsyncRequestConsumer;
import org.apache.http.nio.protocol.BasicAsyncResponseProducer;
import org.apache.http.nio.protocol.HttpAsyncExchange;
import org.apache.http.nio.protocol.HttpAsyncRequestConsumer;
import org.apache.http.nio.protocol.HttpAsyncRequestHandler;
import org.apache.http.nio.protocol.UriHttpAsyncRequestHandlerMapper;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpProcessorBuilder;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;
import org.apache.http.ssl.SSLContexts;

import comp4111project.Handlers.NAddLookUpBookRequestHandler;
import comp4111project.Handlers.NLoginRequestHandler;
import comp4111project.Handlers.NLogoutRequestHandler;
import comp4111project.Handlers.NTestRequestHandler;

/**
 * Embedded HTTP/1.1 file server based on a non-blocking I/O model and capable of direct channel
 * (zero copy) data transfer.
 */
public class NBookManagementServer {

	
    public static void main(final String[] args) throws Exception {
    	// path
        int port = 8081;
        InetAddress host =InetAddress.getByName("localhost");
        String ROOT_DIRECTORY = "/BookManagementService";

        // initializing users
        //QueryManager.getInstance().initUser("user", Arrays.asList("userid", "Username", "Password"), 10000);
        
        SSLContext sslContext = null;
        if (port == 8443) {
            // Initialize SSL context
            final URL url = NBookManagementServer.class.getResource("/test.keystore");
            if (url == null) {
                System.out.println("Keystore not found");
                System.exit(1);
            }
            System.out.println("Loading keystore " + url);
            sslContext = SSLContexts.custom()
                    .loadKeyMaterial(url, "nopassword".toCharArray(), "nopassword".toCharArray())
                    .build();
        }
        // handlers
        UriHttpAsyncRequestHandlerMapper handlerMapper =
                new UriHttpAsyncRequestHandlerMapper();
        handlerMapper.register(ROOT_DIRECTORY + "/", new HttpFileHandler());
        handlerMapper.register(ROOT_DIRECTORY + "/login", new NLoginRequestHandler());
        handlerMapper.register(ROOT_DIRECTORY + "/logout", new NLogoutRequestHandler());
        handlerMapper.register(ROOT_DIRECTORY + "/books", new NAddLookUpBookRequestHandler());
        
        final IOReactorConfig config = IOReactorConfig.custom()
                .setSoTimeout(15000)
                .setTcpNoDelay(true)
                .build();
        final HttpServer server = ServerBootstrap.bootstrap()
                .setListenerPort(port)
                .setLocalAddress(host)
                .setServerInfo("COMP4111-BookManagementServer/1.1")
                .setIOReactorConfig(config)
                .setSslContext(sslContext)
                .setExceptionLogger(ExceptionLogger.STD_ERR)
                .setHandlerMapper(handlerMapper)
                .create();

        server.start();
        System.out.println("Serving on " + server.getEndpoint().getAddress()
                + (sslContext == null ? "" : " with " + sslContext.getProvider() + " " + sslContext.getProtocol()));
        server.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                server.shutdown(5, TimeUnit.SECONDS);
            }
        });

    }

    static class HttpFileHandler implements HttpAsyncRequestHandler<HttpRequest> {

        //private final File docRoot;

        public HttpFileHandler() {
            super();
            //this.docRoot = docRoot;
        }

        public HttpAsyncRequestConsumer<HttpRequest> processRequest(
                final HttpRequest request,
                final HttpContext context) {
            // Buffer request content in memory for simplicity
            return new BasicAsyncRequestConsumer();
        }

        public void handle(
                final HttpRequest request,
                final HttpAsyncExchange httpexchange,
                final HttpContext context) throws HttpException, IOException {
        	
            final HttpResponse response = httpexchange.getResponse();
            handleInternal(request, response, context);
            httpexchange.submitResponse(new BasicAsyncResponseProducer(response));
        }

        private void handleInternal(
                final HttpRequest request,
                final HttpResponse response,
                final HttpContext context) throws HttpException, IOException {

            final String method = request.getRequestLine().getMethod().toUpperCase(Locale.ENGLISH);
            if (!method.equals("GET") && !method.equals("HEAD") && !method.equals("POST")) {
                throw new MethodNotSupportedException(method + " method not supported");
            }
            
            response.setStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_OK);
            NStringEntity myEntity = new NStringEntity("<html><body>"
            		+ "<h1>Asynchronous I/O based on Non-blocking I/O!</h1>"
            		+ "</body></html>",
            		ContentType.create("text/html", "UTF-8"));
            response.setEntity(myEntity);
        }

    }

}