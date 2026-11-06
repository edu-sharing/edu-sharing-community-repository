package org.edu_sharing.service.archive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.metadataset.v2.MetadataQueries;
import org.edu_sharing.metadataset.v2.MetadataReader;
import org.edu_sharing.metadataset.v2.MetadataSet;
import org.edu_sharing.metadataset.v2.tools.MetadataHelper;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.UserEnvironmentTool;
import org.edu_sharing.repository.server.tools.cache.RepositoryCache;
import org.edu_sharing.repository.server.tools.forms.DuplicateFinder;

import org.edu_sharing.service.archive.model.RestoreResult;
import org.edu_sharing.service.authority.AuthorityService;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.SearchService.ContentType;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.service.search.model.SearchToken;
import org.edu_sharing.service.search.model.SortDefinition;
import org.springframework.context.ApplicationContext;

import static org.edu_sharing.repository.server.policies.ReservedNodesPolicy.TX_RESTORE_SYS_FOLDER;

public class ArchiveServiceImpl implements ArchiveService  {
	
	
	public static final String RESTORESTATUS_FALLBACK_PARENT_NOT_EXISTS = "FALLBACK_PARENT_NOT_EXISTS";
	
	public static final String RESTORESTATUS_FALLBACK_PARENT_NO_PERMISSION = "FALLBACK_PARENT_NO_PERMISSION";
	
	public static final String RESTORESTATUS_DUPLICATENAME = "DUPLICATENAME";
	
	public static final String RESTORESTATUS_FINE = "FINE";
	
	
	
	ApplicationInfo appInfo;
	MCAlfrescoAPIClient client;
	
	SearchService searchService;
	
	Logger logger = Logger.getLogger(ArchiveServiceImpl.class);
	
	ApplicationContext alfApplicationContext = AlfAppContextGate.getApplicationContext();

	ServiceRegistry serviceRegistry = (ServiceRegistry) alfApplicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
	NodeService nodeService = serviceRegistry.getNodeService();

	private AuthorityService authorityService;
	
	public ArchiveServiceImpl() {
		try{
			this.appInfo = ApplicationInfoList.getHomeRepository();
			this.client = new MCAlfrescoAPIClient();
			this.searchService = SearchServiceFactory.getSearchService(this.appInfo.getAppId());
			this.authorityService = AuthorityServiceFactory.getAuthorityService(this.appInfo.getAppId());
			
		}catch(Throwable e){
			throw new RuntimeException(e.getMessage());
		}
	}
	
	@Override
	public void purge(List<String> archivedNodeIds) {
		for(String archivedNodeId : archivedNodeIds){
			this.client.removeNode(MCAlfrescoAPIClient.archiveStoreRef.getProtocol(), MCAlfrescoAPIClient.archiveStoreRef.getIdentifier(), archivedNodeId);
			// clear cache so that primary parent etc. gets newly resolved
			new RepositoryCache().remove(archivedNodeId);
		}
	}
	
	@Override
	public List<RestoreResult> restore(List<String> archivedNodeIds, String toFolder) {
		List<RestoreResult> result = new ArrayList<>();
		for(String archivedNodeId : archivedNodeIds){
			result.add(this.restoreNode(archivedNodeId, toFolder));
		}
		return result;
	}
	
	@Override
	public SearchResultNodeRef search(String searchWord, int from, int maxResults,SortDefinition sortDefinition) {
		if(!authorityService.isGlobalAdmin()){
			return search(searchWord,AuthenticationUtil.getFullyAuthenticatedUser(),from,maxResults,sortDefinition);
		}
		return search(searchWord,null,from,maxResults,sortDefinition);
	}

	private SearchToken getSearchToken(int from, int maxResults, SortDefinition sortDefinition) throws Exception {
		SearchToken searchToken = new SearchToken();
		searchToken.setFrom(from);
		searchToken.setMaxResult(maxResults);
		searchToken.setStoreName(MCAlfrescoAPIClient.archiveStoreRef.getIdentifier());
		searchToken.setStoreProtocol(MCAlfrescoAPIClient.archiveStoreRef.getProtocol());
		searchToken.setSortDefinition(sortDefinition);
		searchToken.setContentType(ContentType.FILES_AND_FOLDERS);
		return searchToken;
	}

	@Override
	public SearchResultNodeRef search(String searchWord, String user, int from, int maxResults, SortDefinition sortDefinition) {
		try{
			SearchToken searchToken = getSearchToken(from, maxResults, sortDefinition);
			MetadataSet metadataset = MetadataHelper.getMetadataset(ApplicationInfoList.getHomeRepository(), CCConstants.metadatasetdefault_id);
			Map<String, String[]> params=new HashMap<>();
			params.put(MetadataSet.DEFAULT_CLIENT_QUERY_CRITERIA,new String[]{searchWord});
			if(user!=null && !user.isEmpty()) {
				params.put("user", new String[]{user});
			}
			return this.searchService.search(metadataset,"archive",params,searchToken);
		}catch(Throwable e){
			logger.error(e.getMessage(), e);
			return null;
		}
	}
	
