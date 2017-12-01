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
import org.edu_sharing.repository.server.tools.security.Signing;

public class RenderingTool {
	
	public static String DISPLAY_DYNAMIC = "dynamic";
	public static String DISPLAY_INLINE = "inline";
	
	public static String COM_INTERNAL = "internal";
	
	Logger logger = Logger.getLogger(RenderingTool.class);
	
	public String getRenderServiceUrl(ApplicationInfo repInfo, String nodeId, String username,String version,Map<String,String> parameters) throws GeneralSecurityException{
		return getRenderServiceUrl(repInfo,nodeId,username,version,parameters,false);
	}
	
	/**
	 * this only works for alfresco repositories
	 * 
	 * @param repInfo
	 * @param nodeId
	 * @param usernameEncrypted
	 * @return
	 * @throws GeneralSecurityException
	 */
	public String getRenderServiceUrl(ApplicationInfo repInfo, String nodeId, String username,String version,Map<String,String> parameters, boolean backendCall) throws GeneralSecurityException{
		
		
		String usernameEncrypted = MCAlfrescoBaseClient.getBlowFishEncrypted(username, ApplicationInfoList.getHomeRepository());
		
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
			logger.error("no pkey available");
			throw new GeneralSecurityException("no pkey available");
		}		
		
		renderingProxy = UrlTool.setParam(renderingProxy, "obj_id", nodeId);
		renderingProxy = UrlTool.setParam(renderingProxy, "rep_id",repInfo.getAppId());
		renderingProxy = UrlTool.setParam(renderingProxy, "u",usernameEncrypted);
		if(version!=null)
			renderingProxy = UrlTool.setParam(renderingProxy, "version",version);
		if(parameters!=null){
			for(Entry<String, String> param : parameters.entrySet()){
				renderingProxy = UrlTool.setParam(renderingProxy, param.getKey(),param.getValue());
			}
		}
		renderingProxy = UrlTool.setParam(renderingProxy, "ts",""+timestamp);
		try{
			renderingProxy = UrlTool.setParam(renderingProxy, "language",new AuthenticationToolAPI().getCurrentLanguage());
		}catch(Throwable t){}
		
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
	
	/**
	 * Just an override with few parameters!
	 */
	public String getRenderServiceUrl(ApplicationInfo repInfo, String nodeId, String username) throws GeneralSecurityException{
		return getRenderServiceUrl(repInfo, nodeId, username,null,null);
	}

	public String getRenderServiceUrl(ApplicationInfo repInfo, String nodeId, String username,String version,Map<String,String> parameters,String displayType) throws GeneralSecurityException {
		
		boolean backendCall = (displayType != null && displayType.equals(RenderingTool.DISPLAY_DYNAMIC)) ? true : false;
		String baseUrl = getRenderServiceUrl(repInfo, nodeId, username,version,parameters,backendCall);
		return UrlTool.setParam(baseUrl,"display",displayType);
	}
	
	public String getRenderServiceUrl(ApplicationInfo repInfo, String nodeId, String username,String version,boolean displayMetadata, boolean backendCall) throws GeneralSecurityException{
		
		
		String usernameEncrypted = MCAlfrescoBaseClient.getBlowFishEncrypted(username, ApplicationInfoList.getHomeRepository());
		
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
			logger.error("no pkey available");
			throw new GeneralSecurityException("no pkey available");
		}
		
		renderingProxy = UrlTool.setParam(renderingProxy, "obj_id", nodeId);
		renderingProxy = UrlTool.setParam(renderingProxy, "rep_id",repInfo.getAppId());
		renderingProxy = UrlTool.setParam(renderingProxy, "u",usernameEncrypted);
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
	
}
