package org.edu_sharing.repository.server.tools.cache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.alfresco.model.ContentModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.MCBaseClient;
import org.edu_sharing.repository.server.RepoFactory;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.Constants;
import org.springframework.context.ApplicationContext;

public class RepositoryCacheTool {

	private static Log logger = LogFactory.getLog(RepositoryCacheTool.class);
	
	ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
	
	ServiceRegistry sr = (ServiceRegistry)applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
	NodeService nodeService = sr.getNodeService();
	
	/**
	 * creates a new cache object and puts all subobjects of rootfolderId.
	 * when it's finished the new cache object becomes the current cache object
	 * 
	 * usefull when a cache is already filled but must be refreshed
	 * 
	 * @param authenticationInfo
	 * @param rootfolderId
	 * @throws Throwable
	 */
	public void buildNewCache(HashMap authenticationInfo, String rootfolderId) throws Throwable{
		Map<String,Map<String,Object>> newCache = new ConcurrentHashMap<String,Map<String,Object>>();
		
		MCBaseClient mcBaseClient = RepoFactory.getInstance(ApplicationInfoList.getHomeRepository().getAppId(), authenticationInfo);
		if(mcBaseClient instanceof MCAlfrescoAPIClient){
			MCAlfrescoAPIClient apiClient = (MCAlfrescoAPIClient)mcBaseClient;
			long startMillies = System.currentTimeMillis();
			logger.info("starting getChildrenRecursive");
			HashMap<String, HashMap<String, Object>> childRecursive = apiClient.getChildrenRecursive(MCAlfrescoAPIClient.storeRef, rootfolderId, CCConstants.CCM_TYPE_IO, null, false);
			logger.info("getChildrenRecursive returned.starting to copy to cachemap. size:"+childRecursive.size());
			for(Map.Entry<String, HashMap<String, Object>> entry : childRecursive.entrySet()){
				newCache.put(entry.getKey(), entry.getValue());
			}
			
			long endMillies = System.currentTimeMillis();
			long diff = (endMillies - startMillies)/ 1000; 
			long diffMinutes = diff / 60; 
			logger.info("copy to cachemap finished in seconds:"+diff+" around "+diffMinutes+" minutes" +  " size:" + newCache.size()+" linking the new cache to the cache reference");
			
			RepositoryCache.setCache(newCache);
			//clear facette cache
			FacetteCache.getFacetteCache().clear();
			logger.info("Facettes cleared");
		}else{
			logger.error("cache rebuilding is only available for MCAlfrescoAPIClient");
		}
	}
	
	private static final int THREAD_COUNT = Math.max(1, Math.min(6,Runtime.getRuntime().availableProcessors()-1));
	private ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT, r -> {
		Thread t = new Thread(r);
		t.setPriority(Thread.NORM_PRIORITY-1);
		return t;
	});
	
	/**
	 * builds cache directly to the current cache object, those Objects that are already in cache will not be refreshed
	 * 
	 * usefull after a server reboot cause it's adjuvant to the cachbuilding by user actions
	 * @param authenticationInfo
	 * @param rootfolderId
	 */
	public void buildStickyCache(HashMap authenticationInfo, String rootfolderId) throws Throwable{
		MCBaseClient mcBaseClient = RepoFactory.getInstance(ApplicationInfoList.getHomeRepository().getAppId(), authenticationInfo);
		if(mcBaseClient instanceof MCAlfrescoAPIClient){
	
			logger.info("preparing first level subfolder for threading");
			List<ChildAssociationRef> childAssocRefs = nodeService.getChildAssocs(new NodeRef(Constants.storeRef,rootfolderId));
			
			
			int i = 0;
			
			List<Callable<Void>> threads = new ArrayList<>();
			
			List<ChildAssociationRef> childRefsThread = new ArrayList<ChildAssociationRef>();
			
			long time=System.currentTimeMillis();
			for(ChildAssociationRef childAssocRef : childAssocRefs) {
				
				childRefsThread.add(childAssocRef);
				if(i % 10 == 0) {
					threads.add(() ->{
						List<ChildAssociationRef> childRefPage = new ArrayList<>(childRefsThread);
						childRefsThread.clear();
						
						MCAlfrescoAPIClient apiClient = (MCAlfrescoAPIClient)mcBaseClient;
						for(ChildAssociationRef cr : childRefPage ) {
							long startMillies = System.currentTimeMillis();
							logger.info("starting getChildrenRecursive for folder:" + nodeService.getProperty(cr.getChildRef(), ContentModel.PROP_NAME));
							try {
								HashMap<String, HashMap<String, Object>> childRecursive = apiClient.getChildrenRecursive(MCAlfrescoAPIClient.storeRef, rootfolderId, CCConstants.CCM_TYPE_IO, null, true);
							} catch (Throwable e) {
								logger.error(e.getMessage(), e);
							}
							long endMillies = System.currentTimeMillis();
							long diff = (endMillies - startMillies)/ 1000; 
							long diffMinutes = diff / 60; 
						}
						
						return null;
					});
				}
					
				i++;
			}
			
			executor.invokeAll(threads);
			time=(System.currentTimeMillis()-time);
			if(threads.size()>0) {
				logger.info(THREAD_COUNT + " Threads finished (" + threads.size() + ", " + (time / 1000) + " s -> " + (time / threads.size()) + "ms per entry)");
			}
			
			FacetteCache.getFacetteCache().clear();
			logger.info("Facettes cleared");
			
			
			/*MCAlfrescoAPIClient apiClient = (MCAlfrescoAPIClient)mcBaseClient;
			long startMillies = System.currentTimeMillis();
			logger.info("starting getChildrenRecursive");
			HashMap<String, HashMap<String, Object>> childRecursive = apiClient.getChildrenRecursive(MCAlfrescoAPIClient.storeRef, rootfolderId, CCConstants.CCM_TYPE_IO, null, true);
			long endMillies = System.currentTimeMillis();
			long diff = (endMillies - startMillies)/ 1000; 
			long diffMinutes = diff / 60; 
			logger.info("getChildrenRecursive returned. buildStickyCache finished in seconds:"+diff+" around "+diffMinutes+" minutes" +  " size:" + childRecursive.size()+".");
			FacetteCache.getFacetteCache().clear();
			logger.info("Facettes cleared");*/
			
			
		}
	}
	
}
