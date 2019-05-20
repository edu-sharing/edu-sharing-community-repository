package org.edu_sharing.repository.server.rendering;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.UrlTool;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.HttpQueryTool;
import org.edu_sharing.repository.server.tools.URLTool;
import org.edu_sharing.repository.server.tools.security.Encryption;
import org.edu_sharing.repository.server.tools.security.SignatureVerifier;
import org.edu_sharing.repository.server.tools.security.Signing;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.rendering.RenderingTool;
import org.edu_sharing.service.repoproxy.RepoProxyFactory;
import org.edu_sharing.service.usage.Usage;
import org.edu_sharing.service.usage.Usage2Service;
import org.edu_sharing.webservices.usage2.Usage2;
import org.edu_sharing.webservices.usage2.Usage2Result;
import org.edu_sharing.webservices.usage2.Usage2ServiceLocator;

public class RenderingProxy extends HttpServlet {


	private static final String[] ALLOWED_GET_PARAMS = new String[]{
			"closeOnBack","childobject","childobject_order"
	};
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

        logger.debug("app_id: " +app_id + " rep_id:" +rep_id+ " proxyRepId: "+proxyRepId+ " signed:" +signed +" display:" + display + " nodeId:" + nodeId);

        String childobjectId = req.getParameter("childobject_id");

		String parentId=nodeId;
		if(childobjectId!=null){
			boolean isChild=AuthenticationUtil.runAsSystem(()-> NodeServiceHelper.isChildOf(NodeServiceFactory.getLocalService(),childobjectId,parentId));
			if(!isChild){
				resp.sendError(HttpServletResponse.SC_BAD_REQUEST,"Node "+childobjectId+" is not a child of "+parentId);
				return;
			}
			nodeId = childobjectId;
		}

		boolean doRedirect = true;
		if("inline".equals(display)){
			doRedirect = false;
		}
		
		if(signed == null || signed.trim().equals("")){
			signed = rep_id + ts;
		}
		
