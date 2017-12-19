package org.edu_sharing.repository.server.rendering;

import java.io.IOException;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.apache.axis.AxisFault;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.apache.myfaces.application.ApplicationImpl;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.UrlTool;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.MCAlfrescoBaseClient;
import org.edu_sharing.repository.server.MCBaseClient;
import org.edu_sharing.repository.server.RepoFactory;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.AuthenticatorRemoteRepository;
import org.edu_sharing.repository.server.tools.HttpQueryTool;
import org.edu_sharing.repository.server.tools.URLTool;
import org.edu_sharing.repository.server.tools.security.Encryption;
import org.edu_sharing.repository.server.tools.security.SignatureVerifier;
import org.edu_sharing.repository.server.tools.security.Signing;
import org.edu_sharing.service.rendering.RenderingTool;

public class RenderingProxy extends HttpServlet {

	
	Logger logger = Logger.getLogger(RenderingProxy.class);
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		
		//the app that requested the content
		String app_id = req.getParameter("app_id");
		
		//the repository the where content is stored
		String rep_id = req.getParameter("rep_id");
		
		//the proxy Repository
		String proxyRepId = req.getParameter("proxyRepId");
		String sig = req.getParameter("sig");
		String ts = req.getParameter("ts");
		String signed = req.getParameter("signed");
		
		String display = req.getParameter("display");
		
		String nodeId = req.getParameter("obj_id");
		
