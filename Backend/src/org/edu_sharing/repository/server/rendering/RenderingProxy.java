package org.edu_sharing.repository.server.rendering;

import java.io.IOException;
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
import org.edu_sharing.repository.server.tools.URLTool;
import org.edu_sharing.repository.server.tools.security.Encryption;
import org.edu_sharing.repository.server.tools.security.SignatureVerifier;
import org.edu_sharing.repository.server.tools.security.Signing;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.rendering.RenderingService;
import org.edu_sharing.service.rendering.RenderingServiceData;
import org.edu_sharing.service.rendering.RenderingServiceFactory;
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
		//the repository the where content is stored
		String rep_id = req.getParameter("rep_id");
		String display = req.getParameter("display");
		String nodeId = req.getParameter("obj_id");
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

		if(rep_id == null || rep_id.trim().equals("")){
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST,"missing rep_id");
			return;
		}


		if (!validateSignature(req,resp)) return;

		String usernameDecrypted = getDecryptedUsername(req,resp);
		if(usernameDecrypted==null) return;

		updateUserRemoteRoles(req, usernameDecrypted);

		ApplicationInfo repoInfo = ApplicationInfoList.getRepositoryInfoById(rep_id);
		if("window".equals(display)) {
			openWindow(req, resp, nodeId, parentId, repoInfo, usernameDecrypted);
		}
		else{
			queryRendering(req,resp,nodeId,repoInfo,usernameDecrypted);
		}
	}

	/**
	 * true if the signature is valid, false if not (response error is automatically sent)
	 * @param req
	 * @param resp
	 * @return
	 * @throws IOException
	 */
	private boolean validateSignature(HttpServletRequest req,HttpServletResponse resp) throws IOException {
		String sig = req.getParameter("sig");
		String ts = req.getParameter("ts");
		//@todo refactor 5.1 check if this is required?!
		//String signed = req.getParameter("signed");
		String app_id = req.getParameter("app_id");
		String rep_id = req.getParameter("rep_id");
		String nodeId = req.getParameter("obj_id");
		//the proxy Repository
		String proxyRepId = req.getParameter("proxyRepId");

		//if(signed == null || signed.trim().equals("")){
			String signed = RenderingTool.getSignatureContent(rep_id, nodeId, ts);
		//}
		ApplicationInfo appInfoApplication = ApplicationInfoList.getRepositoryInfoById(app_id);
		if(appInfoApplication != null){

			SignatureVerifier.Result result = new SignatureVerifier().verify(app_id, sig, signed, ts);
			if(result.getStatuscode() != HttpServletResponse.SC_OK){
				resp.sendError(result.getStatuscode(),result.getMessage());
				return false;
			}
		}else{
			if(proxyRepId == null){
				resp.sendError(HttpServletResponse.SC_BAD_REQUEST,"missing proxyRepId");
				return false;
			}

			SignatureVerifier.Result result = new SignatureVerifier().verify(proxyRepId, sig, signed, ts);
			if(result.getStatuscode() != HttpServletResponse.SC_OK){
				resp.sendError(result.getStatuscode(), result.getMessage());
				return false;
			}
		}
		return true;
	}

	/**
	 * set the user ESREMOTEROLES property of the user if provided by the remote system
	 */
	private void updateUserRemoteRoles(HttpServletRequest req, String usernameDecrypted) {
		String[] roles = req.getParameterValues("role");
		if(roles != null && roles.length > 0) {
			final String username = usernameDecrypted;
			RunAsWork<Void> runAs = () -> {
				MCAlfrescoAPIClient apiClient = new MCAlfrescoAPIClient();
				String personId = new MCAlfrescoAPIClient().getUserInfo(username).get(CCConstants.SYS_PROP_NODE_UID);
				apiClient.setProperty(personId, CCConstants.PROP_USER_ESREMOTEROLES, new ArrayList<String>(Arrays.asList(roles)));
				return null;
			};
			AuthenticationUtil.runAsSystem(runAs);
		}
	}

	/**
	 * returns the encrypted username provided in the request, or fails and returns null
	 */
	private String getDecryptedUsername(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		String uEncrypted = req.getParameter("u");
		if(uEncrypted==null){
		    resp.sendError(HttpServletResponse.SC_BAD_REQUEST,"Parameter \"u\" (username) is missing");
		    return null;
        }
		Encryption encryptionTool = new Encryption("RSA");

		try {
			return encryptionTool.decrypt(Base64.decodeBase64(uEncrypted.getBytes()),
					encryptionTool.getPemPrivateKey(ApplicationInfoList.getHomeRepository().getPrivateKey()));
		}catch(Exception e) {
			logger.error(e.getMessage(), e);
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,"Parameter \"u\" (username) could not be decrypted: "+e.getMessage());
			return null;
		}
	}
	private void queryRendering(HttpServletRequest req, HttpServletResponse resp, String nodeId, ApplicationInfo repoInfo, String usernameDecrypted) throws IOException {
		String rep_id = req.getParameter("rep_id");
		ApplicationInfo homeRep = ApplicationInfoList.getHomeRepository();
		Encryption encryptionTool = new Encryption("RSA");
		String contentUrl;
		if(homeRep.getAppId().equals(rep_id)){
			contentUrl = homeRep.getContentUrl();
			if(contentUrl == null) {
				logger.warn("no content url configured");
				resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,"no content url configured");
			}
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

				AuthenticationUtil.RunAsWork<String> runAs = () -> {

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
				};

				try{
					String esuid = AuthenticationUtil.runAs(runAs, usernameDecrypted.trim());
					value = esuid + "@" + homeRep.getAppId();

					byte[] esuidEncrptedBytes = encryptionTool.encrypt(value.getBytes(), encryptionTool.getPemPublicKey(remoteRepo.getPublicKey()));
					value = Base64.encodeBase64String(esuidEncrptedBytes);

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
						value = Base64.encodeBase64String(userEncryptedBytes);

					}catch(Exception e) {
						logger.error(e.getMessage(), e);
					}


				}
			}
			value = URLEncoder.encode(value, "UTF-8");
			contentUrl = UrlTool.setParam(contentUrl, key, value);
		}

		//@todo 5.1 refactoring: check if "signed" is relevant -> renderer only uses "sig"
		/*
		long timestamp = System.currentTimeMillis();
		contentUrl = UrlTool.setParam(contentUrl, "ts",""+timestamp);

		try{
			contentUrl = UrlTool.setParam(contentUrl, "signed",RenderingTool.getSignatureSigned(repoInfo,nodeId,timestamp));

		}catch(GeneralSecurityException e){
			e.printStackTrace();
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}
		*/
		RenderingService service=RenderingServiceFactory.getRenderingService(homeRep.getAppId());
		// @todo 5.1 should version inline be transfered?
		try {
			RenderingServiceData renderData = service.getData(nodeId, null);
			resp.getWriter().print(service.getDetails(contentUrl,renderData));
		} catch (Exception e) {
			logger.error(e);
			resp.sendError(500,e.getMessage());
		}
		/*
		HttpQueryTool httpQuery = new HttpQueryTool();
		String result = httpQuery.query(contentUrl);
		resp.getWriter().println(result);
		*/
	}

	private boolean openWindow(HttpServletRequest req, HttpServletResponse resp, String nodeId, String parentId, ApplicationInfo repoInfo, String usernameDecrypted) throws IOException {
		String app_id = req.getParameter("app_id");
		String ts = req.getParameter("ts");
		String uEncrypted = req.getParameter("u");
		ApplicationInfo appInfoApplication = ApplicationInfoList.getRepositoryInfoById(app_id);

		if(uEncrypted == null) {
			resp.sendError(HttpServletResponse.SC_FORBIDDEN,"no user provided");
			return true;
		}

		String encTicket = req.getParameter("ticket");
		if(encTicket == null) {
			resp.sendError(HttpServletResponse.SC_FORBIDDEN,"no ticket provided");
			return true;
		}

		String ticket = null;
		Encryption enc = new Encryption("RSA");
		try {
			ticket = enc.decrypt(Base64.decodeBase64(encTicket.getBytes()), enc.getPemPrivateKey(ApplicationInfoList.getHomeRepository().getPrivateKey().trim()));
		}catch(GeneralSecurityException e) {
			logger.error(e.getMessage(), e);
			resp.sendError(HttpServletResponse.SC_FORBIDDEN,e.getMessage());
			return true;
		}


		/**
		 * when it's an lms the user who is in a course where the node is used (Angular path can not handle signatures)
		 *
		 * doing edu ticket auth
		 */
		if(appInfoApplication != null && ApplicationInfo.TYPE_LMS.equals(appInfoApplication.getType())) {
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
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST,"only LMS apps allowed for display=\"window\"");
			return true;
		}

		String version=getVersion(req);
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

	private String getVersion(HttpServletRequest req) {
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
		return version;

	}

}
