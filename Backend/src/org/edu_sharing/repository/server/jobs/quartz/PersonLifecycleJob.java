package org.edu_sharing.repository.server.jobs.quartz;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.QName;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

public class PersonLifecycleJob extends AbstractJob{

	ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
	ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
	
	SearchService searchService = serviceRegistry.getSearchService();
	
	NodeService nodeService = serviceRegistry.getNodeService();
	
	PersonService personService = serviceRegistry.getPersonService();
	
	int maxItems = 20;
	
	public static String PERSON_STATUS_ACTIVE = "active";
	
	public static String PERSON_STATUS_BLOCKED = "blocked";
	
	public static String PERSON_STATUS_DEACTIVATED = "deactivated";
	
	public static String PERSON_STATUS_TODELETE = "todelete";
	
	
	/**
	 * Konzept LöschJob -> status todelete
	 * 
	 * Filter in personsearch (invite, workflow - non active)
	 * 
	 * validate session only active
	 * 
	 * 
	 */
	
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		int skipCount = 0;
		
		SearchParameters sp = new SearchParameters();
		sp.setQuery("TYPE:\"cm:person\"");
		sp.setSkipCount(skipCount);
		sp.setMaxItems(maxItems);
		ResultSet rs = searchService.query(sp);
		for(NodeRef nodeRef : rs.getNodeRefs()) {
			String status = (String)nodeService.getProperty(nodeRef, 
					QName.createQName(CCConstants.CM_PROP_PERSON_ESPERSONSTATUS));
			if(status != null) {
				
			}
		}
		
	}
	
	@Override
	public Class[] getJobClasses() {
		// TODO Auto-generated method stub
		return null;
	}
}
