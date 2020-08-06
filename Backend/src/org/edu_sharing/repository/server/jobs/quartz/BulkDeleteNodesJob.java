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
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.xpath.operations.Bool;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.jobs.helper.NodeRunner;
import org.edu_sharing.service.model.NodeRef;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
public class BulkDeleteNodesJob extends AbstractJob{

	protected Logger logger = Logger.getLogger(BulkDeleteNodesJob.class);
	private org.alfresco.service.cmr.repository.NodeService nodeService;
	private List<String> types;
	private NodeService nodeServiceEdu;

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {

		ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();

		ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);

		nodeService = serviceRegistry.getNodeService();
		nodeServiceEdu = NodeServiceFactory.getLocalService();

		String startFolder = (String) context.getJobDetail().getJobDataMap().get("startFolder");
		if(startFolder==null){
			throw new IllegalArgumentException("Missing required parameter 'startFolder'");
		}
		Object recycleStr = context.getJobDetail().getJobDataMap().get("recycle");
		if(recycleStr==null){
			throw new IllegalArgumentException("Missing required boolean parameter 'recycle'");
		}
		boolean recycle = Boolean.parseBoolean(recycleStr.toString());

		try {
			types = Arrays.stream(((String) context.getJobDetail().getJobDataMap().get("types")).
					split(",")).map(String::trim).map(CCConstants::getValidGlobalName).
					collect(Collectors.toList());
		}catch(Throwable t){}

		NodeRunner runner = new NodeRunner();
		runner.setTask((ref)->{
			logger.info("removing node " + ref);
			nodeServiceEdu.removeNode(ref.getId(), null, recycle);
		});
		runner.setTypes(types!=null && !types.isEmpty() ? types : null);
		runner.setRunAsSystem(true);
		runner.setThreaded(true);
		runner.setStartFolder(startFolder);
		runner.setKeepModifiedDate(true);
		runner.setTransaction(NodeRunner.TransactionMode.Local);
		int count=runner.run();
		AuthenticationUtil.runAsSystem(() -> {
			nodeServiceEdu.removeNode(startFolder, null, recycle);
			return null;
		});
		logger.info("Processed "+count+" nodes");
	}
	
	public void run() {

	}
	
	@Override
	public Class[] getJobClasses() {
		// TODO Auto-generated method stub
		return allJobs;
	}
}
