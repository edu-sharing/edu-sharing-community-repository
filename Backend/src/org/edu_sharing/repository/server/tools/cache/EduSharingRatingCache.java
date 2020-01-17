package org.edu_sharing.repository.server.tools.cache;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.cache.SimpleCache;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.rating.AccumulatedRatings;
import org.edu_sharing.service.rating.Rating;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;


public class EduSharingRatingCache {

	public static Logger logger = Logger.getLogger(EduSharingRatingCache.class);

	public static ServiceRegistry serviceRegistry = (ServiceRegistry)AlfAppContextGate.getApplicationContext().getBean(ServiceRegistry.SERVICE_REGISTRY);
			// Shared Cache over EHCache
	final static SimpleCache<NodeRef,AccumulatedRatings> cache = (SimpleCache<NodeRef, AccumulatedRatings>) AlfAppContextGate.getApplicationContext().getBean("eduSharingRatingCache");

	public static void put(NodeRef nodeRef, AccumulatedRatings rating){
		
		synchronized(EduSharingRatingCache.cache){
			EduSharingRatingCache.cache.put(nodeRef, rating);
		}
		
	}

	public static AccumulatedRatings get(NodeRef nodeRef) {
		return EduSharingRatingCache.cache.get(nodeRef);
	}

	public static void delete(NodeRef nodeRef) {
		synchronized(EduSharingRatingCache.cache){
			EduSharingRatingCache.cache.remove(nodeRef);
		}
	}
}
