package org.edu_sharing.repository.update;

import java.io.PrintWriter;
import java.util.List;

import javax.transaction.UserTransaction;

import org.alfresco.model.ContentModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.springframework.context.ApplicationContext;

public class FolderToMap extends UpdateAbstract{
	
	public static final String ID = "Release_1_8_FolderToMapBugfix";

	public static final String description = "folder created over webdav where not transformed to map. this is fixed. use this updater to transform folders starting from a specified root folder.";
	
	ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
	ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);

	NodeService nodeService = null;
	
	int counter = 0;
	
	
	public FolderToMap(PrintWriter out) {
		this.out = out;
		logger = Logger.getLogger(FolderToMap.class);
	}
	
	@Override
	public void execute() {
		run(false);
	}
	
	@Override
	public String getDescription() {
		return description;
	}
	
	public String getId() {
		return ID;
	};
	
	@Override
	public void test() {
		run(true);
	}
	
	private void run(boolean test){
		
		counter = 0;
		//very important: use the alfrescoDefaultDbNodeService defined in custom-core-services-context.xml
		//cause of overwriten getChild... methods in org.edu_sharing.alfresco.fixes.DbNodeServiceImpl
		//this can lead to a problem, that every edugroupfolder is processed for all members of the edugroup again
		nodeService = (NodeService)applicationContext.getBean("alfrescoDefaultDbNodeService");
		
		String rootNode = Context.getCurrentInstance().getRequest().getParameter("root");
		
		if(rootNode == null || rootNode.trim().equals("")){
			String message = "Missing parameter \"root\"";
			RuntimeException e = new RuntimeException(message);
			logError(message, e);
			return;
		}
		
		NodeRef nodeRef = new NodeRef(MCAlfrescoAPIClient.storeRef,rootNode);
		
		if(!nodeService.exists(nodeRef)){
			String message = "node with id "+rootNode+" does not exsist!";
			RuntimeException e = new RuntimeException(message);
			logError(message, e);
			return;
		}
		
		UserTransaction userTransaction = serviceRegistry.getTransactionService().getNonPropagatingUserTransaction();
		
		Protocol protocol = new Protocol();
		
		try{
			
			if(!test) userTransaction.begin();
			
			transformLevel(nodeRef, test);
			
			logInfo("finished. transformed folders:"+counter);
				
			if(!test){
				protocol.writeSysUpdateEntry(this.getId());
			}
				
			if(!test) userTransaction.commit();
		
		} catch(Throwable e) {
			
			logError(e.getMessage(),e);
			
			try{
				logInfo("trying rollback");
				userTransaction.rollback();
			}catch(Exception rollBackException){
				logError(rollBackException.getMessage(),rollBackException);
			}
		}
	}
	
	private void transformLevel(NodeRef parent,boolean test){
		
		List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(parent);
		for(ChildAssociationRef childAssoc : childAssocs){
			
			//transform current folder
			if(nodeService.getType(childAssoc.getChildRef()).equals(ContentModel.TYPE_FOLDER)){
				
				String currentName = (String)nodeService.getProperty(childAssoc.getChildRef(), ContentModel.PROP_NAME);
				
				logInfo("transform folder to map. Path:"+nodeService.getPath(childAssoc.getChildRef()).toDisplayPath(nodeService, serviceRegistry.getPermissionService()) +" n:"+currentName);
				if(!test){
					nodeService.setType(childAssoc.getChildRef(), QName.createQName(CCConstants.CCM_TYPE_MAP));
					nodeService.setProperty(childAssoc.getChildRef(), ContentModel.PROP_TITLE, currentName);
				}
				counter++;
			}
			
			//transform current children
			if(nodeService.getType(childAssoc.getChildRef()).equals(QName.createQName(CCConstants.CCM_TYPE_MAP)) ){
				transformLevel(childAssoc.getChildRef(), test);
			}
			
		}
	}
	
	@Override
	public void run() {
		this.logInfo("not implemented");
	}
	
}
