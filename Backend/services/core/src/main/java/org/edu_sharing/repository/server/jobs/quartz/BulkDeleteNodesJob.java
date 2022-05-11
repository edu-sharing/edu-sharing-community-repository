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
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.xpath.operations.Bool;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.jobs.helper.NodeRunner;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.edu_sharing.service.model.NodeRef;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

import java.io.Serializable;
import java.util.ArrayList;
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
@JobDescription(description = "Bulk delete nodes (for large folders)")
public class BulkDeleteNodesJob extends AbstractJob{

	protected Logger logger = Logger.getLogger(BulkDeleteNodesJob.class);
	private org.alfresco.service.cmr.repository.NodeService nodeService;
	private NodeService nodeServiceEdu;


	@JobFieldDescription(description = "folder id to start from")
	private String startFolder;
	@JobFieldDescription(description = "Lucene query to fetch the nodes that shall be processed. When used, the 'startFolder' parameter is ignored")
	private String lucene;
	@JobFieldDescription(description = "move to recycle (false will immediately remove)")
	private boolean recycle;
	@JobFieldDescription(description = "Force delete (for broken nodes)")
	private boolean force;
	@JobFieldDescription(description = "Cleanup/remove collection references")
	private boolean collectionRefsCleanup;
	@JobFieldDescription(description = "Element types to delete (comma seperated list), e.g. ccm:map,ccm:io")
	private List<String> types;




	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {

		ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();

		ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);

		nodeService = serviceRegistry.getNodeService();
		nodeServiceEdu = NodeServiceFactory.getLocalService();

		lucene = (String) context.getJobDetail().getJobDataMap().get("lucene");
		startFolder = (String) context.getJobDetail().getJobDataMap().get("startFolder");
		if(startFolder==null || startFolder.isEmpty()){
			throw new IllegalArgumentException("Missing required parameter 'startFolder'");
		}
		Object recycleStr = context.getJobDetail().getJobDataMap().get("recycle");
		if(recycleStr==null){
			throw new IllegalArgumentException("Missing required boolean parameter 'recycle'");
		}
		recycle = Boolean.parseBoolean(recycleStr.toString());
		force = Boolean.parseBoolean((String) context.getJobDetail().getJobDataMap().get("force"));
		collectionRefsCleanup = Boolean.parseBoolean((String) context.getJobDetail().getJobDataMap().get("collectionRefsCleanup"));

		try {
			types = Arrays.stream(((String) context.getJobDetail().getJobDataMap().get("types")).
					split(",")).map(String::trim).map(CCConstants::getValidGlobalName).
					collect(Collectors.toList());
		}catch(Throwable t){}
		if(types == null){
			throw new IllegalArgumentException("Missing required boolean parameter 'types'");
		}

		List<String> collectionRefIds = new ArrayList<>();

		NodeRunner runner = new NodeRunner();
		runner.setTask((ref)->{
			logger.info("removing node " + ref);
			try {
				if (collectionRefsCleanup) {
					List<ChildAssociationRef> childRefs = nodeServiceEdu.getChildrenChildAssociationRefType(ref.getId(), CCConstants.CCM_TYPE_USAGE);
					for (ChildAssociationRef childRef : childRefs) {
						String usageResourceId = nodeServiceEdu.getProperty(childRef.getChildRef().getStoreRef().getProtocol(),
								childRef.getChildRef().getStoreRef().getIdentifier(),
								childRef.getChildRef().getId(),
								CCConstants.CCM_PROP_USAGE_RESSOURCEID);
						String storeProtocol = childRef.getChildRef().getStoreRef().getProtocol();
						String storeId = childRef.getChildRef().getStoreRef().getIdentifier();
						if (nodeServiceEdu.exists(storeProtocol, storeId, usageResourceId)
								&& nodeServiceEdu.hasAspect(storeProtocol, storeId, usageResourceId, CCConstants.CCM_ASPECT_COLLECTION_IO_REFERENCE)) {

							collectionRefIds.add(usageResourceId);
						}
					}
				}

				logger.info("will delete node:" + ref.getId());
				if (force) {
					nodeServiceEdu.removeNodeForce(ref.getStoreRef().getProtocol(), ref.getStoreRef().getIdentifier(), ref.getId(),recycle);
				} else {
					nodeServiceEdu.removeNode(ref.getId(), null, recycle);
				}
			}catch (Exception e){
				logger.error(e.getMessage(),e);
			}
		});
		runner.setTypes(types!=null && !types.isEmpty() ? types : null);
		runner.setRunAsSystem(true);
		runner.setThreaded(false);
		runner.setStartFolder(startFolder);
		runner.setLucene(lucene);
		runner.setKeepModifiedDate(true);
		runner.setTransaction(NodeRunner.TransactionMode.LocalRetrying);
		int count=runner.run();
		AuthenticationUtil.runAsSystem(() -> {
			if(lucene == null || lucene.isEmpty()) {
				nodeServiceEdu.removeNode(startFolder, null, recycle);
			}

			for(String collectionRefId : collectionRefIds){
				logger.info("will delete collection_ref: " + collectionRefId);
				try {
					if (force) {
						serviceRegistry.getRetryingTransactionHelper().doInTransaction(() -> {
							nodeServiceEdu.removeNodeForce(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getProtocol(), StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), collectionRefId,recycle);
							return null;
						});
					} else {
						nodeServiceEdu.removeNode(collectionRefId, null, recycle);
					}
				}catch (Exception e){
					logger.error(e.getMessage(),e);
				}
			}

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
