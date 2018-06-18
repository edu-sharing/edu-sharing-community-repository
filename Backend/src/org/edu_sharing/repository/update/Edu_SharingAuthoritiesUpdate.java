package org.edu_sharing.repository.update;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Set;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.PersonService;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.policy.HomeFolderTool;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.springframework.context.ApplicationContext;


public class Edu_SharingAuthoritiesUpdate extends UpdateAbstract {
	
	ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
	ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
	
	NodeService nodeService = null;
	PersonService personService = null;
	
	public static final String ID = "Edu_SharingAuthoritiesUpdate";
	
	public static final String description = "Creates edu-sharing folders in exsisting userhomes." ;
	
	public Edu_SharingAuthoritiesUpdate(PrintWriter out) {
		this.out = out;
		logger = Logger.getLogger(Edu_SharingAuthoritiesUpdate.class);
	}
	
	@Override
	public void execute() {
		logDebug("starting excecute");
		doIt(false);
		logDebug("finished excecute");
	}

	@Override
	public void test() {
		logDebug("starting test");
		doIt(true);
		logDebug("finished test");
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
				createPersonFolders(test);
				if(!test){
					protocol.writeSysUpdateEntry(this.getId());
				}
			}else{
				logDebug("update" +this.getId()+ " already done at "+updateInfo.get(CCConstants.CCM_PROP_SYSUPDATE_DATE));
			}
			
		}catch(Throwable e){
			logError(e.getMessage(),e);
		}
		
	}
	
	public void createPersonFolders(boolean test){
		
		Set<NodeRef> allPeople = personService.getAllPeople();
		int counter = 0;
		for(NodeRef personRef : allPeople){
			
			HomeFolderTool hft = new HomeFolderTool(serviceRegistry);
			if(!test){
				hft.constructPersonFolders(personRef);
			}
			counter++;
			if((counter % 100) == 0){
				logger.debug("processed "+ counter +" persons");
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
