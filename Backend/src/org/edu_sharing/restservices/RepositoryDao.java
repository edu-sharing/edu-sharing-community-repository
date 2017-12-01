package org.edu_sharing.restservices;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.MCAlfrescoBaseClient;
import org.edu_sharing.repository.server.RepoFactory;
import org.edu_sharing.repository.server.authentication.Context;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.collection.CollectionService;
import org.edu_sharing.service.collection.CollectionServiceFactory;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.rendering.RenderingService;
import org.edu_sharing.service.rendering.RenderingServiceFactory;

public class RepositoryDao {

	public static final String HOME = "-home-";

	static Logger logger = Logger.getLogger(RepositoryDao.class);
	
	public static RepositoryDao getRepository(String repId) throws DAOException {

		try {
			ApplicationInfo appInfo = 
					HOME.equals(repId) 
				  ? ApplicationInfoList.getHomeRepository() 
				  : ApplicationInfoList.getRepositoryInfoById(repId); 
			
			if (appInfo == null) {
				
				throw new DAOMissingException(
						new IllegalArgumentException(repId));
			}
				  
			if (appInfo == null || !(ApplicationInfo.TYPE_REPOSITORY.equals(appInfo.getType()))) {
				
				throw new DAOValidationException(
						new IllegalArgumentException("unsupported repository type."));
			}

			
			//WSClient is deprecated always use ApiClient, WS Impl of Service Tier is used
			MCAlfrescoBaseClient baseClient = null;
			
			//prevent when there is a runas user that authenticationService.validate (AuthenticationUtil) overwrites the runas user
			if((AuthenticationUtil.isRunAsUserTheSystemUser() || "admin".equals(AuthenticationUtil.getRunAsUser())) 
					&& ApplicationInfoList.getHomeRepository().getAppId().equals(appInfo.getAppId())){
				baseClient = new MCAlfrescoAPIClient();
			}else {
				baseClient = (MCAlfrescoBaseClient) RepoFactory.getInstance(
						appInfo.getAppId(), 
						//ApplicationInfoList.getHomeRepository().getAppId(),
						Context.getCurrentInstance().getRequest().getSession());
			}
					
					
					

			CollectionService collectionClient = CollectionServiceFactory.getCollectionService(appInfo.getAppId());
			
			RenderingService renderingClient = RenderingServiceFactory.getRenderingService(appInfo.getAppId());
			
			return new RepositoryDao(appInfo, baseClient, collectionClient, renderingClient);
			
		} catch (Throwable t) {
			throw DAOException.mapping(t);
		}
	}

	public static List<RepositoryDao> getRepositories() throws DAOException {

		try {
			
			List<RepositoryDao> result = new ArrayList<RepositoryDao>();
			for (ApplicationInfo appInfo : ApplicationInfoList.getRepositoryInfosOrdered()) {
	
				if (ApplicationInfo.TYPE_REPOSITORY.equals(appInfo.getType())) {
					result.add(getRepository(appInfo.getAppId()));
				}
			}
			
			return result;
			
		} catch (Throwable t) {
	
			throw DAOException.mapping(t);
		}
	}
	
	private final ApplicationInfo appInfo;
	private final MCAlfrescoBaseClient baseClient;
	private final CollectionService collectionClient;
	
	private final RenderingService renderingClient;
	private NodeService nodeService;
	
	private RepositoryDao(ApplicationInfo appInfo, MCAlfrescoBaseClient baseClient, CollectionService collectionClient, RenderingService renderingClient) {
		
		this.appInfo = appInfo;
		this.baseClient = baseClient;
		this.collectionClient = collectionClient;
		this.renderingClient = renderingClient;
		this.nodeService=NodeServiceFactory.getNodeService(appInfo.getAppId());
	}
	
	public String getId() {
		return this.appInfo.getAppId();
	}
	
	public String getCaption() {
		
		return this.appInfo.getAppCaption();
	}

	public boolean isHomeRepo() {
		
		return this.appInfo.ishomeNode();
	}
	
	ApplicationInfo getApplicationInfo() {
		
		return appInfo;
	}
	
	public MCAlfrescoBaseClient getBaseClient() {
		
		return baseClient;
	}
	
	CollectionService getCollectionClient() {
		
		return collectionClient;
	}
	
	RenderingService getRenderingServiceClient(){
		return renderingClient;
	}
	
	public String getUserName(){
		return new AuthenticationToolAPI().getAuthentication(Context.getCurrentInstance().getRequest().getSession()).get(CCConstants.AUTH_USERNAME);
	}
	
	public String getUserHome() throws Exception{
	
		return baseClient.getHomeFolderID(getUserName());
		
	}
	public String getUserInbox() {
		return nodeService.getOrCreateUserInbox();
	}

	public String getRepositoryType() {
		return appInfo.getRepositoryType();
	}
	
	
}
