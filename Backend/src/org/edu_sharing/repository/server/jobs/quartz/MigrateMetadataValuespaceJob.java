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

import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.metadataset.v2.MetadataKey;
import org.edu_sharing.metadataset.v2.MetadataSet;
import org.edu_sharing.metadataset.v2.MetadataWidget;
import org.edu_sharing.metadataset.v2.tools.MetadataHelper;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.jobs.helper.NodeRunner;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

@JobDescription(description = "Migrate a metadata field to a new valuespace (The new valuespace must already be configured for this field in the mds and must provide related-keys in order to transform)")
public class MigrateMetadataValuespaceJob extends AbstractJobMapAnnotationParams{
	protected Logger logger = Logger.getLogger(MigrateMetadataValuespaceJob.class);
	@JobFieldDescription(description = "type of objects to modify, usually ccm:io", sampleValue = "ccm:io")
	private String type;
	@JobFieldDescription(description = "the widget id of the metadataset containing the valuespace mapping information, usually same as targetProperty", sampleValue = "ccm:taxonid")
	private String mdsWidgetId;
	@JobFieldDescription(description = "the source property containing the metadata values to migrate", sampleValue = "ccm:taxonid")
	private String sourceProperty;
	@JobFieldDescription(description = "the target property to store the migrated values, may be the same as sourceProperty", sampleValue = "ccm:taxonid")
	private String targetProperty;
	@JobFieldDescription(description = "The relation to use")
	private MetadataKey.MetadataKeyRelated.Relation relation;
	@JobFieldDescription(description = "Only test and output, but do not modify/store the metadata")
	private boolean testRun;

	@Override
	public void executeInternal(JobExecutionContext context) throws JobExecutionException {
		NodeRunner runner = new NodeRunner();
		runner.setRunAsSystem(true);
		runner.setTransaction(NodeRunner.TransactionMode.Local);
		runner.setKeepModifiedDate(true);
		runner.setThreaded(false);
		runner.setTypes(Collections.singletonList(CCConstants.getValidGlobalName(type)));

		runner.setTask((nodeRef) -> {
			if(isInterrupted()) {
				return;
			}
			try {
				MetadataSet mds = MetadataHelper.getMetadataset(nodeRef);
				MetadataWidget widget;
				try {
					widget = mds.findWidget(mdsWidgetId);
				} catch(IllegalArgumentException e) {
					logger.warn("Metadataset " + mds.getId() +" does not have widget id " + mdsWidgetId + ", node " + nodeRef);
					return;
				}
				Serializable value = NodeServiceHelper.getPropertyNative(nodeRef, CCConstants.getValidGlobalName(sourceProperty));
				if(value == null){
					logger.info("Skipping null value, node " + nodeRef);
					return;
				}
				Map<MetadataKey.MetadataKeyRelated, MetadataKey> mapping = widget.getValuespaceMappingByRelation(relation);
				if(value instanceof List) {
					ArrayList<String> valueMapped = new ArrayList<>();
					((List<?>) value).stream().forEach((v) -> {
						Serializable mapped = mapValue(nodeRef, (String) v, mapping);
						if(mapped instanceof List) {
							valueMapped.addAll((Collection<? extends String>) mapped);
						} else {
							valueMapped.add((String) mapped);
						}
					});
					if(!testRun) {
						NodeServiceHelper.setProperty(nodeRef, CCConstants.getValidGlobalName(targetProperty), valueMapped);
					}
				} else if (value instanceof String) {
					Serializable valueMapped = mapValue(nodeRef, (String) value, mapping);
					if(!testRun) {
						NodeServiceHelper.setProperty(nodeRef, CCConstants.getValidGlobalName(targetProperty), valueMapped);
					}
				} else {
					logger.error("Unable to map a property of type " + value.getClass().getName() + " via valuespace, node " + nodeRef);
				}
			} catch (Exception e) {
				logger.warn(e.getMessage(), e);
			}
		});
		runner.run();
	}

	private Serializable mapValue(NodeRef nodeRef, String value, Map<MetadataKey.MetadataKeyRelated, MetadataKey> metadataKeyRelated) {
		Set<MetadataKey.MetadataKeyRelated> related = metadataKeyRelated.keySet().stream().filter((k) -> k.getKey().equals(value)).collect(Collectors.toSet());
		Set<MetadataKey> relatedMapped = related.stream().map((k) -> metadataKeyRelated.get(k)).collect(Collectors.toSet());
		String mappedIds = StringUtils.join(relatedMapped.stream().map(MetadataKey::getKey).collect(Collectors.toList()), ", ");
		if(relatedMapped.size() > 1) {
			logger.warn("Multiple relation candidates for value " + value +" => " + mappedIds +", node " + nodeRef);
		} else if (relatedMapped.size() == 0) {
			logger.warn("Value " + value + " has no candidate for mapping, node " + nodeRef);
			return value;
		}
		logger.info("Mapping " + value + " => " + mappedIds + ", node " + nodeRef);
		if(relatedMapped.size() == 1) {
			return relatedMapped.iterator().next().getKey();
		}
		return relatedMapped.stream().map(MetadataKey::getKey).collect(Collectors.toCollection(ArrayList::new));
	}
}
