package org.edu_sharing.repository.update;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.springframework.context.ApplicationContext;

public class Edu_SharingPersonEsuidUpdate extends UpdateAbstract {

	ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
	ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
	
	NodeService nodeService = null;
	PersonService personService = null;
	
	public static final String ID = "Edu_SharingPersonEsuidUpdate";
	
	public static final String description = "Creates esuids for all persons." ;
	
	public Edu_SharingPersonEsuidUpdate(PrintWriter out) {
		this.out = out;
		logger = Logger.getLogger(Edu_SharingPersonEsuidUpdate.class);
	}
	
	@Override
	public void execute() {
		logInfo("starting excecute");
		doIt(false);
		logInfo("finished excecute");
	}

	@Override
	public void test() {
		logInfo("starting test");
		doIt(true);
		logInfo("finished test");
	}
	
	public void doIt(boolean test){
		
		/**
		 * create all person folders
		 */
		
		//do it here cause authentication is passed here instead of constructor
		nodeService = serviceRegistry.getNodeService();
		personService = serviceRegistry.getPersonService();
		
		try{
		
			Protocol protocol = new Protocol();
			HashMap<String,Object> updateInfo = protocol.getSysUpdateEntry(this.getId());
			if(updateInfo == null){
				createEsUids(test);
				if(!test){
					protocol.writeSysUpdateEntry(this.getId());
				}
			}else{
				logInfo("update" +this.getId()+ " already done at "+updateInfo.get(CCConstants.CCM_PROP_SYSUPDATE_DATE));
			}
			
		}catch(Throwable e){
			logError(e.getMessage(),e);
		}
		
	}

	public void createEsUids(boolean test){
		
		Set<NodeRef> allPeople = personService.getAllPeople();
		int counter = 0;
		for(NodeRef personRef : allPeople){
			
			if(!test){
				
				//nodeService.setProperty(personRef, ContentModel.PROP_FIRSTNAME, nodeService.getProperty(personRef, ContentModel.PROP_FIRSTNAME));
				
				UUID uuid = UUID.randomUUID();
				QName esUidQName =  QName.createQName(CCConstants.PROP_USER_ESUID);
				if(nodeService.getProperty(personRef, esUidQName) == null){
					nodeService.setProperty(personRef, esUidQName, uuid.toString());
				}
				
			}
			counter++;
			if((counter % 100) == 0){
				logger.info("processed "+ counter +" persons");
			}
		}
	}
	
	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getDescription() {
		return description;
	}
	
	@Override
	public void run() {
		this.logInfo("not implemented");
	}

}
