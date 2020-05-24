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
        
    public synchronized boolean removeUserAndToken(String token) {
    	try{
    		if(tokenToUser.containsKey(token)) {
    			String user = tokenToUser.remove(token);
    			userToToken.remove(user);
    			//System.out.println("removed token successfully");
    			return true;
    		}
    		else {
    			System.out.println("no user exist with this token");
    			return false;
    		}
    	} catch (Exception e) {
    		System.out.println("exception in removeUserAndToken()");
			return false;
		}    	
    }
    
    public synchronized boolean addUserAndToken(String user, String token) {
    	
		try {
			if(!tokenToUser.containsKey(token) && !userToToken.containsKey(user)) {
				tokenToUser.put(token, user);
				userToToken.put(user, token);
    			//System.out.println("add token successfully");
				return true;
			}
			else {
				System.out.println("token user already exist");
				return false;
			}
		} catch (Exception e) {
    		System.out.println("exception in addUserAndToken()");
			return false;
		}

    }
    
	public synchronized String generateNewToken(String username) {
		Base64.Encoder base64Encoder = Base64.getUrlEncoder();
		String newToken = base64Encoder.encodeToString(username.getBytes());

    	return newToken;
	}
    
	// TODO: deprecated, moved to NLogoutRequestHandler and NAddLookUpBookRequestHandler
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
    
    public synchronized Boolean validateToken(String token) {
		return tokenToUser.containsKey(token); 
    }

    public synchronized Boolean validateUser(String username) {
    	return userToToken.containsKey(username);
    }

    public String getUserFromToken(String token) {
    	return tokenToUser.get(token);
    }
    
    public String getTokenFromUser(String username) {
    	return userToToken.get(username);
    }
    
}