		boolean doRedirect = true;
		if("inline".equals(display)){
			doRedirect = false;
		}
		
		
		//evtFehler schmeißen misssing signed
		if(signed == null || signed.trim().equals("")){
			signed = rep_id + ts;
		}
		
		
		
		
		if(rep_id == null || rep_id.trim().equals("")){
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST,"missing rep_id");
			return;
		}
		
		//Signatur validation
		//current repo knows the app
		ApplicationInfo appInfoApplication = ApplicationInfoList.getRepositoryInfoById(app_id);
		if(appInfoApplication != null){
			
			SignatureVerifier.Result result = new SignatureVerifier().verify(app_id, sig, signed, ts);
			if(result.getStatuscode() != HttpServletResponse.SC_OK){
				resp.sendError(result.getStatuscode(),result.getMessage());
				return;
			}
		}else{
			if(proxyRepId == null){
				resp.sendError(HttpServletResponse.SC_BAD_REQUEST,"missing proxyRepId");
				return;
			}
			
			SignatureVerifier.Result result = new SignatureVerifier().verify(proxyRepId, sig, signed, ts);
			if(result.getStatuscode() != HttpServletResponse.SC_OK){
				resp.sendError(result.getStatuscode(), result.getMessage());
				return;
			}
		}
			
		if("window".equals(display)) {
			String uEncrypted = req.getParameter("u");
			if(uEncrypted == null) {
				resp.sendError(HttpServletResponse.SC_FORBIDDEN,"no user provided");
				return;
			}
			
			String encTicket = req.getParameter("ticket");
			if(encTicket == null) {
				resp.sendError(HttpServletResponse.SC_FORBIDDEN,"no ticket provided");
				return;
			}
			
			String ticket = null;
			Encryption enc = new Encryption("RSA");
			try {
				ticket = enc.decrypt(Base64.decodeBase64(encTicket.getBytes()), enc.getPemPrivateKey(ApplicationInfoList.getHomeRepository().getPrivateKey().trim()));
			}catch(GeneralSecurityException e) {
				logger.error(e.getMessage(), e);
				resp.sendError(HttpServletResponse.SC_FORBIDDEN,e.getMessage());
				return;
			}
			
			ApplicationInfo homeRepo = ApplicationInfoList.getHomeRepository();
			
			// TODO change to other encryption in 4.1
			String usernameDecrypted = MCAlfrescoBaseClient.getBlowFishDecrypted(uEncrypted, homeRepo);
			usernameDecrypted=usernameDecrypted.trim();
			
			/**
			 * when it's an lms the user who is in a course where the node is used (Angular path can not handle signatures) 
			 * 
			 * doing edu ticket auth
			 */
			if(appInfoApplication != null && ApplicationInfo.TYPE_LMS.equals(appInfoApplication.getType())) {
				HttpSession session = req.getSession(true);
				req.getSession().setAttribute(CCConstants.AUTH_SINGLE_USE_NODEID, nodeId);
				req.getSession().setAttribute(CCConstants.AUTH_SINGLE_USE_TIMESTAMP, ts);
				
				//new AuthenticationToolAPI().authenticateUser(usernameDecrypted, session);
				AuthenticationToolAPI authTool = new AuthenticationToolAPI();
				if(authTool.validateTicket(ticket)) {
					authTool.storeAuthInfoInSession(usernameDecrypted, ticket,CCConstants.AUTH_TYPE_DEFAULT, session);
				}else {
					logger.warn("ticket:" + ticket +" is not valid");
					return;
				}
			}else {
				resp.sendError(HttpServletResponse.SC_BAD_REQUEST,"only LMS apps allowed for display=\"window\"");
			}
			
			
			String version="/"+req.getParameter("version");
			if(Double.parseDouble(req.getParameter("version"))<1)
				version="";
			String closeOnBack="";
			if(req.getParameter("closeOnBack")!=null){
				closeOnBack="?closeOnBack="+req.getParameter("closeOnBack");
			}
			String urlWindow = URLTool.getNgRenderNodeUrl(nodeId)+version+closeOnBack;
			resp.sendRedirect(urlWindow);
			return;
		}
		
		String contentUrl = null;
		
		final ApplicationInfo homeRep = ApplicationInfoList.getHomeRepository();
		if(homeRep.getAppId().equals(rep_id)){
			contentUrl = homeRep.getContentUrl();
			
			/**
			 * use internal url to renderingservice when rendering snippet was requested
			 */
			if(RenderingTool.DISPLAY_DYNAMIC.equals(display) && homeRep.getContentUrlBackend() != null){
				contentUrl = homeRep.getContentUrlBackend();
				contentUrl = UrlTool.setParam(contentUrl, "com",RenderingTool.COM_INTERNAL);
			}
			
			String com = req.getParameter("com");
			if(com != null && com.equals(RenderingTool.COM_INTERNAL) && homeRep.getContentUrlBackend() != null) {
				contentUrl = homeRep.getContentUrlBackend();
				contentUrl = UrlTool.setParam(contentUrl, "com",RenderingTool.COM_INTERNAL);
			}
			
			
			if(contentUrl == null) logger.warn("no content url configured");
		}else{
			ApplicationInfo appInfo = ApplicationInfoList.getRepositoryInfoById(rep_id);
			if(appInfo == null){
				resp.sendError(HttpServletResponse.SC_BAD_REQUEST,"unknown rep_id "+ rep_id);
				return;
			}
			
			contentUrl = appInfo.getClientBaseUrl() +"/renderingproxy";
			contentUrl = UrlTool.setParam(contentUrl, "proxyRepId", ApplicationInfoList.getHomeRepository().getAppId());
		}
		
		//put all Parameters to url but not sig signeddata and ts 
		Map parameterMap = req.getParameterMap();
		for(Object o : parameterMap.entrySet()){
			Map.Entry entry = (Map.Entry)o;
			String key = (String)entry.getKey();
			String value = null;
			if(entry.getValue() instanceof String[]){
				
				value = ((String[])entry.getValue())[0];
				
			}else{
				value = (String)entry.getValue();
			}
			
			
			//leave out the following cause we add our own signature
			if(key.equals("sig") || key.equals("signed") || key.equals("ts")){
				continue;
			}
			
			if(key.equals("u") && !homeRep.getAppId().equals(rep_id)){
				final String usernameDecrypted = MCAlfrescoBaseClient.getBlowFishDecrypted(value, homeRep);
				
				final String finalRepId = rep_id;
				
				AuthenticationUtil.RunAsWork<String> runAs = new AuthenticationUtil.RunAsWork<String>(){
					
					//Logger logger = Logger.getLogger(this.getClass().getClass());
					@Override
					public String doWork() throws Exception {
						
						String localUsername = new String(usernameDecrypted).trim();
						
						MCAlfrescoAPIClient apiClient = new MCAlfrescoAPIClient();
						
						
						
						HashMap<String,String> personData = apiClient.getUserInfo(localUsername);
						
						/**
						 *make sure that the remote user exists
						 */
						if(personData == null || personData.size() < 1){
							throw new Exception("unknown local user "+localUsername);
						}else{
							
							/**
							 * only do the create process if remote user does not exist
							 */
							MCAlfrescoBaseClient remoteClient = null;
							boolean remoteUserExists = false;
							try{
								MCBaseClient remoteClientBase = RepoFactory.getInstance(finalRepId, apiClient.getAuthenticationInfo());
								if(remoteClientBase instanceof MCAlfrescoBaseClient) {
									remoteClient = (MCAlfrescoBaseClient)remoteClientBase;
									HashMap<String,String> remoteUserInfo = remoteClient.getUserInfo(personData.get(CCConstants.PROP_USER_ESUID) + "@" + homeRep.getAppId());
									if(remoteUserInfo != null && remoteUserInfo.size() > 0){
										remoteUserExists = true;
									}
								}
							}catch(AxisFault e){
								
								boolean userDoesNotExsist = false;
								for (org.w3c.dom.Element ele : e.getFaultDetails()) {
									
									if (ele.getNodeName().equals("faultData")) {
										if(ele.getTextContent().contains("does not exist")){
											userDoesNotExsist = true;
											logger.error( ele.getTextContent());
										}										
									}
								}
								if(!userDoesNotExsist){
									logger.error(e.getMessage(),e);
									throw new Exception(e);
								}
							}
							catch(Throwable e){
								logger.error(e.getMessage(),e);
								throw new Exception(e);
							}
							
							HashMap<String,String> localAuthInfo = apiClient.getAuthenticationInfo();							
							if(!remoteUserExists){
								try{
									new AuthenticatorRemoteRepository().getAuthInfoForApp(localAuthInfo, ApplicationInfoList.getRepositoryInfoById(finalRepId));
								}catch(Throwable e){
									throw new Exception(e);
								}
							}
						}
						
						
						return personData.get(CCConstants.PROP_USER_ESUID);
					}
				};
				
			    try{
			    	String esuid = AuthenticationUtil.runAs(runAs, usernameDecrypted.trim());
			    	value = esuid + "@" + homeRep.getAppId();
			    	value = MCAlfrescoBaseClient.getBlowFishEncrypted(value, homeRep);
			    	
			    }catch(Exception e){
				    	e.printStackTrace();
				    	resp.sendError(HttpServletResponse.SC_BAD_REQUEST,"remote user auth failed "+ rep_id);
				    	return;
			    }
			}
			
			//request.getParameter encodes the value, so we have to decode it again
			if(key.equals("u")){
				value = URLEncoder.encode(value, "UTF-8");
			}
			
			contentUrl = UrlTool.setParam(contentUrl, key, value);
		}
		
			
		long timestamp = System.currentTimeMillis();
		contentUrl = UrlTool.setParam(contentUrl, "ts",""+timestamp);
		
		Signing sigTool = new Signing();
		
		String data = rep_id + nodeId + timestamp;
		contentUrl = UrlTool.setParam(contentUrl, "signed",""+data);
		
		String privateKey = homeRep.getPrivateKey();
		
		try{
			if(privateKey != null){
				byte[] signature = sigTool.sign(sigTool.getPemPrivateKey(privateKey, CCConstants.SECURITY_KEY_ALGORITHM), data, CCConstants.SECURITY_SIGN_ALGORITHM);
					
				String urlSig = URLEncoder.encode(new Base64().encodeToString(signature));
				contentUrl = UrlTool.setParam(contentUrl, "sig",urlSig);
			}
		}catch(GeneralSecurityException e){
			e.printStackTrace();
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}
		
		if(doRedirect){
			if(contentUrl == null){
				resp.sendError(500, "no contenturl configured");
				return;
			}else{
				resp.sendRedirect(contentUrl);
				return;
			}
		}else{
			HttpQueryTool httpQuery = new HttpQueryTool();
			String result = httpQuery.query(contentUrl);
			resp.getWriter().println(result);
		}
		
	}
	
}
