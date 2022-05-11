package org.edu_sharing.repository.server.jobs.quartz;

import java.util.Collections;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
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
import org.edu_sharing.repository.server.jobs.helper.NodeRunner.TransactionMode;
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
		
		String startFolder = (String)jobExecutionContext.getJobDetail().getJobDataMap().get(PARAM_START_FOLDER);
		
		if(startFolder == null || startFolder.trim().equals("")) {
			logger.error("no start folder provided");
			return;
		}
		
		
		logger.info("removing version history from start folder: " + startFolder );
		
		NodeRunner runner = new NodeRunner();
		runner.setTask((ref)->{
			org.alfresco.service.cmr.repository.NodeRef nodeRef = new org.alfresco.service.cmr.repository.NodeRef(ref.getStoreRef(), ref.getId());
			String name = (String)nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CM_NAME));
			logger.info("delete VersionHistory for: " + name + " nodeRef:" + nodeRef);
			versionService.deleteVersionHistory(nodeRef);
			
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
		this.addJobClass(RemoveVersionHistoryJob.class);
		return this.allJobs;
	}
	
	
}
