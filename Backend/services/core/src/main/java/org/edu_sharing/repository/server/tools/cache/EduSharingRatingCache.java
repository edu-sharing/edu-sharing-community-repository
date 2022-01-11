package org.edu_sharing.repository.server.tools.cache;

import org.alfresco.repo.cache.SimpleCache;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.service.rating.RatingsCache;


public class EduSharingRatingCache {

	public static Logger logger = Logger.getLogger(EduSharingRatingCache.class);

	public static ServiceRegistry serviceRegistry = (ServiceRegistry)AlfAppContextGate.getApplicationContext().getBean(ServiceRegistry.SERVICE_REGISTRY);
			// Shared Cache over EHCache
	final static SimpleCache<NodeRef, RatingsCache> cache = (SimpleCache<NodeRef, RatingsCache>) AlfAppContextGate.getApplicationContext().getBean("eduSharingRatingCache");

	public static void put(NodeRef nodeRef, RatingsCache rating){
		
		synchronized(EduSharingRatingCache.cache){
			EduSharingRatingCache.cache.put(nodeRef, rating);
		}
		
	}

	public static RatingsCache get(NodeRef nodeRef) {
		return EduSharingRatingCache.cache.get(nodeRef);
	}

	public static void delete(NodeRef nodeRef) {
		synchronized(EduSharingRatingCache.cache){
			EduSharingRatingCache.cache.remove(nodeRef);
		}
	}
}
