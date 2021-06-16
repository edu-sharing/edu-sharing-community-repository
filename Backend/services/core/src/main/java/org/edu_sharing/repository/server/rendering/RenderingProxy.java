package org.edu_sharing.repository.server.rendering;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;

import com.benfante.jslideshare.App;
import io.swagger.client.model.RenderingDetailsEntry;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.UrlTool;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.HttpException;
import org.edu_sharing.repository.server.tools.URLTool;
import org.edu_sharing.repository.server.tools.security.Encryption;
import org.edu_sharing.repository.server.tools.security.SignatureVerifier;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.permission.PermissionServiceFactory;
import org.edu_sharing.service.rendering.*;
import org.edu_sharing.service.repoproxy.RepoProxy;
import org.edu_sharing.service.repoproxy.RepoProxyFactory;
import org.edu_sharing.service.tracking.NodeTrackingDetails;
import org.edu_sharing.service.tracking.TrackingService;
import org.edu_sharing.service.tracking.TrackingServiceFactory;
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
				throw new RenderingException(HttpServletResponse.SC_BAD_REQUEST,"Node "+childobjectId+" is not a child of "+parentId,RenderingException.I18N.invalid_parameters);
			}
			nodeId = childobjectId;
		}

		if(rep_id == null || rep_id.trim().equals("")){
			throw new RenderingException(HttpServletResponse.SC_BAD_REQUEST,"missing rep_id",RenderingException.I18N.invalid_parameters);
		}

		// will throw if the signature is invalid
		validateSignature(req,resp);

		String usernameDecrypted;
		try {
			usernameDecrypted=getDecryptedUsername(req);
		}catch(Exception e){
			throw new RenderingException(HttpServletResponse.SC_BAD_REQUEST,e.getMessage(),RenderingException.I18N.encryption,e);
		}

		// remove any old states from current session before continuing
		req.getSession().removeAttribute(CCConstants.AUTH_SINGLE_USE_NODEID);

		// will throw if the usage is invalid
		Usage usage = validateUsage(req, nodeId, parentId, usernameDecrypted);

		try {
			updateUserRemoteRoles(req);

			ApplicationInfo repoInfo = ApplicationInfoList.getRepositoryInfoById(rep_id);
			if("window".equals(display)) {
				openWindow(req, resp, nodeId, parentId, repoInfo);
			}
			else{
				queryRendering(req,resp,nodeId,usage,repoInfo);
			}

		}
		catch(RenderingException e){
			throw e;
		}catch(Exception e){
			throw new RenderingException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,e.getMessage(),RenderingException.I18N.unknown,e);
		}

	}

	/**
	 * true if the signature is valid, false if not (response error is automatically sent)
	 * @param req
	 * @param resp
	 * @return
	 * @throws IOException
	 */
	private void validateSignature(HttpServletRequest req,HttpServletResponse resp) throws RenderingException {
		String sig = req.getParameter("sig");
		String ts = req.getParameter("ts");
		//some lms may tell an own signed string to validate?
		String signed = req.getParameter("signed");
		String app_id = req.getParameter("app_id");
		//the proxy Repository
		String proxyRepId = req.getParameter("proxyRepId");

		if(signed == null || signed.trim().equals("")){
			signed = app_id+ts;
		}
		ApplicationInfo appInfoApplication = ApplicationInfoList.getRepositoryInfoById(app_id);
		if(appInfoApplication != null){

			SignatureVerifier.Result result = new SignatureVerifier().verify(app_id, sig, signed, ts);
			if(result.getStatuscode() != HttpServletResponse.SC_OK){
				logger.warn("Signature failed for app " + app_id + ": " + result.getMessage() + " (" + result.getStatuscode()+")");
				throw new RenderingException(result.getStatuscode(),result.getMessage(),RenderingException.I18N.encryption);
			}
		}else{
			if(proxyRepId == null){
				throw new RenderingException(HttpServletResponse.SC_BAD_REQUEST,"missing proxyRepId",RenderingException.I18N.invalid_parameters);
			}

			SignatureVerifier.Result result = new SignatureVerifier().verify(proxyRepId, sig, signed, ts);
			if(result.getStatuscode() != HttpServletResponse.SC_OK){
				throw new RenderingException(result.getStatuscode(),result.getMessage(),RenderingException.I18N.encryption);
			}
		}
	}

	/**
	 * set the user ESREMOTEROLES property of the user if provided by the remote system
	 */
	private void updateUserRemoteRoles(HttpServletRequest req) throws Exception {
		String[] roles = req.getParameterValues("role");
		if(roles != null && roles.length > 0) {
			final String username = getDecryptedUsername(req);
			RunAsWork<Void> runAs = () -> {
				MCAlfrescoAPIClient apiClient = new MCAlfrescoAPIClient();
				String personId = new MCAlfrescoAPIClient().getUserInfo(username).get(CCConstants.SYS_PROP_NODE_UID);
				apiClient.setProperty(personId, CCConstants.PROP_USER_ESREMOTEROLES, new ArrayList<>(Arrays.asList(roles)));
				return null;
			};
			AuthenticationUtil.runAsSystem(runAs);
		}
	}

	/**
	 * returns the encrypted username provided in the request, or fails and returns null
	 */
	private String getDecryptedUsername(HttpServletRequest req) throws Exception {
		String uEncrypted = req.getParameter("u");
		if(uEncrypted==null){
			throw new Exception("Parameter \"u\" (username) is missing");
        }
		Encryption encryptionTool = new Encryption("RSA");

		try {
			return encryptionTool.decrypt(Base64.decodeBase64(uEncrypted.getBytes()),
					encryptionTool.getPemPrivateKey(ApplicationInfoList.getHomeRepository().getPrivateKey()));
		}catch(Exception e) {
			logger.error(e.getMessage(), e);
			throw new SecurityException("Parameter \"u\" (username) could not be decrypted: "+e.getMessage(),e);
		}
	}

	private String getContentUrl(ApplicationInfo homeRep, String rep_id, ApplicationInfo repoInfo)
			throws RenderingException {
		String contentUrl;
		if (homeRep.getAppId().equals(rep_id)) {
			contentUrl = homeRep.getContentUrl();
			if (contentUrl == null) {
				logger.warn("no content url configured");
				throw new RenderingException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "no content url configured",
						RenderingException.I18N.internal);
			}
		} else {
			if (repoInfo == null) {
				throw new RenderingException(HttpServletResponse.SC_BAD_REQUEST, "unknown rep_id " + rep_id,
						RenderingException.I18N.invalid_parameters);
			}
			contentUrl = repoInfo.getClientBaseUrl() + "/renderingproxy";
			contentUrl = UrlTool.setParam(contentUrl, "proxyRepId", ApplicationInfoList.getHomeRepository().getAppId());
		}
		return contentUrl;
	}

	private String handleUserParameter(ApplicationInfo homeRep, String rep_id, String usernameDecrypted, String value, String nodeId)
			throws RenderingException {
		Encryption encryptionTool = new Encryption("RSA");
		if (homeRep.getAppId().equals(rep_id)) {
			// request.getParameter encodes the value, so we have to decode it again
			try {
				ApplicationInfo targetApplication = ApplicationInfoList.getRenderService();
				if (!homeRep.getAppId().equals(rep_id)) {
					targetApplication = ApplicationInfoList.getRepositoryInfoById(rep_id);
				}
				byte[] userEncryptedBytes = encryptionTool.encrypt(usernameDecrypted.getBytes(),
						encryptionTool.getPemPublicKey(targetApplication.getPublicKey()));
				value = java.util.Base64.getEncoder().encodeToString(userEncryptedBytes);

			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		} else {
			final String usernameDecrypted2 = usernameDecrypted;
			ApplicationInfo remoteRepo = ApplicationInfoList.getRepositoryInfoById(rep_id);
			AuthenticationUtil.RunAsWork<String> runAs = () -> {
				String localUsername = new String(usernameDecrypted2).trim();
				MCAlfrescoAPIClient apiClient = new MCAlfrescoAPIClient();
				HashMap<String, String> personData = apiClient.getUserInfo(localUsername);

				/**
				 * make sure that the remote user exists
				 */
				if (RepoProxyFactory.getRepoProxy().myTurn(rep_id, nodeId) != null) {
					try {
                        RepoProxyFactory.getRepoProxy().remoteAuth(remoteRepo, localUsername,false);
					} catch (Throwable t) {
						logger.error("Remote user auth failed", t);
					}
				}
                String forcedUser = remoteRepo.getString(ApplicationInfo.FORCED_USER, null);
                if(forcedUser != null && !forcedUser.isEmpty()){
                    logger.info(ApplicationInfo.FORCED_USER + "is set, will use forced user "+forcedUser);
                    return forcedUser;
                }
				return personData.get(CCConstants.PROP_USER_ESUID);
			};

			try {
				String esuid = AuthenticationUtil.runAs(runAs, usernameDecrypted.trim());
				value = esuid + "@" + homeRep.getAppId();
				byte[] esuidEncrptedBytes = encryptionTool.encrypt(value.getBytes(),
						encryptionTool.getPemPublicKey(remoteRepo.getPublicKey()));
				value = java.util.Base64.getEncoder().encodeToString(esuidEncrptedBytes);
			} catch (Exception e) {
				throw new RenderingException(HttpServletResponse.SC_BAD_REQUEST, "remote user auth failed " + rep_id,
						RenderingException.I18N.invalid_parameters, e);
			}
		}
		return value;
	}

	private Iterable<Map.Entry<String, String>> getParameters(HttpServletRequest req) {
		Map parameterMap = req.getParameterMap();
		ArrayList<Map.Entry<String, String>> parameters = new ArrayList<Map.Entry<String, String>>();
		for (Object o : parameterMap.entrySet()) {
			Map.Entry entry = (Map.Entry) o;
			String key = (String) entry.getKey();
			String value = null;
			if (entry.getValue() instanceof String[]) {
				value = ((String[]) entry.getValue())[0];
			} else {
				value = (String) entry.getValue();
			}
			parameters.add(new AbstractMap.SimpleEntry<String, String>(key, value));
		}
		return parameters;
	}

	private String populateContentUrlParameters(String contentUrl,
			Iterable<Map.Entry<String, String>> requestParameters, ApplicationInfo homeRep, String rep_id,
			String usernameDecrypted, String nodeId) throws RenderingException, UnsupportedEncodingException {
		// put all Parameters to url but not sig signeddata and ts
		for (Map.Entry<String, String> parameter : requestParameters) {
			// leave out the following cause we add our own signature
			if (parameter.getKey().equals("sig") || parameter.getKey().equals("signed")
					|| parameter.getKey().equals("ts")) {
				continue;
			}
			String value = parameter.getValue();
			if (parameter.getKey().equals("u")) {
				value = handleUserParameter(homeRep, rep_id, usernameDecrypted, value, nodeId);
			}
			contentUrl = UrlTool.setParam(contentUrl, parameter.getKey(), URLEncoder.encode(value, "UTF-8"));
		}

		// @todo 5.1 refactoring: check if "signed" is relevant -> renderer only uses
		// "sig"
		long timestamp = System.currentTimeMillis();
		contentUrl = UrlTool.setParam(contentUrl, "ts", timestamp + "");
		try {
			contentUrl = UrlTool.setParam(contentUrl, "sig",
					RenderingTool.getSignatureSigned(rep_id, nodeId, timestamp));
		} catch (GeneralSecurityException e) {
			throw new RenderingException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
					"Error building signature " + rep_id + " " + nodeId, RenderingException.I18N.encryption, e);
		}
		return contentUrl;
	}

	private void render(ApplicationInfo repoInfo, HttpServletRequest req, HttpServletResponse resp,
			String nodeId, String usernameDecrypted, Usage usage,
			RenderingServiceOptions options) throws RenderingException {
		RenderingService service = RenderingServiceFactory.getRenderingService(repoInfo.getAppId());
		RepoProxy.RemoteRepoDetails remoteRepo = RepoProxyFactory.getRepoProxy().myTurn(repoInfo.getAppId(), nodeId);
		if(RepoProxyFactory.getRepoProxy().myTurn(repoInfo.getAppId(), nodeId) != null){
			try {
				Response remoteResult = RepoProxyFactory.getRepoProxy().getDetailsSnippetWithParameters(remoteRepo.getRepository(), remoteRepo.getNodeId(), getVersion(req), options.displayMode, null, req);
				RenderingDetailsEntry entity = (RenderingDetailsEntry) remoteResult.getEntity();
				resp.getOutputStream().write(entity.getDetailsSnippet().getBytes(StandardCharsets.UTF_8));
				return;
			} catch (Throwable throwable) {
				logger.error("Remote repo rendering failed: " + throwable.getMessage());
				throw new RuntimeException(throwable);
			}
		}

		// @todo 5.1 should version inline be transfered?
		try {
			String contentUrl = getContentUrl(repoInfo, repoInfo.getAppId(), repoInfo);
			String finalContentUrl = populateContentUrlParameters(contentUrl, getParameters(req), repoInfo, repoInfo.getAppId(),
					usernameDecrypted, nodeId);

			RenderingServiceData renderData = service.getData(repoInfo, nodeId, null, usernameDecrypted, options);
			resp.getOutputStream().write(service.getDetails(finalContentUrl, renderData).getBytes(StandardCharsets.UTF_8));
			// track inline / lms
			if (options.displayMode.equals(RenderingTool.DISPLAY_INLINE)) {
				NodeTrackingDetails details = new NodeTrackingDetails(getVersion(req));
				details.setLms(new NodeTrackingDetails.NodeTrackingLms(usage));
				TrackingServiceFactory.getTrackingService().trackActivityOnNode(
						new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId), details,
						TrackingService.EventType.VIEW_MATERIAL_EMBEDDED);
			}
		} catch (HttpException e) {
			throw new RenderingException(e);
		} catch (Throwable t) {
			throw new RenderingException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, t.getMessage(),
					RenderingException.I18N.unknown, t);
		}
	}

	private void runAsSystem(ThrowingProcedure<RenderingException> f) throws RenderingException {
		RenderingException error = AuthenticationUtil.runAsSystem(() -> {
			try {
				f.run();
			} catch (RenderingException e) {
				return e;
			}
			return null;
		});
		if (error != null) {
			throw error;
		}
	}

	private void queryRendering(HttpServletRequest req, HttpServletResponse resp, String nodeId, Usage usage,
			ApplicationInfo repoInfo) throws Exception {
		String usernameDecrypted = getDecryptedUsername(req);
		// it is a trusted app who requested and signature was verified, so we can
		// render the node
		RenderingServiceOptions options = RenderingServiceOptions.fromRequestParameters(req);
		runAsSystem(() -> {
			render(repoInfo, req, resp, nodeId, usernameDecrypted, usage, options);
		});
	}

	private void openWindow(HttpServletRequest req, HttpServletResponse resp, String nodeId, String parentId, ApplicationInfo repoInfo) throws Exception {
		String app_id = req.getParameter("app_id");
		String ts = req.getParameter("ts");
		String uEncrypted = req.getParameter("u");
		ApplicationInfo appInfoApplication = ApplicationInfoList.getRepositoryInfoById(app_id);

		if(uEncrypted == null) {
			throw new RenderingException(HttpServletResponse.SC_FORBIDDEN,"no user provided",RenderingException.I18N.invalid_parameters);
		}

		String encTicket = req.getParameter("ticket");
		if(encTicket == null) {
			logger.error("no ticket provided");
			throw new RenderingException(HttpServletResponse.SC_FORBIDDEN,"no ticket provided",RenderingException.I18N.invalid_parameters);
		}

		String ticket = null;
		Encryption enc = new Encryption("RSA");
		try {
			ticket = enc.decrypt(java.util.Base64.getDecoder().decode(encTicket.getBytes()), enc.getPemPrivateKey(ApplicationInfoList.getHomeRepository().getPrivateKey().trim()));
		}catch(GeneralSecurityException e) {
			throw new RenderingException(HttpServletResponse.SC_BAD_REQUEST,e.getMessage(),RenderingException.I18N.encryption,e);
		}


		/**
		 * when it's an lms/cms the user who is in a course where the node is used (Angular path can not handle signatures)
		 *
		 * doing edu ticket auth
		 */
		if(appInfoApplication != null &&
				(ApplicationInfo.TYPE_LMS.equals(appInfoApplication.getType()) ||
				 ApplicationInfo.TYPE_CMS.equals(appInfoApplication.getType()))) {
			req.getSession().setAttribute(CCConstants.AUTH_SINGLE_USE_NODEID, parentId);
			req.getSession().setAttribute(CCConstants.AUTH_SINGLE_USE_TIMESTAMP, ts);

			//new AuthenticationToolAPI().authenticateUser(usernameDecrypted, session);
			AuthenticationToolAPI authTool = new AuthenticationToolAPI();
			if(authTool.validateTicket(ticket)) {
				authTool.storeAuthInfoInSession(getDecryptedUsername(req), ticket,CCConstants.AUTH_TYPE_DEFAULT, req.getSession());
			}else {
				logger.warn("ticket:" + ticket +" is not valid");
				return;
			}
		}else {
			logger.warn("only LMS / CMS apps allowed for display=\"window\"");
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST,"only LMS / CMS apps allowed for display=\"window\"");
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
	}

	private Usage validateUsage(HttpServletRequest req, String nodeId, String parentId, String usernameDecrypted) throws RenderingException {
		String ts=req.getParameter("ts");
		ApplicationInfo appInfoApplication = ApplicationInfoList.getRepositoryInfoById(req.getParameter("app_id"));
		ApplicationInfo repoInfo = ApplicationInfoList.getRepositoryInfoById(req.getParameter("rep_id"));

		if(		Long.parseLong(ts) > (System.currentTimeMillis() - appInfoApplication.getMessageOffsetMs())
			||  Long.parseLong(ts) < (System.currentTimeMillis() + appInfoApplication.getMessageSendOffsetMs())
				) {
			try {
				Usage usage = null;
				if(repoInfo != null && !ApplicationInfoList.getHomeRepository().getAppId().equals(repoInfo.getAppId())){
					/*
					Usage2ServiceLocator locator = new Usage2ServiceLocator();
					locator.setusage2EndpointAddress(repoInfo.getWebServiceHotUrl());
					Usage2 u2 = locator.getusage2();
					Usage2Result u2r = u2.getUsage("ccrep://" + repoInfo.getAppId()+"/"+ nodeId, req.getParameter("app_id"), req.getParameter("course_id"), getDecryptedUsername(req), req.getParameter("resource_id"));
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
					 */
					// TODO: remote repos usages currently not available, permission will be checked
					boolean hasPermission = false;
					try {
						hasPermission = AuthenticationUtil.runAs(() -> {
							try {
								RepoProxyFactory.getRepoProxy().getMetadata(repoInfo.getAppId(), req.getParameter("obj_id"), new ArrayList<>(), null);
								// when no error occurs, the permission is valid
								return true;
							} catch (Throwable t) {
								logger.info(t);
							}
							return false;
						}, usernameDecrypted);

					}catch(Throwable t){
						logger.info("Could not fetch permissions: " + t.getMessage(), t);
					}
					if(!hasPermission) {
						throw new RenderingException(HttpServletResponse.SC_UNAUTHORIZED, "Remote repository does not allow access to  " + nodeId, RenderingException.I18N.usage_missing);
					}
					return null;
				}else {
					usage = new Usage2Service().getUsage(req.getParameter("app_id"), req.getParameter("course_id"), parentId, req.getParameter("resource_id"));
				}
				if(usage==null) {
					boolean ccpublish=AuthenticationUtil.runAs(()->PermissionServiceFactory.getLocalService().hasPermission(StoreRef.PROTOCOL_WORKSPACE,StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(),nodeId,CCConstants.PERMISSION_CC_PUBLISH),getDecryptedUsername(req));
					if(ccpublish){
						throw new RenderingException(HttpServletResponse.SC_UNAUTHORIZED,"Usage fetching failed for node "+nodeId,RenderingException.I18N.usage_missing_permissions);
					}
					throw new RenderingException(HttpServletResponse.SC_UNAUTHORIZED,"Usage fetching failed for node "+nodeId,RenderingException.I18N.usage_missing);
				}
				return usage;
			}
			catch(RenderingException e){
				throw e;
			}
			catch (Throwable t){
				if(t.getCause()!=null && t.getCause() instanceof InvalidNodeRefException){
					throw new RenderingException(HttpServletResponse.SC_NOT_FOUND, t.getCause().getMessage(), RenderingException.I18N.node_missing, t);
				}
				else {
					throw new RenderingException(HttpServletResponse.SC_UNAUTHORIZED, "Usage fetching failed for node " + nodeId + ": " + t.getMessage(), RenderingException.I18N.usage_missing, t);
				}

			}
		}
		else{
			throw new RenderingException(HttpServletResponse.SC_UNAUTHORIZED,"Error with timestamps between the local system and app "+appInfoApplication.getAppId(),RenderingException.I18N.encryption);
		}
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

	@FunctionalInterface
	private interface ThrowingProcedure<E extends Exception> {
		void run() throws E;
	}
}
