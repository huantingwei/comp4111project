package comp4111project;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;


public class TokenManager {

    private ConcurrentHashMap<String, String> userToToken;
    private ConcurrentHashMap<String, String> tokenToUser;
    
	private TokenManager() {
		userToToken = new ConcurrentHashMap<String, String>();
		tokenToUser = new ConcurrentHashMap<String, String>();
	}
    private static class BillPushSingleton {
        private static final TokenManager INSTANCE = new TokenManager();
    }
    
    public static TokenManager getInstance() {
        return BillPushSingleton.INSTANCE;
    }
    
    public Boolean validateTokenFromURI(String url) {
    	String token = getTokenFromURI(url);
    	if(token!=null) {
    		return validateToken(token);
    	}
    	else return false;
    }
        
    public synchronized void removeUserAndToken(String token) {
    	String user = getUserFromToken(token);
    	tokenToUser.remove(token);
    	userToToken.remove(user);
    }
    
    public synchronized void addUserAndToken(String user, String token) {
    	tokenToUser.put(token, user);
    	userToToken.put(user, token);
    }
    
	public String generateNewToken(String username) {
		Base64.Encoder base64Encoder = Base64.getUrlEncoder();
		String newToken = base64Encoder.encodeToString(username.getBytes());

    	return newToken;
	}
    
    public String getTokenFromURI(String url) {
    	String token = null;
    	try {
    		URI uri = new URI(url);
			ConcurrentHashMap<String, String> query_pairs = new ConcurrentHashMap<String, String>();
	        String query = uri.getQuery();
	        String[] pairs = query.split("&");
	        for (String pair : pairs) {
	            int idx = pair.indexOf("=");
	            query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
	        }
	        token = query_pairs.get("token");
    	} catch (URISyntaxException | UnsupportedEncodingException e) {
			e.printStackTrace();
		}
    	return token;
    }
    
    public Boolean validateToken(String token) {
        return tokenToUser.get(token) != null;
    }

    public Boolean validateUser(String username) {
    	return userToToken.get(username) != null;
    }

    public String getUserFromToken(String token) {
    	return tokenToUser.get(token);
    }
    
    public String getTokenFromUser(String username) {
    	return userToToken.get(username);
    }
    
}
