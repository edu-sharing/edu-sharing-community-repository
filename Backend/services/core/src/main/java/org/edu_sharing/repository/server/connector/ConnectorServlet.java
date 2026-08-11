package org.edu_sharing.repository.server.connector;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.action.RessourceInfoExecuter;
import org.edu_sharing.alfresco.service.connector.*;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.UrlTool;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.repository.server.MCAlfrescoBaseClient;
import org.edu_sharing.repository.server.SimpleErrorWithDetailsException;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.http.HttpQueryTool;
import org.edu_sharing.repository.server.tools.security.Encryption;
import org.edu_sharing.service.InsufficientPermissionException;
import org.edu_sharing.service.authentication.oauth2.TokenService;
import org.edu_sharing.service.authentication.oauth2.TokenService.Token;
import org.edu_sharing.service.connector.ConnectorServiceFactory;
import org.edu_sharing.service.connector.SimpleConnectorAttributes;
import org.edu_sharing.service.editlock.EditLockService;
import org.edu_sharing.service.editlock.EditLockServiceFactory;
import org.edu_sharing.service.editlock.LockedException;
import org.edu_sharing.service.mime.MimeTypesV2;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.permission.PermissionService;
import org.edu_sharing.service.permission.PermissionServiceFactory;
import org.edu_sharing.service.toolpermission.ToolPermissionServiceFactory;
import org.edu_sharing.spring.servlet.SpringHttpServlet;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.context.ApplicationContext;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


public class ConnectorServlet extends SpringHttpServlet {

	private static Logger logger = Logger.getLogger(ConnectorServlet.class);

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String connectorId = req.getParameter("connectorId");
		String nodeId = req.getParameter("nodeId");


		Map<String,String> auth = AuthenticationToolAPI.getInstance().validateAuthentication(req.getSession());

		if(auth == null){
			resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
			return;
		}

		ApplicationInfo homeRepo = ApplicationInfoList.getHomeRepository();

