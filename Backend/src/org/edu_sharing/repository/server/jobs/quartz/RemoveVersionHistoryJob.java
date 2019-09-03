package org.edu_sharing.repository.server.jobs.quartz;

import org.alfresco.model.ContentModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.jobs.helper.NodeRunner;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

public class RemoveVersionHistoryJob extends AbstractJob {

	
	NodeService nodeService;
	VersionService versionService;
	
	public static String PARAM_START_FOLDER = "START_FOLDER";
	
	Logger logger = Logger.getLogger(RemoveVersionHistoryJob.class);
	
	@Override
	public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
		ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();

		ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);

		nodeService = serviceRegistry.getNodeService();
		versionService = serviceRegistry.getVersionService();
		
		String startFolder = (String)jobExecutionContext.get(PARAM_START_FOLDER);
		
		if(startFolder == null || startFolder.trim().equals("")) {
			logger.error("no start folder provided");
			return;
		}
		
		logger.info("removing version history from start folder: " + nodeService.getProperty(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,startFolder ),ContentModel.PROP_NAME));
		
		NodeRunner runner = new NodeRunner();
		runner.setTask((ref)->{
			org.alfresco.service.cmr.repository.NodeRef nodeRef = new org.alfresco.service.cmr.repository.NodeRef(ref.getStoreRef(), ref.getId());
			QName type = nodeService.getType(nodeRef);
			String name = (String)nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CM_NAME));
			logger.info("delete VersionHistory for: " + name + " nodeRef:" + nodeRef);
			if(type.equals(QName.createQName(CCConstants.CCM_TYPE_IO))) {
				versionService.deleteVersionHistory(nodeRef);
			}
		});
		runner.setStartFolder(startFolder);

	}
	
	@Override
	public Class[] getJobClasses() {
		this.addJobClass(RemoveVersionHistoryJob.class);
		return this.allJobs;
	}
	
	
}
