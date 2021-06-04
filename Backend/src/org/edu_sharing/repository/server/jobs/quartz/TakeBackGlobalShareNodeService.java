package org.edu_sharing.repository.server.jobs.quartz;

import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

public class TakeBackGlobalShareNodeService extends AbstractJob {

	ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
	ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
	private NodeService nodeService = serviceRegistry.getNodeService();
	private PermissionService permissionService = serviceRegistry.getPermissionService();
	Repository repositoryHelper = (Repository) applicationContext.getBean("repositoryHelper");

	
	Logger logger = Logger.getLogger(TakeBackGlobalShare.class);
	
	public static String PARAM_EXECUTE = "EXECUTE";
	
	public static String PARAM_START_FOLDER = "START_FOLDER";
	
	TakeBackGlobalShareWorker worker;
	
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		boolean execute = new Boolean((String)context.getJobDetail().getJobDataMap().get(PARAM_EXECUTE));
		String startFolder = (String)context.getJobDetail().getJobDataMap().get(PARAM_START_FOLDER);
		worker = new TakeBackGlobalShareWorker(nodeService, permissionService, execute);
		AuthenticationUtil.RunAsWork<Void> runAs = new AuthenticationUtil.RunAsWork<Void>() {
			@Override
			public Void doWork() throws Exception {
				run(execute, startFolder);
				return null;
			}
		};
		AuthenticationUtil.runAsSystem(runAs);
	}
	
	public void run(boolean execute, String startFolder) {
		NodeRef nodeRefStartFolder = 
				(startFolder != null && !startFolder.trim().equals("")) 
				? new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,startFolder) 
						: repositoryHelper.getCompanyHome(); 
		work(nodeRefStartFolder);
	}
	
	private void work(NodeRef nodeRef) {
		QName type = nodeService.getType(nodeRef);
		worker.work(nodeRef);
		if(type.equals(ContentModel.TYPE_FOLDER) 
				|| type.equals(QName.createQName(CCConstants.CCM_TYPE_MAP))) {
			
			String mapType = (String) nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_MAP_TYPE));
			if(CCConstants.CCM_VALUE_MAP_TYPE_EDUGROUP.equals(mapType)) {
				return;
			}
			String name = (String)nodeService.getProperty(nodeRef, ContentModel.PROP_NAME);
			logger.info("opening folder:" + nodeService.getPath(nodeRef).toDisplayPath(nodeService, permissionService) + " /" + name);
			
			List<ChildAssociationRef> childRefs = nodeService.getChildAssocs(nodeRef);
			for(ChildAssociationRef childRef : childRefs) {
				this.work(childRef.getChildRef());
			}
		}
	}

	@Override
	public Class[] getJobClasses() {
		this.addJobClass(TakeBackGlobalShareNodeService.class);
		return this.allJobs;
	}

}
