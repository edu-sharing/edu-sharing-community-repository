package org.edu_sharing.service.rendering;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.GsonBuilder;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.log4j.Logger;
import org.edu_sharing.metadataset.v2.tools.MetadataHelper;
import org.edu_sharing.metadataset.v2.tools.MetadataTemplateRenderer;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.UrlTool;
import org.edu_sharing.repository.server.AuthenticationTool;
import org.edu_sharing.repository.server.RepoFactory;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.repository.server.rendering.RenderingErrorServlet;
import org.edu_sharing.repository.server.rendering.RenderingException;
import org.edu_sharing.repository.server.tools.*;
import org.edu_sharing.restservices.*;
import org.edu_sharing.restservices.shared.Filter;
import org.edu_sharing.restservices.shared.Node;
import org.edu_sharing.restservices.shared.SearchResult;
import org.edu_sharing.service.InsufficientPermissionException;
import org.edu_sharing.service.config.ConfigServiceFactory;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.permission.PermissionService;
import org.edu_sharing.service.permission.PermissionServiceFactory;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.model.SortDefinition;

import javax.servlet.http.HttpServletResponse;

public class RenderingServiceImpl implements RenderingService{


	private PermissionService permissionService;
	ApplicationInfo appInfo;
	
	HashMap<String,String> authInfo;
	

	AuthenticationTool authTool;
	
	Logger logger = Logger.getLogger(RenderingServiceImpl.class);
	
