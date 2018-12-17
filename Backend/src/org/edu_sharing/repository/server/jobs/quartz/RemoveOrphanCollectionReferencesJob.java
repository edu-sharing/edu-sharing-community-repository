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
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.repository.server.importer.PersistentHandlerEdusharing;
import org.edu_sharing.service.model.NodeRef;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.service.search.model.SearchToken;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;


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
			SearchToken token=new SearchToken();
			token.setLuceneString("ASPECT:\"ccm:collection_io_reference\"");
			token.setMaxResult(Integer.MAX_VALUE);
			token.setContentType(SearchService.ContentType.FILES);
			SearchResultNodeRef result = searchService.search(token);
			int deleted=0;
			for(NodeRef node : result.getData()){
				String ref=nodeService.getProperty(node.getStoreProtocol(),node.getStoreId(),node.getNodeId(),CCConstants.CCM_PROP_IO_ORIGINAL);
				if(ref!=null && !nodeService.exists(node.getStoreProtocol(),node.getStoreId(),ref)){
					logger.info("Found orphan reference "+node.getNodeId()+", removing it");
					// parent=false == primary
					nodeService.removeNode(node.getNodeId(),null,false);
					deleted++;
				}
			}
			logger.info("RemoveOrphanCollectionReferencesJob finished, processed "+result.getData().size()+" references, deleted "+deleted);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public Class[] getJobClasses() {
		// TODO Auto-generated method stub
		return allJobs;
	}
}
