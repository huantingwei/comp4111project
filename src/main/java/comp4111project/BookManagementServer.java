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

import org.apache.http.ConnectionClosedException;
import org.apache.http.ExceptionLogger;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.apache.http.protocol.UriHttpRequestHandlerMapper;
import org.apache.http.ssl.SSLContexts;

import comp4111project.Handlers.AddBookRequestHandler;
import comp4111project.Handlers.LoginRequestHandler;
import comp4111project.Handlers.LogoutRequestHandler;
import comp4111project.Handlers.ManageBookRequestHandler;

public class BookManagementServer {

	// database connection configuration file (in root folder)
	final static String DB_CONFIG_FILE = "connection.prop";
    // global database connection pool
    public static DBConnection DB;	
	
	public static String HOST = "localhost";
	public static int PORT = 8081;
	public static String ROOT_DIRECTORY = "/BookManagementService";
	

    // global record of access token (whether the user is logged in or not)
    // not sure if this is correct
    public final static ConcurrentHashMap<String, String> USER_TOKEN = new ConcurrentHashMap<String, String>();    
    public final static ConcurrentHashMap<String, String> TOKEN_USER = new ConcurrentHashMap<String, String>(); 
    
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
		
		/**
		 * Database connection: use a "connection pool"
		 */
		DB = new DBConnection(DB_CONFIG_FILE);		
		/**
		 * Don't really know what these are yet!
		 */
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
        
        /**
         * Map the RequestHandlers to each urls
         */
		UriHttpRequestHandlerMapper handlerMapper = new UriHttpRequestHandlerMapper();
		LoginRequestHandler loginRequestHandler = new LoginRequestHandler();
		LogoutRequestHandler logoutRequestHandler = new LogoutRequestHandler();
		AddBookRequestHandler addBookRequestHandler = new AddBookRequestHandler();
		ManageBookRequestHandler manageBookRequestHandler = new ManageBookRequestHandler();
		
		handlerMapper.register(ROOT_DIRECTORY+ "/login", loginRequestHandler);
		handlerMapper.register(ROOT_DIRECTORY + "/logout", logoutRequestHandler);
		// TODO: how to map request handlers under same path
		handlerMapper.register(ROOT_DIRECTORY + "/books/*", manageBookRequestHandler);
		handlerMapper.register(ROOT_DIRECTORY + "/books", addBookRequestHandler);
		// other requestHandlers
		
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
