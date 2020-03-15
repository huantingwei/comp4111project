package comp4111project;

import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
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

	// database connection configuration file (in root folder)
	final static String dbConfigFile = "connection.prop";
	// randomly chosen 8081
	static int port = 8081;
	// root directory path for the service
	static String rootDirectory = "/BookManagementService";
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

    // global database connection pool
    public static DBSource db;
    // global record of access token (whether the user is logged in or not)
    // not sure if this is correct
    public static Map<String, String> userToToken = new HashMap<String, String>();    
    public static Map<String, String> tokenToUser = new HashMap<String, String>(); 
    
	public static void main(String[] args) throws Exception {
		
		/**
		 * Database connection: use a "connection pool"
		 */
		db = new BasicDBSource(dbConfigFile);
		
		
		/**
		 * Don't really know what these are yet!
		 */
        SSLContext sslContext = null;
        if (port == 8443) {
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

        LookupBookRequestHandler lookupBookRequestHandler = new LookupBookRequestHandler();
        ManageBookRequestHandler manageBookRequestHandler = new ManageBookRequestHandler();
		
		handlerMapper.register(rootDirectory + "/login", loginRequestHandler);
		handlerMapper.register(rootDirectory + "/logout", logoutRequestHandler);
//		handlerMapper.register(rootDirectory + "/books", deleteBookRequestHandler);
		// other requestHandlers
        handlerMapper.register( rootDirectory + "/books", lookupBookRequestHandler);
        handlerMapper.register( rootDirectory + "/books/*", manageBookRequestHandler);
		
		/**
		 * Set up the server
		 */
		SocketConfig socketConfig = SocketConfig.custom()
                .setSoTimeout(15000)
                .setTcpNoDelay(true)
                .build();

        final HttpServer server = ServerBootstrap.bootstrap()
                .setListenerPort(port)
                .setServerInfo("COMP4111-BookManagementServer/1.1")
                .setSocketConfig(socketConfig)
                .setSslContext(sslContext)
                .setExceptionLogger(new StdErrorExceptionLogger())
                .setHandlerMapper(handlerMapper)
                .create();

        try {
            server.start();
            System.out.println("Server has started with port " + server.getLocalPort());
            server.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        } catch (Exception err) {
            System.out.println("Error: "  + err);
        }

        // not sure what are these yet! 
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                server.shutdown(5, TimeUnit.SECONDS);
            }
        });
	}
}