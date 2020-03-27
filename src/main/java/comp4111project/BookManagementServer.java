package comp4111project;

import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

import comp4111project.Handlers.*;
import org.apache.http.ConnectionClosedException;
import org.apache.http.ExceptionLogger;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.apache.http.protocol.UriHttpRequestHandlerMapper;
import org.apache.http.ssl.SSLContexts;

public class BookManagementServer {
	
	public static String HOST = "localhost";
	public static int PORT = 8081;
	public static String ROOT_DIRECTORY = "/BookManagementService";
    
    static class StdErrorExceptionLogger implements ExceptionLogger {

        @Override
        public void log(final Exception ex) {
            if (ex instanceof SocketTimeoutException) {
                System.err.println("Connection timed out");
            } else if (ex instanceof ConnectionClosedException) {
            	System.err.println("instance of ConnectionClosedException");
                System.err.println(ex.getMessage());
            } else {
                ex.printStackTrace();
            }
        }

    }	

    
	public static void main(String[] args) throws Exception {
		
        SSLContext sslContext = null;
        if (PORT == 8443) {
            // Initialize SSL context
            URL url = BookManagementServer.class.getResource("/my.keystore");
            if (url == null) {
                System.out.println("Keystore not found");
                System.exit(1);
            }
            sslContext = SSLContexts.custom()
                    .loadKeyMaterial(url, "secret".toCharArray(), "secret".toCharArray())
                    .build();
        }
        
//        DBConnection conn = new DBConnection("connection.prop");
//        conn.initUser("user", Arrays.asList("userid", "Username", "Password"), 100);
//        System.out.println("Finished inserting 100 users");
        
        /**
         * Map the RequestHandlers to each urls
         */
		UriHttpRequestHandlerMapper handlerMapper = new UriHttpRequestHandlerMapper();
		LoginRequestHandler loginRequestHandler = new LoginRequestHandler();
		LogoutRequestHandler logoutRequestHandler = new LogoutRequestHandler();
		LoanReturnDeleteBookRequestHandler manageBookRequestHandler = new LoanReturnDeleteBookRequestHandler();
        AddLookUpBookRequestHandler addLookUpBookRequestHandler = new AddLookUpBookRequestHandler();
		TransactionRequestHandler transactionRequestHandler = new TransactionRequestHandler();
        
		handlerMapper.register(ROOT_DIRECTORY+ "/login", loginRequestHandler);
		handlerMapper.register(ROOT_DIRECTORY + "/logout", logoutRequestHandler);
        handlerMapper.register(ROOT_DIRECTORY + "/books", addLookUpBookRequestHandler);
		handlerMapper.register(ROOT_DIRECTORY + "/books/*", manageBookRequestHandler);
		handlerMapper.register(ROOT_DIRECTORY + "/transaction", transactionRequestHandler);
		
		/**
		 * Set up the server
		 */
		SocketConfig socketConfig = SocketConfig.custom()
                .setSoTimeout(15000)
                .setTcpNoDelay(true)
                .build();

        final HttpServer server = ServerBootstrap.bootstrap()
                .setListenerPort(PORT)
                .setServerInfo("COMP4111-BookManagementServer/1.1")
                .setSocketConfig(socketConfig)
                .setSslContext(sslContext)
                .setExceptionLogger(new StdErrorExceptionLogger())
                .setHandlerMapper(handlerMapper)
                .create();

        server.start();
        System.out.println("Server has started with port " + server.getLocalPort());
        server.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        
        
        // not sure what are these yet! 
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                server.shutdown(5, TimeUnit.SECONDS);
            }
        });
	}
	


}
