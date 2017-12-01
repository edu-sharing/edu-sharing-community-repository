package org.edu_sharing.repository.server.tools.cache;

import java.util.Collection;

import org.alfresco.repo.cache.SimpleCache;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.service.editlock.LockBy;

public class EditLockCache {
	
public static Logger logger = Logger.getLogger(EditLockCache.class);
	

	public static ServiceRegistry serviceRegistry = (ServiceRegistry)AlfAppContextGate.getApplicationContext().getBean(ServiceRegistry.SERVICE_REGISTRY);
	
			// Shared Cache over EHCache
	public final static SimpleCache<NodeRef,LockBy> cache = (SimpleCache<NodeRef,LockBy>)  AlfAppContextGate.getApplicationContext().getBean("eduSharingEditLockCache");

	
	public static void put(NodeRef nodeRef, LockBy lockBy){
		
		synchronized(EditLockCache.cache){
			EditLockCache.cache.put(nodeRef, lockBy);
		}
		
	}

	public static LockBy get(NodeRef nodeRef) {
		return EditLockCache.cache.get(nodeRef);
	}

	public static void remove(NodeRef nodeRef) {
		synchronized(EditLockCache.cache){
			EditLockCache.cache.remove(nodeRef);
		}		
	}
	
	public static Collection<NodeRef> getKeys(){
		return EditLockCache.cache.getKeys();
	}
	
}
