package org.edu_sharing.service.authentication.oauth2;

import java.io.File;
import java.io.Serializable;
import java.util.Map;

import org.alfresco.repo.cache.SimpleCache;
import org.alfresco.repo.content.ContentStore;
import org.apache.log4j.Logger;
import org.apache.oltu.oauth2.as.issuer.MD5Generator;
import org.apache.oltu.oauth2.as.issuer.OAuthIssuer;
import org.apache.oltu.oauth2.as.issuer.OAuthIssuerImpl;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.server.authentication.ContextManagementFilter;
import org.springframework.context.ApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

public class TokenService {

	private static final String STORE_LOCATION = "tokenStore";
	Logger logger = Logger.getLogger(TokenService.class);

	private SimpleCache<String, Token> tokenCache;
	private File tokenStore;
	
	private long expiresIn;
	private Map<String, String> trustedClients;
	
    @SuppressWarnings("unchecked")
	public void init(){
    	
		tokenStore = new File(getRootLocation(), STORE_LOCATION);
		
		if (! tokenStore.exists()) {
			tokenStore.mkdir();
		}
		
		tokenCache = 
				(SimpleCache<String, Token>) 
					AlfAppContextGate.getApplicationContext().getBean("eduSharingTokenCache");
	}
    
    public long getExpiresIn() {
		return expiresIn;
	}

	public void setExpiresIn(long expiresIn) {
		this.expiresIn = expiresIn;
	}

	public Map<String, String> getTrustedClients() {
		return trustedClients;
	}

	public void setTrustedClients(Map<String, String> trustedClients) {
		this.trustedClients = trustedClients;
	}

	private File getRootLocation() {
    	
		ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
		
		ContentStore store = (ContentStore) applicationContext.getBean("fileContentStore");
		return new File(store.getRootLocation()).getParentFile();
    }
    
    public Token createToken(String username, String accessToken, String refreshToken, String clientId, String ticket) throws Exception {
    
    	ObjectMapper mapper = new ObjectMapper();

    	Token token = new Token();
    	token.setUsername(username);
    	token.setAccessToken(accessToken);
    	token.setRefreshToken(refreshToken);
    	token.setClientId(clientId);
    	token.setTicket(ticket);
    	token.setTimestamp(System.currentTimeMillis());

    	tokenCache.put(accessToken, token);    	

    	mapper.writeValue(new File(tokenStore, refreshToken), token);
    	
    	return token;
    }
    public Token createToken(String username, String ticket) throws Exception {
    	return createToken(username,trustedClients.keySet().iterator().next(),ticket);
    }
    /** Creates a new token and creates new token strings itself
     * 
     * @param username
     * @param clientId
     * @param ticket
     * @return
     * @throws Exception
     */
    public Token createToken(String username, String clientId, String ticket) throws Exception {
        
		OAuthIssuer oauthIssuerImpl = new OAuthIssuerImpl(new MD5Generator());
		String accessToken = oauthIssuerImpl.accessToken();
		String refreshToken = oauthIssuerImpl.refreshToken();
		
    	ObjectMapper mapper = new ObjectMapper();

    	Token token = new Token();
    	token.setUsername(username);
    	token.setAccessToken(accessToken);
    	token.setRefreshToken(refreshToken);
    	token.setClientId(clientId);
    	token.setTicket(ticket);
    	token.setTimestamp(System.currentTimeMillis());

    	tokenCache.put(accessToken, token);    	

    	mapper.writeValue(new File(tokenStore, refreshToken), token);
    	
    	return token;
    }
    public Token refreshToken(String oldRefreshToken, String accessToken, String refreshToken, String clientId, String ticket) throws Exception {

    	ObjectMapper mapper = new ObjectMapper();
    	
    	File oldToken = new File(tokenStore, oldRefreshToken);
    	Token token = mapper.readValue(oldToken, Token.class);
    	
    	tokenCache.remove(token.getAccessToken());
    	oldToken.delete();
    	
    	token.setAccessToken(accessToken);
    	token.setRefreshToken(refreshToken);
    	token.setClientId(clientId);
    	token.setTicket(ticket);
    	token.setTimestamp(System.currentTimeMillis());

    	tokenCache.put(accessToken, token);    	
    	mapper.writeValue(new File(tokenStore, refreshToken), token);
		
		return token;
    }    
    
    public Token getToken(String accessToken) throws Exception {
    	Token token=tokenCache.get(accessToken);
    	if(token!=null) {
    		if(System.currentTimeMillis()>token.getTimestamp()+getExpiresIn()*1000) {
    			logger.info(accessToken+" token is too old, will remove it");
    			tokenCache.remove(accessToken);
    			return null;
    		}
    	}
    	return token;    	
    }
    public Token getRefreshToken(String refreshToken) throws OAuthProblemException {
    	ObjectMapper mapper = new ObjectMapper();
    	try {
			return mapper.readValue(new File(tokenStore,refreshToken), Token.class);
		} catch (java.io.IOException e) {
			throw OAuthProblemException.error("token_invalid","The refresh_token must be a valid id");
		}
    }
    public void validateClient(String clientId, String clientSecret) throws OAuthProblemException {
    	
    	if  (! trustedClients.containsKey(clientId)) {
    		
    		throw OAuthProblemException.error("trustless client_id");
    	}
    	
    	if (! trustedClients.get(clientId).equals(clientSecret)) {
    		
    		throw OAuthProblemException.error("invalid client_secret");
    	}
    }
    
    public static class Token implements Serializable {
    	
    	/**
		 * 
		 */
		private static final long serialVersionUID = -3853091262670597487L;
		private String accessToken;
    	private String refreshToken;
    	private String username;
    	private String clientId;
    	private String ticket;
    	private long timestamp;
		public String getAccessToken() {
			return accessToken;
		}		
		public long getTimestamp() {
			return timestamp;
		}
		public void setTimestamp(long timestamp) {
			this.timestamp = timestamp;
		}
		public void setAccessToken(String accessToken) {
			this.accessToken = accessToken;
		}
		public String getRefreshToken() {
			return refreshToken;
		}
		public void setRefreshToken(String refreshToken) {
			this.refreshToken = refreshToken;
		}
		public String getUsername() {
			return username;
		}
		public void setUsername(String username) {
			this.username = username;
		}
		public String getClientId() {
			return clientId;
		}
		public void setClientId(String clientId) {
			this.clientId = clientId;
		}
		public String getTicket() {
			return ticket;
		}
		public void setTicket(String ticket) {
			this.ticket = ticket;
		}   	
    }
}
