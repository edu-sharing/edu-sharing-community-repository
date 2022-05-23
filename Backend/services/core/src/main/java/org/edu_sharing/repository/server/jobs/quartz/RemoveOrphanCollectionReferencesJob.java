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
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.repository.StoreRef;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.repository.server.importer.PersistentHandlerEdusharing;
import org.edu_sharing.repository.server.jobs.helper.NodeRunner;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.service.model.NodeRef;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.service.search.model.SearchToken;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.Collections;

@JobDescription(description = "Remove all orphan collection references (with no valid original object)")
public class RemoveOrphanCollectionReferencesJob extends AbstractJob{

	private SearchService searchService=SearchServiceFactory.getLocalService();
	private NodeService nodeService=NodeServiceFactory.getLocalService();
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		
		
		String username = (String) context.getJobDetail().getJobDataMap().get(OAIConst.PARAM_USERNAME);
		
		RunAsWork<Void> runAs = new RunAsWork<Void>() {
			@Override
			public Void doWork() throws Exception {
				run();
				return null;
			}
		};
		
		AuthenticationUtil.runAs(runAs,username);
	
	}
	
	public void run() {
		try {
			final int[] deleted=new int[]{0};
			NodeRunner runner=new NodeRunner();
			runner.setTypes(Collections.singletonList(CCConstants.CCM_TYPE_IO));
			runner.setTransaction(NodeRunner.TransactionMode.Local);
			runner.setKeepModifiedDate(true);
			runner.setThreaded(false);
			runner.setFilter((node)->
				nodeService.hasAspect(node.getStoreRef().getProtocol(),node.getStoreRef().getIdentifier(),node.getId(),CCConstants.CCM_ASPECT_COLLECTION_IO_REFERENCE)
			);
			runner.setTask((node)->{
				String ref = nodeService.getProperty(node.getStoreRef().getProtocol(), node.getStoreRef().getIdentifier(), node.getId(), CCConstants.CCM_PROP_IO_ORIGINAL);
				if (!nodeService.exists(StoreRef.PROTOCOL_WORKSPACE,StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), ref)) {
					logger.info("Found orphan reference " + node.getId() + ", removing it");
					// parent=false == primary
					nodeService.removeNode(node.getId(), null, false);
					deleted[0]++;
				}
			});
			int count=runner.run();
			logger.info("RemoveOrphanCollectionReferencesJob finished, processed "+count+" references, deleted "+deleted);
		} catch (Throwable e) {
			logger.warn(e.getMessage(),e);
		}
	}
	
	@Override
	public Class[] getJobClasses() {
		// TODO Auto-generated method stub
		return allJobs;
	}
}
