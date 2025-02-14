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

import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.server.jobs.helper.NodeRunner;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.edu_sharing.service.archive.ArchiveService;
import org.edu_sharing.service.archive.ArchiveServiceFactory;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.Collections;

/**
 * Batch edit property for multiple nodes
 * Required parameters:
 * property: The property name to replace
 * value: the target value to set
 * OR copy: the source property to copy the value of
 * startFolder: The id of the folder to start (recursively processing all children)
 * mode: The mode, see enum
 * types: the types of nodes to process, e.g. ccm:io (comma seperated string)
 *
 */
@JobDescription(description = "Bulk restore nodes (from recycle)")
public class BulkRestoreNodesJob extends AbstractJob{

	protected Logger logger = Logger.getLogger(BulkRestoreNodesJob.class);

	@JobFieldDescription(description = "Folder id in which the nodes shall be recovered")
	private String targetFolder;
	@JobFieldDescription(description = "Elastic query to fetch the nodes that shall be processed.")
	private String elastic;


	private ArchiveService archiveService;


	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {

		archiveService = ArchiveServiceFactory.getLocalService();

		elastic = (String) context.getJobDetail().getJobDataMap().get("lucene");
		if(elastic ==null || elastic.isEmpty()){
			throw new IllegalArgumentException("Missing required parameter 'lucene'");
		}
		targetFolder = (String) context.getJobDetail().getJobDataMap().get("targetFolder");
		if(targetFolder==null || targetFolder.isEmpty()){
			throw new IllegalArgumentException("Missing required parameter 'targetFolder'");
		}
		NodeRunner runner = new NodeRunner();
		runner.setTask((ref)->{
			try {
				archiveService.restore(Collections.singletonList(ref.getId()), targetFolder);
				logger.info("Restored " + ref);
			} catch(Throwable t){
				logger.warn("Error restoring " + ref + ":" + t.getMessage(), t);
			}
		});
		runner.setRunAsSystem(true);
		runner.setThreaded(false);
		runner.setElastic(elastic);
		runner.setElasticStore(StoreRef.STORE_REF_ARCHIVE_SPACESSTORE);
		runner.setKeepModifiedDate(true);
		runner.setTransaction(NodeRunner.TransactionMode.LocalRetrying);
		int count=runner.run();
		logger.info("Restored "+count+" nodes");
	}
	
	public void run() {

	}
	
	@Override
	public Class[] getJobClasses() {
		// TODO Auto-generated method stub
		return allJobs;
	}
}