	public RenderingServiceImpl(String appId){

		try{
			this.appInfo = ApplicationInfoList.getRepositoryInfoById(appId);
			this.authTool = RepoFactory.getAuthenticationToolInstance(appId);
			this.permissionService = PermissionServiceFactory.getLocalService();

			if((AuthenticationUtil.isRunAsUserTheSystemUser() || "admin".equals(AuthenticationUtil.getRunAsUser())) ) {
				logger.debug("starting in runas user mode");
				this.authInfo = new HashMap<String,String>();
				this.authInfo.put(CCConstants.AUTH_USERNAME, AuthenticationUtil.getRunAsUser());
			}else {
				this.authInfo = this.authTool.validateAuthentication(Context.getCurrentInstance().getCurrentInstance().getRequest().getSession());
			}
			
			
		}catch(Throwable e){
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public String getDetails(String nodeId,String nodeVersion,String displayMode,Map<String,String> parameters) throws InsufficientPermissionException, Exception{
		
		if(!this.permissionService.hasPermission(StoreRef.PROTOCOL_WORKSPACE,StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(),nodeId,CCConstants.PERMISSION_READ)){
			throw new InsufficientPermissionException("no read permission");
		}
		String renderingServiceUrl = "";
		try {
			ApplicationInfo appInfo = ApplicationInfoList.getRepositoryInfoById(this.appInfo.getAppId());
			// switch to home repo if the defined app is "local" (secondary home repo)
			if(appInfo.getRepositoryType().equals(ApplicationInfo.REPOSITORY_TYPE_LOCAL)){
				appInfo = ApplicationInfoList.getHomeRepository();
			}
			renderingServiceUrl = new RenderingTool().getRenderServiceUrl(appInfo,nodeId,parameters,displayMode);
			// base url for dynamic context routing of domains
			renderingServiceUrl = UrlTool.setParam(renderingServiceUrl, "baseUrl",URLEncoder.encode(URLTool.getBaseUrl(true)));
			logger.debug(renderingServiceUrl);
			RenderingServiceOptions options = new RenderingServiceOptions();
			options.displayMode = displayMode;
			RenderingServiceData data = getData(appInfo, nodeId, nodeVersion, AuthenticationUtil.getFullyAuthenticatedUser(), options);
			return getDetails(renderingServiceUrl, data);
		}catch(Throwable t) {
			logger.warn(t.getMessage(),t);
			return RenderingErrorServlet.errorToHTML(null,
					new RenderingException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,t.getMessage(),RenderingException.I18N.unknown,t));
			/*
			String repository=VersionService.getVersionNoException(VersionService.Type.REPOSITORY);
			String rs=VersionService.getVersionNoException(VersionService.Type.RENDERSERVICE);
			String info="Repository version "+repository+", Renderservice version "+rs;
			logger.info("called url:" + renderingServiceUrl);
			if(repository.equals(rs)) {
				logger.info(info);
				throw t;
			}
			else {
				info+=" do not match";
				logger.warn(info);
				throw t;
			}
			*/
		}
	
	}

	@Override
	public String getDetails(String renderingServiceUrl, RenderingServiceData data) throws JsonProcessingException, UnsupportedEncodingException {
		PostMethod post = new PostMethod(renderingServiceUrl);
		/*
		ObjectMapper mapper = new ObjectMapper();
		ObjectWriter ow = mapper.writer().withDefaultPrettyPrinter();
		String json = ow.writeValueAsString(data);
		*/
		String json=new GsonBuilder().serializeNulls().create().toJson(data);
		/*
		Encryption encryption=new Encryption("RSA");
		try {
			json=new String(encryption.encrypt(json, encryption.getPemPublicKey(ApplicationInfoList.getRenderService().getPublicKey())));
		} catch (Exception e) {
			logger.warn(e);
			return "";
		}
		*/
		post.setRequestEntity(new StringRequestEntity(json,"application/json","UTF-8"));
		try {
			return new HttpQueryTool().query(post);
		}catch(HttpException e){
			return RenderingErrorServlet.errorToHTML(null,new RenderingException(e));
		}
	}
	@Override
	public RenderingServiceData getData(ApplicationInfo appInfo, String nodeId, String nodeVersion, String user, RenderingServiceOptions options) throws Throwable {
		long time=System.currentTimeMillis();
		NodeService nodeService = NodeServiceFactory.getNodeService(appInfo.getAppId());
		RenderingServiceData data=new RenderingServiceData();
		RepositoryDao repoDao = RepositoryDao.getRepository(this.appInfo.getAppId());
		NodeDao nodeDao = NodeDao.getNodeWithVersion(repoDao, nodeId, nodeVersion);

		// child object: inherit all props from parent
		nodeDao.setNativeProperties(nodeDao.getInheritedPropertiesFromParent());

		Node node = nodeDao.asNode();

		if(appInfo.ishomeNode()) {
			if (nodeService.hasAspect(StoreRef.PROTOCOL_WORKSPACE,
					StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(),
					nodeId,
					CCConstants.CCM_ASPECT_COLLECTION_IO_REFERENCE)) {
				String original = nodeService.getProperty(StoreRef.PROTOCOL_WORKSPACE,
						StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(),
						nodeId, CCConstants.CCM_PROP_IO_ORIGINAL);
				AuthenticationUtil.runAsSystem(() -> {
					try {
						NodeDao originalNodeDao = NodeDao.getNode(repoDao, original);
						node.setContent(originalNodeDao.asNode().getContent());
					} catch (DAOException e) {
						logger.error(e.getMessage());
					}
					return null;
				});
			}
		}
		ApplicationInfo remoteApp=ApplicationInfoList.getRepositoryInfoById(nodeDao.getRepositoryDao().getId());
		// remove any javascript (important for title)
		node.setProperties(new HashMap<>(new MetadataTemplateRenderer(
				MetadataHelper.getMetadataset(
					remoteApp,node.getMetadataset()==null ? CCConstants.metadatasetdefault_id : node.getMetadataset()),
				new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId),
				user,
				nodeDao.getNativeProperties())
			.getProcessedProperties()));
		data.setNode(node);
		if(CCConstants.CCM_TYPE_SAVED_SEARCH.equals(nodeService.getType(nodeId))){
			SearchResult<Node> search = nodeDao.runSavedSearch(0,
					options.savedSearch.getMaxItems(),
					SearchService.ContentType.FILES,
					new SortDefinition(Collections.singletonList(options.savedSearch.getSortBy()),
							Collections.singletonList(options.savedSearch.getSortAscending())),
					null);
			data.setChildren(search.getNodes());
		}else{
			data.setChildren(
					NodeDao.convertToRest(repoDao, Filter.createShowAllFilter(), nodeDao.getChildrenSubobjects(), 0, Integer.MAX_VALUE).getNodes()
			);
		}
		// template
		// switch to the remote appInfo (for shadow objects) so the mds is the right one
		data.setMetadataHTML(new MetadataTemplateRenderer(
				MetadataHelper.getMetadataset(
						remoteApp,node.getMetadataset()==null ? CCConstants.metadatasetdefault_id : node.getMetadataset()),
				new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,nodeId),
				user,
				nodeDao.getNativeProperties()).render(RenderingTool.DISPLAY_INLINE.equals(options.displayMode) ? "io_render_inline" : "io_render"));

		// user
		if(!AuthenticationUtil.isRunAsUserTheSystemUser()) {
			data.setUser(PersonDao.getPerson(RepositoryDao.getHomeRepository(), user).asPersonRender());
		}

		// context/config
		data.setConfigValues(ConfigServiceFactory.getCurrentConfig().values);

		logger.info("Preparing rendering data took "+(System.currentTimeMillis()-time)+" ms");
		return data;
	}

	@Override
	public boolean renderingSupported() {
		return true;
	}
}