		if(rep_id == null || rep_id.trim().equals("")){
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST,"missing rep_id");
			return;
		}
		
		ApplicationInfo repoInfo = ApplicationInfoList.getRepositoryInfoById(rep_id);
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
		
		
		String uEncrypted = req.getParameter("u");
		ApplicationInfo homeRep = ApplicationInfoList.getHomeRepository();

		String usernameDecrypted = null;
		Encryption encryptionTool = new Encryption("RSA");
		
		try {
			
			usernameDecrypted = encryptionTool.decrypt(java.util.Base64.getDecoder().decode(uEncrypted.getBytes()), encryptionTool.getPemPrivateKey(homeRep.getPrivateKey()));
		}catch(GeneralSecurityException e) {
			logger.error(e.getMessage(), e);
			resp.sendError(HttpServletResponse.SC_FORBIDDEN,e.getMessage());
		}

		String[] roles = req.getParameterValues("role");
		if(roles != null && roles.length > 0) {
			final String username = usernameDecrypted;
			RunAsWork<Void> runAs = new RunAsWork<Void>() {
				@Override
				public Void doWork() throws Exception {
					MCAlfrescoAPIClient apiClient = new MCAlfrescoAPIClient();
					String personId = new MCAlfrescoAPIClient().getUserInfo(username).get(CCConstants.SYS_PROP_NODE_UID);
					apiClient.setProperty(personId, CCConstants.PROP_USER_ESREMOTEROLES, new ArrayList<String>(Arrays.asList(roles)));
					return null;
				}
			};
			AuthenticationUtil.runAsSystem(runAs);
		}

		if("window".equals(display)) {

			openWindow(req, resp, nodeId, parentId, appInfoApplication,repoInfo, usernameDecrypted);
			return;
		}
		
		String contentUrl = null;
		
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
			
			if(repoInfo == null){
				resp.sendError(HttpServletResponse.SC_BAD_REQUEST,"unknown rep_id "+ rep_id);
				return;
			}
			
			contentUrl = repoInfo.getClientBaseUrl() +"/renderingproxy";
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
				final String usernameDecrypted2 = usernameDecrypted;
				
				ApplicationInfo remoteRepo = ApplicationInfoList.getRepositoryInfoById(rep_id);
				
				AuthenticationUtil.RunAsWork<String> runAs = new AuthenticationUtil.RunAsWork<String>(){
					
					//Logger logger = Logger.getLogger(this.getClass().getClass());
					@Override
					public String doWork() throws Exception {
						
						String localUsername = new String(usernameDecrypted2).trim();
						
						MCAlfrescoAPIClient apiClient = new MCAlfrescoAPIClient();
						
						
						
						HashMap<String,String> personData = apiClient.getUserInfo(localUsername);
						
						/**
						 *make sure that the remote user exists
						 */
						if(RepoProxyFactory.getRepoProxy().myTurn(rep_id)) {
							RepoProxyFactory.getRepoProxy().remoteAuth(remoteRepo,false);
						}
					
						
						return personData.get(CCConstants.PROP_USER_ESUID);
					}
				};
				
			    try{
			    	String esuid = AuthenticationUtil.runAs(runAs, usernameDecrypted.trim());
			    	value = esuid + "@" + homeRep.getAppId();
				    	
					byte[] esuidEncrptedBytes = encryptionTool.encrypt(value.getBytes(), encryptionTool.getPemPublicKey(remoteRepo.getPublicKey()));
					value = java.util.Base64.getEncoder().encodeToString(esuidEncrptedBytes);

			    }catch(Exception e){
				    	e.printStackTrace();
				    	resp.sendError(HttpServletResponse.SC_BAD_REQUEST,"remote user auth failed "+ rep_id);
				    	return;
			    }
			}else {
			
				//request.getParameter encodes the value, so we have to decode it again
				if(key.equals("u")){
					try {
						ApplicationInfo targetApplication = ApplicationInfoList.getRenderService();
						if(!homeRep.getAppId().equals(rep_id)){
							targetApplication = ApplicationInfoList.getRepositoryInfoById(rep_id);
						}
						byte[] userEncryptedBytes = encryptionTool.encrypt(usernameDecrypted.getBytes(), encryptionTool.getPemPublicKey(targetApplication.getPublicKey()));
						value = java.util.Base64.getEncoder().encodeToString(userEncryptedBytes);
	
					}catch(Exception e) {
						logger.error(e.getMessage(), e);
					}
					
					
				}
			}
			value = URLEncoder.encode(value, "UTF-8");
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
					
				String urlSig = URLEncoder.encode(java.util.Base64.getEncoder().encodeToString(signature));
				contentUrl = UrlTool.setParam(contentUrl, "sig",urlSig);
			}
		}catch(GeneralSecurityException e){
			e.printStackTrace();
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}
		
		
		logger.debug("contentUrl:" + contentUrl);
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

	private boolean openWindow(HttpServletRequest req, HttpServletResponse resp, String nodeId, String parentId, ApplicationInfo appInfoApplication, ApplicationInfo repoInfo, String usernameDecrypted) throws IOException {
		String ts = req.getParameter("ts");
		String uEncrypted = req.getParameter("u");

		if(uEncrypted == null) {
			resp.sendError(HttpServletResponse.SC_FORBIDDEN,"no user provided");
			return true;
		}

		String encTicket = req.getParameter("ticket");
		if(encTicket == null) {
			logger.error("no ticket provided");
			resp.sendError(HttpServletResponse.SC_FORBIDDEN,"no ticket provided");
			return true;
		}

		String ticket = null;
		Encryption enc = new Encryption("RSA");
		try {
			ticket = enc.decrypt(java.util.Base64.getDecoder().decode(encTicket.getBytes()), enc.getPemPrivateKey(ApplicationInfoList.getHomeRepository().getPrivateKey().trim()));
		}catch(GeneralSecurityException e) {
			logger.error(e.getMessage(), e);
			resp.sendError(HttpServletResponse.SC_FORBIDDEN,e.getMessage());
			return true;
		}


		/**
		 * when it's an lms/cms the user who is in a course where the node is used (Angular path can not handle signatures)
		 *
		 * doing edu ticket auth
		 */
		if(appInfoApplication != null &&
					   (ApplicationInfo.TYPE_LMS.equals(appInfoApplication.getType()) ||
						ApplicationInfo.TYPE_CMS.equals(appInfoApplication.getType()))) {
			req.getSession().removeAttribute(CCConstants.AUTH_SINGLE_USE_NODEID);
			HttpSession session = req.getSession(true);
			if(		Long.parseLong(ts) > (System.currentTimeMillis() - appInfoApplication.getMessageOffsetMs())
				||  Long.parseLong(ts) < (System.currentTimeMillis() + appInfoApplication.getMessageSendOffsetMs())
					) {
				try {
					Usage usage = null;
					if(repoInfo != null && !ApplicationInfoList.getHomeRepository().getAppId().equals(repoInfo.getAppId())){
						Usage2ServiceLocator locator = new Usage2ServiceLocator();
						locator.setusage2EndpointAddress(repoInfo.getWebServiceHotUrl());
						Usage2 u2 = locator.getusage2();
						Usage2Result u2r = u2.getUsage("ccrep://" + repoInfo.getAppId()+"/"+ nodeId, req.getParameter("app_id"), req.getParameter("course_id"), usernameDecrypted, req.getParameter("resource_id"));
						if(u2r != null) {
							usage = new Usage();
							usage.setAppUser(u2r.getAppUser());
							usage.setAppUserMail(u2r.getAppUserMail());
							usage.setCourseId(u2r.getCourseId());
							usage.setDistinctPersons(u2r.getDistinctPersons());
							usage.setFromUsed(u2r.getFromUsed());
							usage.setLmsId(u2r.getLmsId());
							usage.setNodeId(u2r.getNodeId());
							usage.setParentNodeId(u2r.getParentNodeId());
							usage.setResourceId(u2r.getResourceId());
							usage.setToUsed(u2r.getToUsed());
							usage.setUsageCounter(u2r.getUsageCounter());
							usage.setUsageVersion(u2r.getUsageVersion());
							usage.setUsageXmlParams(u2r.getUsageXmlParams());
						}
					}else {
						usage = new Usage2Service().getUsage(req.getParameter("app_id"), req.getParameter("course_id"), parentId, req.getParameter("resource_id"));
					}
					if(usage==null)
						throw new SecurityException("No usage found for course id "+req.getParameter("course_id")+" and resource id "+req.getParameter("resource_id"));
					req.getSession().setAttribute(CCConstants.AUTH_SINGLE_USE_NODEID, parentId);
					req.getSession().setAttribute(CCConstants.AUTH_SINGLE_USE_TIMESTAMP, ts);
				}
				catch (Throwable t){
					logger.warn("Usage fetching failed for node "+nodeId+": "+t.getMessage());
				}
			}
			else{
				String error="Error with timestamps between the local system and app "+appInfoApplication.getAppId();
				logger.error(error);
				resp.sendError(HttpServletResponse.SC_BAD_REQUEST,error);
			}

			//new AuthenticationToolAPI().authenticateUser(usernameDecrypted, session);
			AuthenticationToolAPI authTool = new AuthenticationToolAPI();
			if(authTool.validateTicket(ticket)) {
				authTool.storeAuthInfoInSession(usernameDecrypted, ticket,CCConstants.AUTH_TYPE_DEFAULT, session);
			}else {
				logger.warn("ticket:" + ticket +" is not valid");
				return true;
			}
		}else {
			logger.warn("only LMS / CMS apps allowed for display=\"window\"");
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST,"only LMS / CMS apps allowed for display=\"window\"");
			return true;
		}

		String version=req.getParameter("version");

		if(version==null) {
			logger.info("parameter version missing, will use latest (-1)");
		}
		try {
			if(Double.parseDouble(version)<1)
				version=null;
		}catch(Throwable t) {
			logger.warn("parameter version is non-numeric ("+version+"), will use latest (-1)");
			version=null;
		}

		String urlWindow = URLTool.getNgRenderNodeUrl(nodeId,version,false,(repoInfo != null) ? repoInfo.getAppId() : null);
		

		Map parameterMap = req.getParameterMap();
		for(Object o : parameterMap.entrySet()) {
			Map.Entry entry = (Map.Entry) o;
			String key = (String)entry.getKey();
			if(!Arrays.asList(ALLOWED_GET_PARAMS).contains(key)) {
				continue;
			}
			String value;
			if(entry.getValue() instanceof String[]){
				value = ((String[])entry.getValue())[0];
			}else{
				value = (String)entry.getValue();
			}
			urlWindow = UrlTool.setParam(urlWindow,key,value);
		}
		
		logger.debug("urlWindow:" + urlWindow);
		
		resp.sendRedirect(urlWindow);
		return false;
	}

}
