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
	public enum Mode {
		Merge,
		Replace
	}


	protected static Logger logger = Logger.getLogger(MigrateMetadataValuespaceJob.class);
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
	@JobFieldDescription(description = "The mode to use (Merge = Merge any existing target field values with the mapping values, Replace = replace the target field valiues")
	private Mode mode;
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
				Map<String, Collection<MetadataKey.MetadataKeyRelated>> mapping;
				try {
					widget = mds.findWidget(mdsWidgetId);
					mapping = widget.getValuespaceMappingByRelation(relation);
				} catch(IllegalArgumentException e) {
					logger.debug("Metadataset " + mds.getId() +" does not have widget id " + mdsWidgetId + ", node " + nodeRef);
					return;
				}
				Object value = NodeServiceHelper.getPropertyNative(nodeRef, CCConstants.getValidGlobalName(sourceProperty));
				Object target = NodeServiceHelper.getPropertyNative(nodeRef, CCConstants.getValidGlobalName(targetProperty));
				if(value == null){
					logger.debug("Skipping null value, node " + nodeRef);
					return;
				}
				HashSet<String> mapped = mapValueToTarget(nodeRef, mapping, mode, value, target, true);
				if(mapped!=null && mapped.size() > 0) {
					logger.info("Mapped " + value + " -> " + StringUtils.join(mapped,", "));
				}
				if(!testRun && mapped!=null && mapped.size() > 0) {
					NodeServiceHelper.setProperty(nodeRef,
							CCConstants.getValidGlobalName(targetProperty),
							mapped
					);
					logger.info("set property " + targetProperty +" for " + nodeRef.getId());
				}

			} catch (Exception e) {
				logger.warn(e.getMessage(), e);
			}
		});
		runner.run();
	}

	public static HashSet<String> mapValueToTarget(NodeRef nodeRef, Map<String, Collection<MetadataKey.MetadataKeyRelated>> mapping, Mode mode, Object value, Object targetValue, boolean reverseMapping) {
		if(value instanceof String || value instanceof List) {
			if(value instanceof String) {
				value = Collections.singletonList(value);
			}
			ArrayList<String> valueMapped = new ArrayList<>();
			((List<?>) value).stream().forEach((v) -> {
				Serializable mapped = mapValue(nodeRef, (String) v, mapping, reverseMapping);
				if(mapped != null) {
					if (mapped instanceof Collection) {
						valueMapped.addAll((Collection<? extends String>) mapped);
					} else {
						valueMapped.add((String) mapped);
					}
				}
			});
			HashSet<String> target = new HashSet<>();
			if(mode.equals(Mode.Merge)) {
				if(targetValue == null) {
				} else if (targetValue instanceof String) {
					target.add((String) targetValue);
				} else if (targetValue instanceof List) {
					target.addAll((Collection<? extends String>) targetValue);
				}
				target.addAll(valueMapped);
			}
			target.addAll(valueMapped);
			return target;
		} else {
			logger.error("Unable to map a property of type " + value.getClass().getName() + " via valuespace, node " + nodeRef);
		}
		return null;
	}

	/**
	 *
	 * @param nodeRef
	 * @param value
	 * @param metadataKeyRelated
	 * Data structure containting sourceId -> [relation with target id[, relation with target id, ...]]
	 * @param reverseMapping
	 * When true: the "value" is supposed to have on of the relation target ids. It will be mapped matching source ids of this link
	 * When false: The "value" is supposed to be the source id. It will be mapped to target ids of this link. This mode is much faster because of the given data structure
	 * @return
	 */
	private static Serializable mapValue(NodeRef nodeRef, String value, Map<String, Collection<MetadataKey.MetadataKeyRelated>> metadataKeyRelated, boolean reverseMapping) {
		Collection<String> mappedKeys;
		Collection<MetadataKey.MetadataKeyRelated> relatedMapped;
		if (reverseMapping) {
			mappedKeys = metadataKeyRelated.entrySet().stream().filter(
					(e) -> e.getValue().stream().filter((t) -> t.getKey().equals(value)).count() > 0
					).map(Map.Entry::getKey).collect(Collectors.toSet());
		} else {
			relatedMapped = metadataKeyRelated.getOrDefault(value, new ArrayList<>());
			mappedKeys = relatedMapped.stream().map(MetadataKey::getKey).collect(Collectors.toCollection(ArrayList::new));
		}
		String mappedIds = StringUtils.join(mappedKeys, ", ");
		if(mappedKeys.size() > 1) {
			//logger.info("Multiple relation candidates for value " + value +" => " + mappedIds +", node " + nodeRef);
		} else if (mappedKeys.size() == 0) {
			//logger.warn("Value " + value + " has no candidate for mapping, node " + nodeRef);
			return null;
		}
		//logger.debug("Mapping " + value + " => " + mappedIds + ", node " + nodeRef);
		if(mappedKeys.size() == 1) {
			return mappedKeys.iterator().next();
		}
		return (Serializable) mappedKeys;
	}
}
