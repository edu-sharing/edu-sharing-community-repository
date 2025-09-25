package org.edu_sharing.repository.server.jobs.quartz;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.apache.commons.logging.LogFactory;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.*;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.edu_sharing.repository.server.tools.NodeTool;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.SearchServiceElastic;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@JobDescription(description = "distributes flat folder nodes over a date path folder hierarchy ")
public class NodeDistributionJob extends AbstractJobMapAnnotationParams{

	@JobFieldDescription(description = "path of the folder that contains the nodes to process.i.e.: app:company_home/ccm:test")
	private String path;

	@JobFieldDescription(description = "date pattern that defines the folder structure. i.e: yyyy/MM/dd/HH/mm/ss/SS")
	private String pattern;
	private static final String SEPARATOR = "/";

    @Autowired
    private SearchService searchService;

	public NodeDistributionJob() {
		this.logger = LogFactory.getLog(NodeDistributionJob.class);
	}
	
	@Override
	public Class<?>[] getJobClasses() {

		Class<?>[] result = Arrays.copyOf(allJobs, allJobs.length + 1);
	    result[result.length - 1] = NodeDistributionJob.class;
		return result;
	}

	@Override
	public void executeInternal(JobExecutionContext context) throws JobExecutionException {

		AuthenticationUtil.runAsSystem(() ->{
			try {

				MCAlfrescoBaseClient client = new MCAlfrescoAPIClient();

				String[] patterns = pattern.split(SEPARATOR);

				DateFormat[] formatter = new DateFormat[patterns.length];
				for (int i = 0, c = patterns.length; i<c; ++i) {

					formatter[i] = new SimpleDateFormat(patterns[i]);
				}

				// request node

				SearchResultNodeRef searchResultNodeRef = searchService.searchByDisplayPath(path, SearchServiceElastic.WORKSPACE_INDEX);

				if (searchResultNodeRef.getNodeCount() != 1) {
					throw new IllegalArgumentException("The path must reference a unique node.");
				}

				String rootId = searchResultNodeRef.getData().get(0).getNodeId();

				// request io's

				Map<String, Map<String, Object>> children =
						client.getChildrenByType(rootId, CCConstants.CCM_TYPE_IO);

				Calendar calendar = Calendar.getInstance();
				Map<String, String> cache = new HashMap<>();

				for (Entry<String, Map<String, Object>> child : children.entrySet()) {

					String childId = child.getKey();
					Map<String, Object> childProps = child.getValue();

					Date created = new Date(Long.parseLong((String) childProps.get(CCConstants.CM_PROP_C_CREATED)));

					String[] items = new String[formatter.length];
					StringBuilder path = new StringBuilder();

					for (int i = 0, c = formatter.length; i < c; ++i) {
						items[i] = formatter[i].format(created);

						if (i > 0) {
							path.append(SEPARATOR);
						}
						path.append(items[i]);
					}

					String key = path.toString();

					String nodeId = cache.get(key);

					if (nodeId == null) {

						cache.put(key, nodeId = NodeTool.createOrGetNodeByName(rootId, items));
					}

					String childName = childProps.get(CCConstants.CM_NAME).toString();

					if (client.getChild(nodeId, CCConstants.CCM_TYPE_IO, CCConstants.CM_NAME, childName) == null) {

						client.moveNode(nodeId, CCConstants.CM_ASSOC_FOLDER_CONTAINS, childId);

					} else {

						logger.warn("Node (" + childId +") can't move to (" + nodeId + ") due to name collision");

					}
				}

			} catch (Throwable e) {
				logger.error(e.getMessage(), e);
				e.printStackTrace();
			}
			return null;
		});
	}
}
