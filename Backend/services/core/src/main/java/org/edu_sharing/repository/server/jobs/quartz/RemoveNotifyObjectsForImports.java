package org.edu_sharing.repository.server.jobs.quartz;

import java.util.Collections;
import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.jobs.helper.NodeRunner;
import org.edu_sharing.repository.server.jobs.helper.NodeRunner.TransactionMode;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

public class RemoveNotifyObjectsForImports extends AbstractJob {
	
	NodeService nodeService;
	
	public static String PARAM_START_FOLDER = "START_FOLDER";
	
	
	Logger logger = Logger.getLogger(RemoveNotifyObjectsForImports.class);
	
	@Override
	public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
		ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();

		ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);

		nodeService = serviceRegistry.getNodeService();
		
		String startFolder = (String)jobExecutionContext.getJobDetail().getJobDataMap().get(PARAM_START_FOLDER);
		
		if(startFolder == null || startFolder.trim().equals("")) {
			logger.error("no start folder provided");
			return;
		}
		
		NodeRunner runner = new NodeRunner();
		runner.setTask((ref)->{
			org.alfresco.service.cmr.repository.NodeRef nodeRef = new org.alfresco.service.cmr.repository.NodeRef(ref.getStoreRef(), ref.getId());
			String name = (String)nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CM_NAME));
			List<ChildAssociationRef> parents = nodeService.getParentAssocs(nodeRef);
			for(ChildAssociationRef parentAssocRef : parents) {
				QName parentType = nodeService.getType(parentAssocRef.getParentRef());
				if(QName.createQName(CCConstants.CCM_TYPE_NOTIFY).equals(parentType)) {
					
					logger.info("delete Notify for: " + name + " nodeRef:" + nodeRef +" notfiyNoeRef:" +parentAssocRef.getParentRef());
					
					nodeService.addAspect(parentAssocRef.getParentRef(), ContentModel.ASPECT_TEMPORARY, null);
					nodeService.deleteNode(parentAssocRef.getParentRef());
				}
			}
		});
		runner.setTypes(Collections.singletonList(CCConstants.CCM_TYPE_IO));
		runner.setStartFolder(startFolder);
		runner.setRunAsSystem(true);
		runner.setTransaction(TransactionMode.Local);
		runner.setThreaded(false);
		runner.setKeepModifiedDate(true);
		
		int count=runner.run();
		logger.info("Processed "+count+" nodes");
	}
	
	@Override
	public Class[] getJobClasses() {
		this.addJobClass(RemoveNotifyObjectsForImports.class);
		return this.allJobs;
	}
	

}
