/**
 *
 *  
 * 
 * 
 *	
 *
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 *
 */
package org.edu_sharing.repository.server.jobs.quartz;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.jobs.helper.NodeRunner;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.restservices.shared.Node;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.nodeservice.RecurseMode;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Remove orphan usages pointing to non-existing collection references
 * Required parameters:
 * testRun: Only test/output
 */
public class RemoveOrphanCollectionUsageDescriptions extends AbstractJob{

	protected Logger logger = Logger.getLogger(RemoveOrphanCollectionUsageDescriptions.class);
	private List<String> types;
	private NodeService nodeService;

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();

		ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);

		nodeService = serviceRegistry.getNodeService();
		org.edu_sharing.service.nodeservice.NodeService nodeServiceEdu = NodeServiceFactory.getLocalService();

		Object testRunStr = context.getJobDetail().getJobDataMap().get("testRun");
		if(testRunStr==null){
			throw new IllegalArgumentException("Missing required boolean parameter 'testRun'");
		}
		boolean testRun = Boolean.parseBoolean(testRunStr.toString());

		String localApp = ApplicationInfoList.getHomeRepository().getAppId();
		NodeRunner runner = new NodeRunner();
		runner.setTask((ref)->{
			String appId = (String) NodeServiceHelper.getPropertyNative(ref, CCConstants.CCM_PROP_USAGE_APPID);
			if(localApp.equals(appId) || isNodeCollection((String) NodeServiceHelper.getPropertyNative(ref, CCConstants.CCM_PROP_USAGE_COURSEID))){
				String refNodeId = NodeServiceHelper.getProperty(ref, CCConstants.CCM_PROP_USAGE_RESSOURCEID);
				if(!nodeService.exists(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, refNodeId))) {
					logger.info("Delete orphan usage from node: " + nodeService.getPrimaryParent(ref).getParentRef().getId() +
							" Target Ref id: " + refNodeId +
							" Collection id: " + NodeServiceHelper.getPropertyNative(ref, CCConstants.CCM_PROP_USAGE_COURSEID)
					);
					if(!testRun) {
						nodeServiceEdu.removeNode(ref.getId(), null, false);
					}
				}
			}
		});
		runner.setTypes(Collections.singletonList(CCConstants.CCM_TYPE_USAGE));
		runner.setRunAsSystem(true);
		runner.setRecurseMode(RecurseMode.All);
		runner.setThreaded(false);
		runner.setKeepModifiedDate(true);
		runner.setTransaction(NodeRunner.TransactionMode.Local);
		int count=runner.run();
		logger.info("Processed "+count+" nodes");
	}

	private boolean isNodeCollection(String ref) {
		NodeRef nodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, ref);
		return nodeService.exists(nodeRef) && nodeService.hasAspect(nodeRef, QName.createQName(CCConstants.CCM_ASPECT_COLLECTION));
	}

	public void run() {

	}
	
	@Override
	public Class[] getJobClasses() {
		// TODO Auto-generated method stub
		return allJobs;
	}
}