	private RestoreResult restoreNode(String archivedNodeId, String destinationParentId){
		
		NodeRef archivedNodeRef = new NodeRef(MCAlfrescoAPIClient.archiveStoreRef,archivedNodeId);
		String name = client.getProperty(MCAlfrescoAPIClient.archiveStoreRef,archivedNodeId,CCConstants.CM_NAME);
		
		RestoreResult restoreResult = new RestoreResult();
		restoreResult.setArchiveNodeId(archivedNodeId);
		
		
		//try to use original
		if(destinationParentId == null || destinationParentId.trim().equals("")){
			ChildAssociationRef childRef = (ChildAssociationRef)nodeService.getProperty(archivedNodeRef, QName.createQName(CCConstants.SYS_PROP_ARCHIVED_ORIGINAL_PARENT_ASSOC));
			if(childRef != null){
				destinationParentId = childRef.getParentRef().getId();
			}else{
				try{
					destinationParentId = new UserEnvironmentTool(null,client.getAuthenticationInfo()).getDefaultUserDataFolder();
					restoreResult.setRestoreStatus(RESTORESTATUS_FALLBACK_PARENT_NOT_EXISTS);
				}catch(Throwable e){
					logger.error(e.getMessage(), e);
				}
			}
		}
		
		if(!client.exists(destinationParentId)){
			try{
				destinationParentId = new UserEnvironmentTool(null,client.getAuthenticationInfo()).getDefaultUserDataFolder();
				restoreResult.setRestoreStatus(RESTORESTATUS_FALLBACK_PARENT_NOT_EXISTS);
			}catch(Throwable e){
				logger.error(e.getMessage(), e);
			}
		}
		
		if(!client.hasPermissions(destinationParentId, new String[]{CCConstants.PERMISSION_ADD_CHILDREN})){
			try{
				destinationParentId = new UserEnvironmentTool(null,client.getAuthenticationInfo()).getDefaultUserDataFolder();
				restoreResult.setRestoreStatus(RESTORESTATUS_FALLBACK_PARENT_NO_PERMISSION);
			}catch(Throwable e){
				logger.error(e.getMessage(), e);
			}
		}
		
		if(destinationParentId == null){
			throw new RuntimeException("can not restore, no target available");
		}

		String fdestinationParentId = destinationParentId;
		String status = restoreResult.getRestoreStatus();
		NodeRef restoredNode = serviceRegistry.getRetryingTransactionHelper().doInTransaction(() -> {
			restoreResult.setRestoreStatus(status);
			restoreResult.setName(name);
			String iname = name;
			try {
				String newName = new DuplicateFinder().getUniqueValue(fdestinationParentId, CCConstants.CM_NAME, iname);
				if (!newName.equals(iname)) {
					iname = newName;
					nodeService.setProperty(archivedNodeRef, QName.createQName(CCConstants.CM_NAME), iname);
					QName type = nodeService.getType(archivedNodeRef);

					if (QName.createQName(CCConstants.CCM_TYPE_IO).equals(type)) {
						nodeService.setProperty(archivedNodeRef, QName.createQName(CCConstants.LOM_PROP_GENERAL_TITLE), iname);
					}
					restoreResult.setRestoreStatus(RESTORESTATUS_DUPLICATENAME);
				}
				restoreResult.setName(iname);

			} catch (Throwable e) {
				logger.error(e.getMessage(), e);
			}

			try{
				boolean isReservedFolder = restoresAsSystemFolder(archivedNodeRef, fdestinationParentId);

				if (isReservedFolder){
					AlfrescoTransactionSupport.bindResource(TX_RESTORE_SYS_FOLDER, Boolean.TRUE);
				}

				String assocName = QName.createValidLocalName(iname);
				assocName = "{" + CCConstants.NAMESPACE_CCM + "}" + assocName;


				return nodeService.restoreNode(archivedNodeRef,
						new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, fdestinationParentId),
						QName.createQName(CCConstants.CM_ASSOC_FOLDER_CONTAINS),
						QName.createQName(assocName));
			}finally{
				AlfrescoTransactionSupport.unbindResource(TX_RESTORE_SYS_FOLDER);
			}
		});

		// clear cache so that primary parent etc. gets newly resolved
		new RepositoryCache().remove(restoredNode.getId());
		
		restoreResult.setNodeId(restoredNode.getId());
		restoreResult.setParent(destinationParentId);
		restoreResult.setPath(client.getPath(restoredNode.getId()));
		if(restoreResult.getRestoreStatus() == null){
			restoreResult.setRestoreStatus(RESTORESTATUS_FINE);
		}

		new RepositoryCache().remove(restoredNode.getId());
		
		return restoreResult;
	}

	/**
	 * a user system folder (inbox, saved search) is recreated automatically once it was deleted.
	 * if the archived one is restored to a parent which already holds such a folder,
	 * the map type is removed so the restored node becomes a regular folder and the user does not end up with two of them
	 *
	 * returns true if it is a systemFolder
	 */
	private boolean restoresAsSystemFolder(NodeRef archivedNodeRef, String destinationParentId) {
		QName mapTypeProp = QName.createQName(CCConstants.CCM_PROP_MAP_TYPE);
		String mapType = (String) nodeService.getProperty(archivedNodeRef, mapTypeProp);
		if (!CCConstants.CCM_VALUE_MAP_TYPE_USERINBOX.equals(mapType) && !CCConstants.CCM_VALUE_MAP_TYPE_USERSAVEDSEARCH.equals(mapType)) {
			return false;
		}
		List<ChildAssociationRef> existing = nodeService.getChildAssocsByPropertyValue(
				new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, destinationParentId), mapTypeProp, mapType);
		if (existing != null && !existing.isEmpty()) {
			logger.info("restoring " + archivedNodeRef + " as regular folder, " + mapType + " already exists in " + destinationParentId);
			nodeService.removeProperty(archivedNodeRef, mapTypeProp);
			return false;
		}
		return true;
	}

}