		boolean readOnly=true;
		boolean isCollection;
		String toolInstanceNodeId = null;
		NodeRef nodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId);
		NodeRef nodeRefOriginal = nodeRef;
		try{
			MCAlfrescoBaseClient repoClient = null;
			NodeService nodeService = NodeServiceFactory.getInstance().getLocalService();
			PermissionService permissionService = PermissionServiceFactory.getInstance().getLocalService();
			// if collection ref, use original node
			isCollection = NodeServiceHelper.hasAspect(nodeRef, CCConstants.CCM_ASPECT_COLLECTION_IO_REFERENCE);
			if(isCollection){
				nodeRefOriginal = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeService.getProperty(StoreRef.PROTOCOL_WORKSPACE,StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(),nodeId,CCConstants.CCM_PROP_IO_ORIGINAL));
			}
			// for writing, access to the original is required
			readOnly=!permissionService.hasPermission(StoreRef.PROTOCOL_WORKSPACE,StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(),nodeRefOriginal.getId(),CCConstants.PERMISSION_WRITE);
			// check if user has permissions on the real node (i.e. the reference io)
			if(!permissionService.hasPermission(StoreRef.PROTOCOL_WORKSPACE,StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(),nodeId,CCConstants.PERMISSION_READ_ALL)){
				resp.sendError(HttpServletResponse.SC_FORBIDDEN);
				return;
			}
			// run as system since we're may having redirected to an original io where the user might not have permissions on the original node
			String toolInstanceNodeRef = (String) NodeServiceHelper.getPropertiesOriginal(nodeRef).get(CCConstants.CCM_PROP_TOOL_OBJECT_TOOLINSTANCEREF);
			if(toolInstanceNodeRef != null) {
				toolInstanceNodeId = new NodeRef(toolInstanceNodeRef).getId();
			}
		}catch(Throwable e){
			logger.error(e.getMessage(),e);
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,e.getMessage());
			return;
		}

		Connector connector = null;
		if(connectorId != null) {
			// use the unfiltered list, so a missing toolpermission leads to a 403 and not to an "unknown connector" error
			ConnectorList connectorList = ConnectorServiceFactory.getConnectorService().getConnectorList();
			connector = connectorList.getConnectors().stream().filter(c -> c.getId().equals(connectorId)).findAny().orElse(null);
			Optional<SimpleConnector> simpleConnector = connectorList.getSimpleConnectors() == null
					? Optional.empty()
					: connectorList.getSimpleConnectors().stream().filter(c -> c.getId().equals(connectorId)).findAny();

			if(connector == null && simpleConnector.isEmpty()){
				logger.error("no valid connector " + connectorId);
				resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,"no valid connector");
				return;
			}

			if(!ToolPermissionServiceFactory.getInstance().hasToolPermissionForConnector(connectorId)){
				resp.sendError(HttpServletResponse.SC_FORBIDDEN);
				return;
			}

			if(simpleConnector.isPresent()) {
				Map<String, String[]> requestParameters = convertParameters(req);
				requestParameters.put(SimpleConnectorAttributes.ATTRIBUTE_ORIGINAL_NODE_ID,
						new String[]{ SimpleConnectorAttributes.resolveReferenceOriginalNodeId(nodeId) });
				HashMap<String, Serializable> properties;
				if(simpleConnector.get().getApi() == null) {
					if(StringUtils.isEmpty(simpleConnector.get().getUrl())) {
						logger.error("simple connector " + connectorId + " has neither an \"api\" nor an \"url\" configured");
						resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "no valid connector");
						return;
					}
					properties = handleSimpleConnectorUrl(requestParameters, simpleConnector.get());
				} else {
					// the target is provided by the api result / the postRequestHandler
					properties = handleSimpleConnector(requestParameters, simpleConnector.get(), nodeRefOriginal);
				}
				String redirect = extractSimpleConnectorTarget(simpleConnector.get(), properties);
				NodeServiceFactory.getInstance().getLocalService().updateNodeNative(nodeRefOriginal.getId(), properties);
				if(redirect == null) {
					try {
						// try to re-fetch to obey Node Interceptors!
						redirect = (String) NodeServiceHelper.getProperties(nodeRefOriginal).get(CCConstants.CCM_PROP_IO_WWWURL);
					} catch (Throwable e) {
						redirect = (String) properties.get(CCConstants.CCM_PROP_IO_WWWURL);
					}
				}
				if(StringUtils.isEmpty(redirect)) {
					logger.error("simple connector " + connectorId + " did not provide any target url (" + CCConstants.CCM_PROP_IO_WWWURL + ")");
					resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "no valid connector");
					return;
				}
				resp.sendRedirect(redirect);
				return;
			}
		}

		ApplicationInfo connectorAppInfo = null;
		for(Map.Entry<String, ApplicationInfo> entry : ApplicationInfoList.getApplicationInfos().entrySet()){
			ApplicationInfo appInfo = entry.getValue();
			if(ApplicationInfo.TYPE_CONNECTOR.equals(appInfo.getType())){
				connectorAppInfo = appInfo;
			}
		}

		if(connectorAppInfo == null){
			logger.error("no connector appinfo registered");
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,"no connector appinfo registered");
			return;
		}

		Map<String, Object> properties;
		try {
			properties = NodeServiceHelper.getPropertiesOriginal(nodeRef);
		} catch (Throwable e1) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "node id is invalid or can not be accessed");
			return;
		}
		if(connector != null && ConnectorService.ID_TINYMCE.equals(connector.getId())){
			try{
				EditLockService editLockService = EditLockServiceFactory.getEditLockService();
				if(!readOnly)
					editLockService.lock(nodeRefOriginal);
			}catch( LockedException e){
				resp.sendError(HttpServletResponse.SC_FORBIDDEN, "node is locked by another user");
				return;
			}catch( InsufficientPermissionException e){
				resp.sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
				return;
			}
		}

		try{
			JSONObject jsonObject = new JSONObject();
			// connector get's the real node id. API can handle permissions for references this way
			jsonObject.put("node",nodeId);

			if(connector != null) {
				jsonObject.put("endpoint",connector.getUrl());
				jsonObject.put("tool", connector.getConnectorId()!=null ? connector.getConnectorId() : connector.getId());
				jsonObject.put("defaultCreateElement", connector.getDefaultCreateElement());
				String mimetype = MimeTypesV2.getMimeType(properties, NodeServiceHelper.getType(nodeRef));
				jsonObject.put("mimetype",mimetype);
				for(ConnectorFileType filetype : connector.getFiletypes()){
					if(filetype.getMimetype().equals(mimetype))
						jsonObject.put("filetype", filetype.getFiletype());
				}

				for(ConnectorFileType filetype : connector.getFiletypes()){
					if(filetype.getMimetype().equals(mimetype))
						jsonObject.put("filetype", filetype.getFiletype());
				}
			}

			if(toolInstanceNodeId != null && !toolInstanceNodeId.trim().equals("")) {
				jsonObject.put("tool","LTI");
			}
			// hint that connector should start in edit mode (i.e. for onlyOffice read/preview mode skip)
			jsonObject.put("preferEdit", Boolean.parseBoolean(req.getParameter("preferEdit")));
			jsonObject.put("ts", System.currentTimeMillis() / 1000);
			jsonObject.put("sessionId", req.getSession().getId());
			try{
				jsonObject.put("language",AuthenticationToolAPI.getInstance().getCurrentLanguage());
			}catch(Throwable t){}
			jsonObject.put("ticket", req.getSession().getAttribute(CCConstants.AUTH_TICKET));
			jsonObject.put("api_url",homeRepo.getClientBaseUrl() + "/rest");
			jsonObject.put("appid",homeRepo.getAppId());

			if(req.getSession().getAttribute(CCConstants.AUTH_SCOPE)==null){
				ApplicationContext eduApplicationContext = org.edu_sharing.spring.ApplicationContextFactory.getApplicationContext();
				TokenService tokenService = (TokenService) eduApplicationContext.getBean("oauthTokenService");
				Token token=tokenService.createToken(AuthenticationUtil.getFullyAuthenticatedUser(),(String)req.getSession().getAttribute(CCConstants.AUTH_TICKET));
				jsonObject.put("accessToken", token.getAccessToken());
				jsonObject.put("refreshToken", token.getRefreshToken());
				jsonObject.put("expiresIn", tokenService.getExpiresIn());
			}

			logger.debug("jsonObject:" + jsonObject);


			pushToConnector(jsonObject,connectorAppInfo,resp);

		}catch(Exception e){
			logger.error(e.getMessage(), e);
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,e.getMessage());
			return;
		}



	}

	private Map<String, String[]> convertParameters(HttpServletRequest req) {
		HashMap<String, String[]> converted = new HashMap<>();
		IteratorUtils.toList(req.getParameterNames().asIterator()).forEach(key -> converted.put(key.toString(), req.getParameterValues(key.toString())));
		return converted;
	}

	/**
	 * a connector without an api: the configured url is the target of the element
	 */
	HashMap<String, Serializable> handleSimpleConnectorUrl(Map<String, String[]> requestParameters, SimpleConnector simpleConnector) {
		HashMap<String, Serializable> properties = simpleConnectorBaseProperties(simpleConnector);
		properties.put(CCConstants.CCM_PROP_IO_WWWURL,
				SimpleConnectorAttributes.replaceForUrl(requestParameters, simpleConnector.getUrl()));
		return properties;
	}

	/**
	 * @return the target url in case it should not be stored on the element
	 *         ({@link SimpleConnector.RedirectMode#Redirect}, the url is removed from the given properties),
	 *         null in case it has to be resolved from the element after the properties were written
	 *         ({@link SimpleConnector.RedirectMode#Link})
	 */
	String extractSimpleConnectorTarget(SimpleConnector simpleConnector, Map<String, Serializable> properties) {
		if(SimpleConnector.RedirectMode.Redirect.equals(simpleConnector.getRedirectMode())) {
			return (String) properties.remove(CCConstants.CCM_PROP_IO_WWWURL);
		}
		return null;
	}

	/**
	 * properties marking the element as being handled by the given connector
	 */
	private HashMap<String, Serializable> simpleConnectorBaseProperties(SimpleConnector simpleConnector) {
		HashMap<String, Serializable> properties = new HashMap<>();
		properties.put(CCConstants.CCM_PROP_CCRESSOURCETYPE, RessourceInfoExecuter.CCM_RESSOURCETYPE_CONNECTOR);
		properties.put(CCConstants.CCM_PROP_CCRESSOURCESUBTYPE, simpleConnector.getId());
		return properties;
	}

	HashMap<String, Serializable> handleSimpleConnector(Map<String, String[]> requestParameters, SimpleConnector simpleConnector, NodeRef nodeRefOriginal) throws UnsupportedEncodingException, SimpleErrorWithDetailsException {
		RequestBuilder builder = null;
		String url = SimpleConnectorAttributes.replaceForUrl(requestParameters, simpleConnector.getApi().getUrl());
		if(simpleConnector.getApi().getMethod().equals(SimpleConnector.SimpleConnectorApi.Method.Post)) {
			builder = RequestBuilder.post(url);
			try {
				SimpleConnectorHelper.addAuthentication(simpleConnector, builder);
			}catch(Throwable t){
				throw new SimpleErrorWithDetailsException("Authentication failed for connector " + simpleConnector.getId() + ". Check the configuration.");
			}
			if(simpleConnector.getApi().getBodyType() == null) {

			} else if(simpleConnector.getApi().getBodyType().equals(SimpleConnector.SimpleConnectorApi.BodyType.Form)) {
				List<? extends NameValuePair> data = mapSimpleConnectorBody(requestParameters, simpleConnector);
				try {
					logger.debug(EntityUtils.toString(new UrlEncodedFormEntity(data)));
				}catch(Throwable ignored) {}
				builder.setEntity(new UrlEncodedFormEntity(data));
				builder.setHeader("Content-Type", "application/x-www-form-urlencoded");
			}
		}
		String resultStr = new HttpQueryTool().query(builder);
		try {
			JSONObject result = new JSONObject(resultStr);
			HashMap<String, Serializable> properties = simpleConnectorBaseProperties(simpleConnector);
		if(StringUtils.isNotEmpty(simpleConnector.getApi().getPostRequestHandler())) {
			SimpleConnector.ConnectorRequest request = new SimpleConnector.ConnectorRequest(
					requestParameters, simpleConnector, nodeRefOriginal
			);
			try {
				properties.putAll(
						((SimpleConnector.PostRequestHandler)Class.forName(simpleConnector.getApi().getPostRequestHandler()).getDeclaredConstructor().newInstance()).handleRequest(request, result)
				);
			} catch (Throwable t) {
				throw new RuntimeException("Error for postRequestHandler", t);
			}
		}
		return properties;
		} catch(JSONException e) {
			logger.warn("Invalid json received from API " + builder, e);
			throw e;
		}
	}

	@NotNull
	private List<BasicNameValuePair> mapSimpleConnectorBody(Map<String, String[]> requestParameters, SimpleConnector simpleConnector) {
		List<BasicNameValuePair> pairs = simpleConnector.getApi().getBody().entrySet().stream()
				.map((e) -> new BasicNameValuePair(e.getKey(), SimpleConnectorAttributes.replace(requestParameters, e.getValue().toString(), StringUtils::join)))
				.filter(f -> StringUtils.isNotBlank(f.getValue()))
				.collect(Collectors.toList());
		if (StringUtils.isNotEmpty(simpleConnector.getApi().getBodyHandler())) {
			try {
				SimpleConnector.BodyHandler handler = ((SimpleConnector.BodyHandler) Class.forName(simpleConnector.getApi().getBodyHandler()).getDeclaredConstructor().newInstance());
				pairs = handler.handle(pairs, requestParameters, simpleConnector);
			} catch(Exception e) {
				logger.warn(e.getMessage(), e);
			}
		}
		return pairs;
	}

	public void pushToConnector(JSONObject jsonObject, ApplicationInfo connectorAppInfo, HttpServletResponse resp) throws Exception{
		/**
		 * encrypt the values with AES to prevent the length limit of 245 bytes with RSA
		 */
		KeyGenerator keygen = KeyGenerator.getInstance("AES");
		//maybe use 256:
		//http://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html
		keygen.init(128);
		SecretKey aesKey = keygen.generateKey();
		Encryption eAES = new Encryption("AES");
		byte[] encrypted = eAES.encrypt(jsonObject.toString(), aesKey);
		String url = UrlTool.setParam(connectorAppInfo.getContentUrl(), "e", URLEncoder.encode(java.util.Base64.getEncoder().encodeToString(encrypted)));

		/**
		 * encrypt the AES key with RSA public key
		 */
		Encryption eRSA = new Encryption("RSA");
		byte[] aesKeyEncrypted = eRSA.encrypt(aesKey.getEncoded(), eRSA.getPemPublicKey(connectorAppInfo.getPublicKey()));
		url = UrlTool.setParam(url, "k", URLEncoder.encode(java.util.Base64.getEncoder().encodeToString(aesKeyEncrypted)));
		logger.info("url:" + url + "  length:" + url.length());
		resp.sendRedirect(url);
	}
}
