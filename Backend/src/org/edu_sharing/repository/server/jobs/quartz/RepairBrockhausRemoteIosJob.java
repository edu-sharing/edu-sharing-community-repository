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

import org.alfresco.service.ServiceRegistry;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.metadataset.v2.MetadataSetV2;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.repository.server.jobs.helper.NodeRunner;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.model.NodeRef;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.service.search.model.SearchToken;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@JobDescription(description = "Fix brockhaus nodes with wrong identifiers")
public class RepairBrockhausRemoteIosJob extends AbstractJob{

	protected Logger logger = Logger.getLogger(RepairBrockhausRemoteIosJob.class);
	private org.alfresco.service.cmr.repository.NodeService nodeService;
	private NodeService nodeServiceEdu;

	@JobFieldDescription(description = "testMode", sampleValue = "true")
	private Boolean test;

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {

		ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();

		ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);

		nodeService = serviceRegistry.getNodeService();
		nodeServiceEdu = NodeServiceFactory.getLocalService();

		test = context.getJobDetail().getJobDataMap().getBooleanFromString("test");
		if(test == null){
			throw new IllegalArgumentException("Missing required boolean parameter 'test'");
		}
		NodeRunner runner = new NodeRunner();
		SearchService searchServiceBrockhaus = SearchServiceFactory.getSearchService(ApplicationInfoList.getRepositoryInfoByRepositoryType("BROCKHAUS").getAppId());
		runner.setTask((ref)->{
			try {
				if(isInterrupted()) {
					return;
				}
				String brockhausId = (String) NodeServiceHelper.getPropertyNative(ref, CCConstants.CCM_PROP_REMOTEOBJECT_NODEID);
				if(brockhausId == null) {
					logger.warn("Object " + ref + " does not have a remote id");
					return;
				}
				if(brockhausId.contains("%2f")) {
					logger.info("Object " + ref + " already has a valid id: " + brockhausId);
					return;
				}
				String title = (String) NodeServiceHelper.getPropertyNative(ref, CCConstants.LOM_PROP_GENERAL_TITLE);
				Serializable description = NodeServiceHelper.getPropertyNative(ref, CCConstants.LOM_PROP_GENERAL_DESCRIPTION);
				if(description instanceof List) {
					description = (Serializable) ((List<?>) description).get(0);
				}
				if(title == null) {
					logger.warn("Object " + ref + " " + brockhausId + " does not have a title");
					return;
				}
				logger.info("Processing Object " + ref + "... " + brockhausId + " " + title);
				Map<String, String[]> criteria = new HashMap<>();
				criteria.put(MetadataSetV2.DEFAULT_CLIENT_QUERY_CRITERIA, new String[]{title});
				SearchToken token = new SearchToken();
				token.setMaxResult(100);
				SearchResultNodeRef results = searchServiceBrockhaus.searchV2(null, MetadataSetV2.DEFAULT_CLIENT_QUERY, criteria, token);
				Serializable finalDescription = description;
				List<NodeRef> filtered = results.getData().stream().filter(r -> {
					HashMap<String, Object> props = r.getProperties();
					return
							Objects.equals(props.get(CCConstants.LOM_PROP_GENERAL_TITLE), title) &&
							Objects.equals(props.get(CCConstants.LOM_PROP_GENERAL_DESCRIPTION), finalDescription);
				}).collect(Collectors.toList());
				if(filtered.size() == 1) {
					String newId = filtered.get(0).getNodeId();
					logger.info("Replacing old id " + brockhausId + " with new id " + newId);
					if(!test) {
						NodeServiceHelper.setProperty(ref, CCConstants.CCM_PROP_REMOTEOBJECT_NODEID, newId);
					}
				} else if(filtered.size() == 0) {
					logger.warn("Failed: Brockhaus search did not return results for this item");
				} else {
					logger.warn("Failed: Brockhaus search did return too many results for this item (" + filtered.size() + ")");
				}
			}catch (Throwable t){
				logger.error(t.getMessage(),t);
			}
		});
		runner.setRunAsSystem(true);
		runner.setThreaded(false);
		runner.setKeepModifiedDate(true);
		runner.setLucene("ASPECT:\"ccm:remoterepository\" AND @ccm\\:remoterepositorytype:\"BROCKHAUS\"");
		runner.setTransaction(NodeRunner.TransactionMode.LocalRetrying);
		int count=runner.run();
		logger.info("Processed "+count+" nodes");
	}
}
