package org.edu_sharing.repository.update;

import java.io.PrintWriter;
import java.util.HashMap;

import javax.transaction.UserTransaction;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.springframework.context.ApplicationContext;

public class Release_3_2_PermissionInheritFalse implements Update {

	public static String ID = "Release_3_2_PermissionInheritFalse";
	
	public static String description = "sets inherit to false on User Homes";
	
	ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
	ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);

	Logger logger = Logger.getLogger(Release_3_2_FillOriginalId.class);
	
	PrintWriter out;
	
	public Release_3_2_PermissionInheritFalse(PrintWriter out) {
		this.out = out;
	}
	
	public void execute() {
		run(false);
	};
	
	@Override
	public void test() {
		run(true);
	}
	
	public String getDescription() {
		return Release_3_2_PermissionInheritFalse.description;
	};
	
	public String getId() {
		return Release_3_2_PermissionInheritFalse.ID;
	};
	
	void run(boolean test){
		
		UserTransaction transaction = serviceRegistry.getTransactionService().getNonPropagatingUserTransaction();
		try{
		    transaction.begin();
		    
			Protocol protocol = new Protocol();
			HashMap<String,Object> updateInfo = protocol.getSysUpdateEntry(this.getId());
			if(updateInfo == null){
				NodeRef nodeRefUserHome = new MCAlfrescoAPIClient().getUserHomesNodeRef(MCAlfrescoAPIClient.storeRef);
				serviceRegistry.getPermissionService().setInheritParentPermissions(nodeRefUserHome, false);
				if(!test){
					protocol.writeSysUpdateEntry(getId());
				}
			}else{
				if(this.out != null) logger.debug("Updater "+this.getId() + " already done");
				logger.debug("Updater "+this.getId() + " already done");
			}
			transaction.commit();
			
		}catch(Throwable e){
			if(this.out != null) this.out.println(e.getMessage());
			logger.error(e.getMessage(),e);
			
			try{
				transaction.rollback();
			}catch(Exception e2){
				logger.error(e.getMessage(),e2);
			}
		}
	
	}
}
