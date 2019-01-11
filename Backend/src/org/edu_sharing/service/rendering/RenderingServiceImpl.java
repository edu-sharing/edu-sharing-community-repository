package org.edu_sharing.service.rendering;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.log4j.Logger;
import org.edu_sharing.metadataset.v2.MetadataReaderV2;
import org.edu_sharing.metadataset.v2.MetadataSetV2;
import org.edu_sharing.metadataset.v2.tools.MetadataHelper;
import org.edu_sharing.metadataset.v2.tools.MetadataTemplateRenderer;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.UrlTool;
import org.edu_sharing.repository.server.AuthenticationTool;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.MCAlfrescoBaseClient;
import org.edu_sharing.repository.server.RepoFactory;
import org.edu_sharing.repository.server.authentication.Context;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.HttpQueryTool;
import org.edu_sharing.repository.server.tools.URLTool;
import org.edu_sharing.restservices.DAOException;
import org.edu_sharing.restservices.NodeDao;
import org.edu_sharing.restservices.PersonDao;
import org.edu_sharing.restservices.RepositoryDao;
import org.edu_sharing.restservices.shared.Filter;
import org.edu_sharing.restservices.shared.Node;
import org.edu_sharing.service.InsufficientPermissionException;
import org.edu_sharing.service.config.ConfigServiceFactory;
import org.edu_sharing.service.permission.PermissionService;
import org.edu_sharing.service.permission.PermissionServiceFactory;
import org.edu_sharing.service.version.VersionService;

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
	public String getDetails(String nodeId,String nodeVersion,Map<String,String> parameters) throws InsufficientPermissionException, Exception{		
		
		if(!this.permissionService.hasPermission(StoreRef.PROTOCOL_WORKSPACE,StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(),nodeId,CCConstants.PERMISSION_READ)){
			throw new InsufficientPermissionException("no read permission");
		}
		String renderingServiceUrl = "";
		try {
			ApplicationInfo appInfo = ApplicationInfoList.getRepositoryInfoById(this.appInfo.getAppId());
			renderingServiceUrl = new RenderingTool().getRenderServiceUrl(appInfo,parameters,RenderingTool.DISPLAY_DYNAMIC);
			// base url for dynamic context routing of domains
			renderingServiceUrl = UrlTool.setParam(renderingServiceUrl, "baseUrl",URLEncoder.encode(URLTool.getBaseUrl(true)));
			logger.debug(renderingServiceUrl);
			RenderingServiceData data = getData(nodeId,nodeVersion);
			return getDetails(renderingServiceUrl, data);
		}catch(Throwable t) {
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
				throw new Exception(t.getMessage()+" ("+info+")",t);
			}
		}
	
	}

	@Override
	public String getDetails(String renderingServiceUrl, RenderingServiceData data) throws JsonProcessingException, UnsupportedEncodingException {
		PostMethod post = new PostMethod(renderingServiceUrl);
		ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
		String json = ow.writeValueAsString(data);
		post.setRequestEntity(new StringRequestEntity(json,"application/json","UTF-8"));
		return new HttpQueryTool().query(post);
	}
	@Override
	public RenderingServiceData getData(String nodeId, String nodeVersion) throws Exception {
		long time=System.currentTimeMillis();
		RenderingServiceData data=new RenderingServiceData();
		RepositoryDao repoDao = RepositoryDao.getRepository(appInfo.getAppId());
		NodeDao nodeDao = NodeDao.getNodeWithVersion(repoDao, nodeId,nodeVersion);

		Node node = nodeDao.asNode();
		data.setNode(node);

		data.setChildren(
				NodeDao.convertToRest(repoDao,Filter.createShowAllFilter(),nodeDao.getChildrenSubobjects(),0,Integer.MAX_VALUE).getNodes()
		);
		// template
		data.setMetadataHTML(new MetadataTemplateRenderer(
				MetadataHelper.getMetadataset(
						appInfo,node.getMetadataset()==null ? CCConstants.metadatasetdefault_id : node.getMetadataset()),
				nodeDao.getAllProperties()).render("io_render"));

		// user
		data.setUser(PersonDao.getPerson(repoDao,AuthenticationUtil.getFullyAuthenticatedUser()).asPersonSimple());

		// context/config
		data.setConfigValues(ConfigServiceFactory.getCurrentConfig().values);

		logger.info("Preparing rendering data took "+(System.currentTimeMillis()-time)+" ms");
		return data;
	}
}
