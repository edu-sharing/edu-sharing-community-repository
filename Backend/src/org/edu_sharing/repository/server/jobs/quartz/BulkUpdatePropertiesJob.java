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

import org.alfresco.model.ContentModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.jobs.helper.NodeRunner;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.edu_sharing.service.util.CSVTool;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

import java.io.BufferedReader;
import java.io.Serializable;
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@JobDescription(description = "Bulk import multiple metadata for multiple nodes")
public class BulkUpdatePropertiesJob extends AbstractJob{
	protected Logger logger = Logger.getLogger(BulkUpdatePropertiesJob.class);
	private org.alfresco.service.cmr.repository.NodeService nodeService;
	@JobFieldDescription(sampleValue = "YYYY-MM-DD", description = "Only update nodes when they're not modified since the given date. If a node was modified, it won't be updated with a new value")
	private String notModifiedSince;
	@JobFieldDescription(file = true, description = "CSV file to use for update data (Requires \"node_id\", \"property\" and \"value\" as rows")
	private String data;
	@JobFieldDescription(description = "test mode only", sampleValue = "true")
	private Boolean test;
	private CSVTool.CSVResult csv;
	private Date notModifiedSinceDate;

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {

		ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();

		ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);

		nodeService = serviceRegistry.getNodeService();

		data = (String) context.getJobDetail().getJobDataMap().get(JobHandler.FILE_DATA);
		test = context.getJobDetail().getJobDataMap().getBooleanFromString("test");
		csv = readCSV(data);
		notModifiedSince = (String) context.getJobDetail().getJobDataMap().get("notModifiedSince");
		if(notModifiedSince != null) {
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
			try {
				notModifiedSinceDate = formatter.parse(notModifiedSince);
			} catch (ParseException e) {
				throw new RuntimeException(e);
			}
			logger.info("notModifiedSince was set, will not update nodes which were modified after " + notModifiedSinceDate);
		}
		NodeRunner runner = new NodeRunner();
		Set<NodeRef> nodeList = csv.getLines().stream().map(
				l -> new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, l.get("node_id"))
		).collect(Collectors.toSet());

		runner.setTask((ref) -> {
			NodeRef nodeRef = new NodeRef(ref.getStoreRef(), ref.getId());
			logger.info("Bulk edit metadata for node " + ref.getId());
			List<Map<String, String>> toUpdate = csv.getLines().stream().filter(
					l -> l.get("node_id").equals(ref.getId())
			).collect(Collectors.toList());
			toUpdate.forEach(line -> {
						String property = CCConstants.getValidGlobalName(line.get("property"));
						String value = line.get("value");
						if(!nodeService.exists(nodeRef)) {
							logger.warn("Given node with id " + nodeRef.getId() + " doesn't exist, skipping");
							return;
						}
						if(notModifiedSinceDate != null) {
							Date modified = (Date) nodeService.getProperty(nodeRef, ContentModel.PROP_MODIFIED);
							if(modified.after(notModifiedSinceDate)) {
								logger.warn("Node " + nodeRef + " has been modified after given notModifiedSince value, will skip it " + modified);
								return;
							}
						}
						logger.info("Set property " + property + "=" + value + ", node: " + ref.getId());
						if(!test) {
							nodeService.setProperty(nodeRef, QName.createQName(property), value);
						}
					});
		});
		runner.setNodesList(nodeList);
		runner.setRunAsSystem(true);
		runner.setThreaded(false);
		runner.setKeepModifiedDate(true);
		runner.setTransaction(NodeRunner.TransactionMode.Local);
		int count=runner.run();
		logger.info("Processed "+count+" nodes");
	}

	private CSVTool.CSVResult readCSV(String data) {
		CSVTool.CSVResult csv = CSVTool.readCSV(new BufferedReader(new StringReader(data)), ',');
		if(csv == null) {
			throw new IllegalArgumentException("Provided csv was not parsable. Is the file encoding utf-8?");
		}
		Arrays.asList(
				"node_id", "property", "value"
		).forEach((header) -> {
			if(!csv.getHeaders().contains(header)){
				throw new IllegalArgumentException("Provided csv must contain " + header + " headers");
			}
		});
		return csv;
	}
	public void run() {

	}

	@Override
	public Class[] getJobClasses() {
		return allJobs;
	}
}
