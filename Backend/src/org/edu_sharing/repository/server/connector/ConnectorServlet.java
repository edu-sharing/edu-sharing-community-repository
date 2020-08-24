package org.edu_sharing.repository.server.connector;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.UrlTool;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.MCAlfrescoBaseClient;
import org.edu_sharing.repository.server.RepoFactory;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.security.Encryption;
import org.edu_sharing.service.Constants;
import org.edu_sharing.service.InsufficientPermissionException;
import org.edu_sharing.service.authentication.oauth2.TokenService;
import org.edu_sharing.service.authentication.oauth2.TokenService.Token;
import org.edu_sharing.service.connector.Connector;
import org.edu_sharing.service.connector.ConnectorFileType;
import org.edu_sharing.service.connector.ConnectorService;
import org.edu_sharing.service.connector.ConnectorServiceFactory;
import org.edu_sharing.service.editlock.EditLockService;
import org.edu_sharing.service.editlock.EditLockServiceFactory;
import org.edu_sharing.service.editlock.LockedException;
import org.edu_sharing.service.mime.MimeTypesV2;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.permission.PermissionService;
import org.edu_sharing.service.permission.PermissionServiceFactory;
import org.edu_sharing.service.toolpermission.ToolPermissionServiceFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.context.ApplicationContext;


public class ConnectorServlet extends HttpServlet  {

	Logger logger = Logger.getLogger(ConnectorServlet.class);
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String connectorId = req.getParameter("connectorId");
		String nodeId = req.getParameter("nodeId");
		
		
		HashMap<String,String> auth = new AuthenticationToolAPI().validateAuthentication(req.getSession());
		
		if(auth == null){
			resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
			return;
		}
		
		ApplicationInfo homeRepo = ApplicationInfoList.getHomeRepository();
		
		boolean readOnly=true;
		String toolInstanceNodeId = null;
		try{
			MCAlfrescoBaseClient repoClient = null;
			NodeService nodeService = NodeServiceFactory.getLocalService();
			PermissionService permissionService = PermissionServiceFactory.getLocalService();
			// if collection ref, use original node
			String realNodeId=nodeId;
			if(Arrays.asList(nodeService.getAspects(StoreRef.PROTOCOL_WORKSPACE,StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(),nodeId)).contains(CCConstants.CCM_ASPECT_COLLECTION_IO_REFERENCE)){
				logger.info("ConnectorServlet detected io reference "+nodeId+", will sent original io node ref to service");
				nodeId = nodeService.getProperty(StoreRef.PROTOCOL_WORKSPACE,StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(),nodeId,CCConstants.CCM_PROP_IO_ORIGINAL);
			}
			// for writing, access to the original is required
			readOnly=!permissionService.hasPermission(StoreRef.PROTOCOL_WORKSPACE,StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(),nodeId,CCConstants.PERMISSION_WRITE);
			// check if user has permissions on the real node (i.e. the reference io)
			if(!permissionService.hasPermission(StoreRef.PROTOCOL_WORKSPACE,StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(),realNodeId,CCConstants.PERMISSION_READ_ALL)){
				resp.sendError(HttpServletResponse.SC_FORBIDDEN);
				return;
			}
			
			String toolInstanceNodeRef = nodeService.getProperty(MCAlfrescoAPIClient.storeRef.getProtocol(), MCAlfrescoAPIClient.storeRef.getIdentifier(), nodeId, CCConstants.CCM_PROP_TOOL_OBJECT_TOOLINSTANCEREF);
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
			for(Connector con : ConnectorServiceFactory.getConnectorList().getConnectors()){
				if(con.getId().equals(connectorId)){
					connector = con;
				}
			}
			if(connector == null){
				logger.error("no valid connector");
				resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,"no valid connector");
				return;
			}

			if(!ToolPermissionServiceFactory.getInstance().hasToolPermissionForConnector(connectorId)){
				resp.sendError(HttpServletResponse.SC_FORBIDDEN);
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
		
		NodeRef nodeRef = new NodeRef(Constants.storeRef,nodeId);
		NodeService nodeService=NodeServiceFactory.getLocalService();
		HashMap<String, Object> properties=null;
		try {
			properties = nodeService.getProperties(nodeRef.getStoreRef().getProtocol(), nodeRef.getStoreRef().getIdentifier(), nodeId);
		} catch (Throwable e1) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "node id is invalid or can not be accessed");
			return;
		}
		if(connector != null && ConnectorService.ID_TINYMCE.equals(connector.getId())){
			try{
				EditLockService editLockService = EditLockServiceFactory.getEditLockService();
				if(!readOnly)
					editLockService.lock(nodeRef);
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
			jsonObject.put("node",nodeId);

			if(connector != null) {
				jsonObject.put("endpoint",connector.getUrl());
				jsonObject.put("tool", connector.getId());
				jsonObject.put("defaultCreateElement", connector.getDefaultCreateElement());
				String mimetype = MimeTypesV2.getMimeType(properties);
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

			jsonObject.put("ts", System.currentTimeMillis() / 1000);
            jsonObject.put("sessionId", req.getSession().getId());
            try{
                jsonObject.put("language",new AuthenticationToolAPI().getCurrentLanguage());
            }catch(Throwable t){}
            jsonObject.put("ticket", req.getSession().getAttribute(CCConstants.AUTH_TICKET));
			jsonObject.put("api_url",homeRepo.getClientBaseUrl() + "/rest");
			
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
