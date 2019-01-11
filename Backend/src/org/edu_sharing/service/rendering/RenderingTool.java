package org.edu_sharing.service.rendering;

import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.UrlTool;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.repository.server.MCAlfrescoBaseClient;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.URLTool;
import org.edu_sharing.repository.server.tools.security.Encryption;
import org.edu_sharing.repository.server.tools.security.Signing;

public class RenderingTool {
	
	public static String DISPLAY_DYNAMIC = "dynamic";
	public static String DISPLAY_INLINE = "inline";
	
	public static String COM_INTERNAL = "internal";
	
	Logger logger = Logger.getLogger(RenderingTool.class);

	/**
	 * this only works for alfresco repositories
	 * 
	 * @param repInfo
	 * @return
	 * @throws GeneralSecurityException
	 */
	public String getRenderServiceUrl(ApplicationInfo repInfo,String nodeId,Map<String,String> parameters) throws GeneralSecurityException{

		ApplicationInfo homeRepo = ApplicationInfoList.getHomeRepository();
		
		String renderingService = homeRepo.getContentUrl();

		//renderServiceUrl = UrlTool.setParam(renderServiceUrl, "proxyRepId", ApplicationInfoList.getHomeRepository().getAppId());
		
		long timestamp = System.currentTimeMillis();
		Signing sig = new Signing();
		
		String privateKey = repInfo.getPrivateKey();
		
		//take the homeRepository keys for signature	
		privateKey = homeRepo.getPrivateKey();
	
		if(privateKey == null){
			logger.error("no privateKey available");
			throw new GeneralSecurityException("no privateKey available");
		}
		if(parameters!=null){
			for(Entry<String, String> param : parameters.entrySet()){
				renderingService = UrlTool.setParam(renderingService, param.getKey(),param.getValue());
			}
		}
		renderingService = UrlTool.setParam(renderingService, "ts",""+timestamp);
		try{
			renderingService = UrlTool.setParam(renderingService, "language",new AuthenticationToolAPI().getCurrentLanguage());
		}catch(Throwable t){}
		
		String data = repInfo.getAppId()+nodeId+timestamp;
		byte[] signature = sig.sign(sig.getPemPrivateKey(privateKey, CCConstants.SECURITY_KEY_ALGORITHM), data, CCConstants.SECURITY_SIGN_ALGORITHM);
		String urlSig = URLEncoder.encode(new Base64().encodeToString(signature));
		renderingService = UrlTool.setParam(renderingService, "sig",urlSig);
		return renderingService;
		
	}
	
	/**
	 * Just an override with few parameters!
	 */
	public String getRenderServiceUrl(ApplicationInfo repInfo,String nodeId) throws GeneralSecurityException{
		return getRenderServiceUrl(repInfo,nodeId,null,null);
	}

	public String getRenderServiceUrl(ApplicationInfo repInfo,String nodeId,Map<String,String> parameters,String displayType) throws GeneralSecurityException {
		
		String baseUrl = getRenderServiceUrl(repInfo,nodeId,parameters);
		return UrlTool.setParam(baseUrl,"display",displayType);
	}
	
	public String getRenderServiceUrl(ApplicationInfo repInfo, String nodeId,String version,boolean displayMetadata, boolean backendCall) throws GeneralSecurityException{

		ApplicationInfo homeRepo = ApplicationInfoList.getHomeRepository();
		
		String renderingProxy = (backendCall) ? homeRepo.getWebServerUrl() + "/" + homeRepo.getWebappname() +"/renderingproxy" 
											  : homeRepo.getClientBaseUrl() +"/renderingproxy";
		//renderServiceUrl = UrlTool.setParam(renderServiceUrl, "proxyRepId", ApplicationInfoList.getHomeRepository().getAppId());
		
		long timestamp = System.currentTimeMillis();
		Signing sig = new Signing();
		
		String privateKey = repInfo.getPrivateKey();
		
		//take the homeRepository keys for signature	
		privateKey = homeRepo.getPrivateKey();
			
	
		if(privateKey == null){
			logger.error("no privateKey available");
			throw new GeneralSecurityException("no privateKey available");
		}
		
		renderingProxy = UrlTool.setParam(renderingProxy, "obj_id", nodeId);
		renderingProxy = UrlTool.setParam(renderingProxy, "rep_id",repInfo.getAppId());
		if(version!=null)
			renderingProxy = UrlTool.setParam(renderingProxy, "version",version);
		renderingProxy = UrlTool.setParam(renderingProxy, "metadata",""+displayMetadata);
		renderingProxy = UrlTool.setParam(renderingProxy, "ts",""+timestamp);
		
		String data = repInfo.getAppId()+timestamp;
		byte[] signature = sig.sign(sig.getPemPrivateKey(privateKey, CCConstants.SECURITY_KEY_ALGORITHM), data, CCConstants.SECURITY_SIGN_ALGORITHM);
		String urlSig = URLEncoder.encode(new Base64().encodeToString(signature));
		renderingProxy = UrlTool.setParam(renderingProxy, "sig",urlSig);
		
		if(repInfo.ishomeNode()){
			renderingProxy = UrlTool.setParam(renderingProxy, "app_id",repInfo.getAppId());
		}else{
			renderingProxy = UrlTool.setParam(renderingProxy, "proxyRepId",homeRepo.getAppId());
		}
		
		renderingProxy = URLTool.addOAuthAccessToken(renderingProxy);
			
		return renderingProxy;
		
	}
	
	private String getUsernameEncrypted(String username) {
		ApplicationInfo appInfoRender = ApplicationInfoList.getHomeRepository();
		String usernameEncrypted = null;
		try {
			Encryption encryptionTool = new Encryption("RSA");
			byte[] userEncryptedBytes = encryptionTool.encrypt(username.getBytes(), encryptionTool.getPemPublicKey(appInfoRender.getPublicKey()));
			usernameEncrypted = Base64.encodeBase64String(userEncryptedBytes);
			usernameEncrypted = URLEncoder.encode(usernameEncrypted, "UTF-8");
			return usernameEncrypted;
		}catch(Exception e) {
			logger.error(e.getMessage(), e);
			return null;
		}
	}
	
}
