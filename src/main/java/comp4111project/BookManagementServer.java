package comp4111project;

import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

import org.apache.http.ConnectionClosedException;
import org.apache.http.ExceptionLogger;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.apache.http.protocol.UriHttpRequestHandlerMapper;
import org.apache.http.ssl.SSLContexts;

public class BookManagementServer {

	static int port = 8080;
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

	public static void main(String[] args) throws Exception {
		
		/**
		 * Database connection: use a "connection pool"
		 */
		
		
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
		handlerMapper.register(rootDirectory + "/login", loginRequestHandler);
		handlerMapper.register(rootDirectory + "/logout", logoutRequestHandler);
		// other requestHandlers
		
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
