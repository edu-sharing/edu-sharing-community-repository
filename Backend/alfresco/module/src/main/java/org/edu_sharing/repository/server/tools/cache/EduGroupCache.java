package org.edu_sharing.repository.server.tools.cache;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.cache.SimpleCache;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;



public class EduGroupCache {

	public static Logger logger = Logger.getLogger(EduGroupCache.class);
	
	/**
	 * Administration by:
	 * org.edu_sharing.repository.server.tools.EduGroupTool: put when aspect added, remove when aspect removed
	 * org.edu_sharing.alfresco.policy.BeforeEduGroupDeletePolicy remove when edugroup will be deleted
	 * org.edu_sharing.repository.server.MCAlfrescoManager: init when webapp is started
	 * 
	 */
	

	public static ServiceRegistry serviceRegistry = (ServiceRegistry)AlfAppContextGate.getApplicationContext().getBean(ServiceRegistry.SERVICE_REGISTRY);
	
			// Shared Cache over EHCache
	final static SimpleCache<NodeRef,Map<QName,Serializable>> cache = (SimpleCache<NodeRef,Map<QName,Serializable>>)  AlfAppContextGate.getApplicationContext().getBean("eduSharingEduGroupCache");

	
	public static void put(NodeRef nodeRef, Map<QName,Serializable> props){
		
		synchronized(EduGroupCache.cache){
			EduGroupCache.cache.put(nodeRef, props);
		}
		
	}

	public static Map<QName,Serializable> get(NodeRef nodeRef) {
		return EduGroupCache.cache.get(nodeRef);
	}

	public static void remove(NodeRef nodeRef) {
		synchronized(EduGroupCache.cache){
			EduGroupCache.cache.remove(nodeRef);
		}		
	}
	
	public static Collection<NodeRef> getKeys(){
		return EduGroupCache.cache.getKeys();
	}
	
	public static String[] getNames(){
		ArrayList<String> names = new ArrayList<String>();
		for(NodeRef nodeRef : cache.getKeys()){
			names.add((String)cache.get(nodeRef).get(ContentModel.PROP_AUTHORITY_NAME));
		}
		return names.toArray(new String[names.size()]);
	}
	
	public static List<NodeRef> getAllEduGoupFolder(){
		List<NodeRef> result = new ArrayList<NodeRef>();
		for(NodeRef groupNodeRef : EduGroupCache.cache.getKeys()){
			result.add((NodeRef)EduGroupCache.cache.get(groupNodeRef).get(QName.createQName(CCConstants.CCM_PROP_EDUGROUP_EDU_HOMEDIR)));
		}
		return result;
	}
	
	/**
	 * 
	 * @return Map keys: EduGroup HomeFolder NodeRef, Values EduGroup Properties 
	 */
	public static Map<NodeRef,Map<QName,Serializable>> getAllEduGroupFolderAndEduGroupProps(){
		Map<NodeRef,Map<QName,Serializable>> result = new HashMap<NodeRef,Map<QName,Serializable>>();
		for(NodeRef groupNodeRef : EduGroupCache.cache.getKeys()){

			NodeRef homeDirNodeRef = (NodeRef)EduGroupCache.cache.get(groupNodeRef).get(QName.createQName(CCConstants.CCM_PROP_EDUGROUP_EDU_HOMEDIR));
			if(homeDirNodeRef != null) {
				result.put(homeDirNodeRef,
						EduGroupCache.cache.get(groupNodeRef));
			}else{
				logger.error("homeDirNodeRef is null for " + EduGroupCache.cache.get(groupNodeRef).get(ContentModel.PROP_AUTHORITY_NAME));
			}
		}
		return result;
	}
	
	protected static void setCache(Map<NodeRef,Map<QName,Serializable>> cache) {
		synchronized(EduGroupCache.cache){
			EduGroupCache.cache.clear();
			for(Map.Entry<NodeRef,Map<QName,Serializable>> entry : cache.entrySet()){
				EduGroupCache.cache.put(entry.getKey(), entry.getValue());
			}
		}
	}
	
	public static void refresh(){
		synchronized(EduGroupCache.cache){
			logger.info("size before refresh:"+EduGroupCache.cache.getKeys().size());
			EduGroupCache.cache.clear();
			for(NodeRef eduGroupNodeRef : getEduGroupNodeRefs()){
				
				EduGroupCache.cache.put(eduGroupNodeRef, serviceRegistry.getNodeService().getProperties(eduGroupNodeRef));
			}
			logger.info("size after refresh:"+EduGroupCache.cache.getKeys().size());
		}		
	}
	
	public static void refreshByKeepExisting(){
		synchronized(EduGroupCache.cache){
			logger.info("size before refresh:"+EduGroupCache.cache.getKeys().size());
			//EduGroupCache.cache.clear();
			for(NodeRef eduGroupNodeRef : getEduGroupNodeRefs()){
				if(!EduGroupCache.cache.contains(eduGroupNodeRef)) {
					EduGroupCache.cache.put(eduGroupNodeRef, serviceRegistry.getNodeService().getProperties(eduGroupNodeRef));
				}
			}
			logger.info("size after refresh:"+EduGroupCache.cache.getKeys().size());
		}		
	}
	
	private static List<NodeRef> getEduGroupNodeRefs2(){
		List<NodeRef> result = new ArrayList<NodeRef>();
		NodeRef rootNode = serviceRegistry.getNodeService().getRootNode(new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore"));
		List<ChildAssociationRef> rootChildAssocs = serviceRegistry.getNodeService().getChildAssocs(rootNode);
		for(ChildAssociationRef childAssocRef : rootChildAssocs){
			
			if(childAssocRef.getQName().equals(QName.createQName("{"+NamespaceService.SYSTEM_MODEL_1_0_URI+"}system"))){
				
				List<ChildAssociationRef> systemChildren =  serviceRegistry.getNodeService().getChildAssocs(childAssocRef.getChildRef());
				
				for(ChildAssociationRef sysChild : systemChildren){
					if(sysChild.getQName().equals(QName.createQName("{"+NamespaceService.SYSTEM_MODEL_1_0_URI+"}authorities"))){
						List<ChildAssociationRef> authorities = serviceRegistry.getNodeService().getChildAssocs(sysChild.getChildRef());
						
						for(ChildAssociationRef authorityChild :authorities ){
							if(serviceRegistry.getNodeService().hasAspect(authorityChild.getChildRef(), QName.createQName(CCConstants.CCM_ASPECT_EDUGROUP))){
								result.add(authorityChild.getChildRef());
							}
						}
						
						
					}
				}
				
				
			}
		}
		return result;
		
	}
	
	private static List<NodeRef> getEduGroupNodeRefs(){
		logger.info("starting");
		AuthorityService authorityService =serviceRegistry.getAuthorityService();
		NodeService nodeService = serviceRegistry.getNodeService();
		List<NodeRef> result = new ArrayList<NodeRef>();
		Set<String> allGroups = authorityService.getAllAuthoritiesInZone(AuthorityService.ZONE_APP_DEFAULT, AuthorityType.GROUP);
		for(String authority : allGroups) {
			NodeRef authorityNodeRef = authorityService.getAuthorityNodeRef(authority);
			if(nodeService.hasAspect(authorityNodeRef, QName.createQName(CCConstants.CCM_ASPECT_EDUGROUP))) {
				result.add(authorityNodeRef);
			}
		}
		logger.info("found groupCount: " + allGroups.size() + " eduGroupCount:" + result.size() );
		return result;
		
	}
	
}
