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
import org.apache.log4j.Logger;
import org.edu_sharing.metadataset.v2.tools.MetadataHelper;
import org.edu_sharing.metadataset.v2.tools.MetadataTemplateRenderer;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.UrlTool;
import org.edu_sharing.repository.server.AuthenticationTool;
import org.edu_sharing.repository.server.RepoFactory;
import org.edu_sharing.repository.server.authentication.Context;
import org.edu_sharing.repository.server.rendering.RenderingErrorServlet;
import org.edu_sharing.repository.server.rendering.RenderingException;
import org.edu_sharing.repository.server.tools.*;
import org.edu_sharing.restservices.*;
import org.edu_sharing.restservices.shared.Filter;
import org.edu_sharing.restservices.shared.Node;
import org.edu_sharing.service.InsufficientPermissionException;
import org.edu_sharing.service.config.ConfigServiceFactory;
import org.edu_sharing.service.permission.PermissionService;
import org.edu_sharing.service.permission.PermissionServiceFactory;

import javax.servlet.http.HttpServletRequest;
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
			renderingServiceUrl = new RenderingTool().getRenderServiceUrl(appInfo,nodeId,parameters,displayMode);
			// base url for dynamic context routing of domains
			renderingServiceUrl = UrlTool.setParam(renderingServiceUrl, "baseUrl",URLEncoder.encode(URLTool.getBaseUrl(true)));
			logger.debug(renderingServiceUrl);
			RenderingServiceData data = getData(nodeId,nodeVersion,AuthenticationUtil.getFullyAuthenticatedUser(),displayMode);
			return getDetails(renderingServiceUrl, data);
		}catch(Throwable t) {
			logger.warn(t.getMessage(),t);
			return RenderingErrorServlet.errorToHTML((HttpServletRequest) Context.getCurrentInstance().getRequest().getSession().getServletContext(),
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
				throw new Exception(t.getMessage()+" ("+info+")",t);
			}
			*/
		}
	
	}

	@Override
	public String getDetails(String renderingServiceUrl, RenderingServiceData data) throws JsonProcessingException, UnsupportedEncodingException {
		PostMethod post = new PostMethod(renderingServiceUrl);
		ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
		String json = ow.writeValueAsString(data);
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
			return RenderingErrorServlet.errorToHTML((HttpServletRequest) Context.getCurrentInstance().getRequest().getSession().getServletContext(),
					new RenderingException(e));
		}
	}
	@Override
	public RenderingServiceData getData(String nodeId, String nodeVersion, String user, String displayMode) throws Exception {
		long time=System.currentTimeMillis();
		RenderingServiceData data=new RenderingServiceData();
		RepositoryDao repoDao = RepositoryDao.getRepository(appInfo.getAppId());
		NodeDao nodeDao = NodeDao.getNodeWithVersion(repoDao, nodeId, nodeVersion);
		Node node = nodeDao.asNode();
		data.setNode(node);
		data.setChildren(
				NodeDao.convertToRest(repoDao,Filter.createShowAllFilter(),nodeDao.getChildrenSubobjects(),0,Integer.MAX_VALUE).getNodes()
		);
		// template
		// switch to the remote appInfo (for shadow objects) so the mds is the right one
		ApplicationInfo remoteApp=ApplicationInfoList.getRepositoryInfoById(nodeDao.getRepositoryDao().getId());
		data.setMetadataHTML(new MetadataTemplateRenderer(
				MetadataHelper.getMetadataset(
						remoteApp,node.getMetadataset()==null ? CCConstants.metadatasetdefault_id : node.getMetadataset()),
				nodeDao.getAllProperties()).render(RenderingTool.DISPLAY_INLINE.equals(displayMode) ? "io_render_inline" : "io_render"));

		// user
		data.setUser(PersonDao.getPerson(RepositoryDao.getHomeRepository(),user).asPersonSimple());

		// context/config
		data.setConfigValues(ConfigServiceFactory.getCurrentConfig().values);

		logger.info("Preparing rendering data took "+(System.currentTimeMillis()-time)+" ms");
		return data;
	}
}
