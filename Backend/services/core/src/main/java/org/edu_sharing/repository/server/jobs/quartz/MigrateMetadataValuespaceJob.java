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
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.queryparser.classic.QueryParser;
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
	private MappingCallback mappingCallback;
	public enum Mode {
		Merge,
		Replace
	}
	public interface MappingCallback {
		Serializable onMap(NodeRef nodeRef, String input, boolean reverseMapping);
	}


	protected static Logger logger = Logger.getLogger(MigrateMetadataValuespaceJob.class);
	@JobFieldDescription(description = "type of objects to modify, usually ccm:io, split mulitple values via comma", sampleValue = "ccm:io")
	private String type;
	@JobFieldDescription(description = "Single node id to apply the task (for testing purposes only)")
	private String nodeId;
	@JobFieldDescription(description = "the widget id of the metadataset containing the valuespace mapping information, usually same as targetProperty", sampleValue = "ccm:taxonid")
	private String mdsWidgetId;
	@JobFieldDescription(description = "the source property containing the metadata values to migrate", sampleValue = "ccm:taxonid")
	private String sourceProperty;
	@JobFieldDescription(description = "the target property to store the migrated values, may be the same as sourceProperty", sampleValue = "ccm:taxonid")
	private String targetProperty;
	@JobFieldDescription(description = "The relation(s) to use (all values will be combined into the result)")
	private List<MetadataKey.MetadataKeyRelated.Relation> relations;
	@JobFieldDescription(description = "The mode to use (Merge = Merge any existing target field values with the mapping values, Replace = replace the target field values")
	private Mode mode;
	@JobFieldDescription(description = "Use solr/searchindex. This can improve performance significantly because it only fetches nodes with the sourceProperty set. Make sure your index is up to date")
	private boolean viaSolr;
	@JobFieldDescription(description = "Only test and output, but do not modify/store the metadata")
	private boolean testRun;
	@JobFieldDescription(description = "Clear/remove the source field content after successful migration")
	private boolean clearSourceProperty;
	@Override
	public void executeInternal(JobExecutionContext context) throws JobExecutionException {
		if(clearSourceProperty && sourceProperty.equals(targetProperty)) {
			throw new IllegalArgumentException("clearSourceProperty can't be used when sourceProperty == targetProperty!");
		}
		NodeRunner runner = new NodeRunner();
		runner.setRunAsSystem(true);
		runner.setTransaction(NodeRunner.TransactionMode.Local);
		runner.setKeepModifiedDate(true);
		runner.setThreaded(false);
		if(viaSolr) {
			runner.setLucene("@" + QueryParser.escape(sourceProperty) + ":\"*\"");
		}
		runner.setTypes(Arrays.stream(type.split(",")).map(String::trim).map(CCConstants::getValidGlobalName).collect(Collectors.toList()));

		runner.setTask((nodeRef) -> {
			if(isInterrupted()) {
				return;
			}
			try {
				MetadataSet mds = MetadataHelper.getMetadataset(nodeRef);
				MetadataWidget widget;
				Collection<MetadataKey> targetKeys;
				List<Map<String, Collection<MetadataKey.MetadataKeyRelated>>> mappings = null;
				mappingCallback = (MappingCallback)getJobDataMap().get("mappingCallback");
				try {
					if(mdsWidgetId != null) {
						widget = mds.findWidget(mdsWidgetId);
						mappings = relations.stream().map(widget::getValuespaceMappingByRelation).collect(Collectors.toList());
					}
					targetKeys = mds.findWidget(targetProperty).getValues();
				} catch(IllegalArgumentException e) {
					logger.warn("Metadataset " + mds.getId() +" does not have widget id " + mdsWidgetId + ", node " + nodeRef);
					return;
				}
				Object value = NodeServiceHelper.getPropertyNative(nodeRef, CCConstants.getValidGlobalName(sourceProperty));
				Object target = NodeServiceHelper.getPropertyNative(nodeRef, CCConstants.getValidGlobalName(targetProperty));
				if(value == null){
					logger.debug("Skipping null value, node " + nodeRef);
					return;
				}
				HashSet<String> mapped = new HashSet<>();
				if(mappings == null) {
					mapped.addAll(mapValueToTarget(nodeRef, null, targetKeys, mode, value, target, true, mappingCallback));
				} else {
					mapped.addAll(mapValueToTarget(nodeRef, mappings, targetKeys, mode, value, target, true, mappingCallback));
				}
				if(mapped.size() > 0) {
					logger.info("Mapped " + value + " -> " + StringUtils.join(mapped,", "));
				}
				if(!testRun) {
					 if(mapped.size() > 0) {
						NodeServiceHelper.setProperty(nodeRef,
								CCConstants.getValidGlobalName(targetProperty),
								mapped,
								false);
					}
					 if(clearSourceProperty) {
						 NodeServiceHelper.removeProperty(nodeRef, CCConstants.getValidGlobalName(sourceProperty));
					 }
					logger.info("set property " + targetProperty +" for " + nodeRef.getId());
				}

			} catch (Exception e) {
				logger.warn(e.getMessage(), e);
			}
		});
		if(nodeId != null && !nodeId.isEmpty()) {
			logger.info("Transforming single node " +nodeId);
			AuthenticationUtil.runAsSystem(() -> {
				runner.getTask().accept(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId));
				return null;
			});
			return;
		}
		runner.run();
	}

	public static HashSet<String> mapValueToTarget(NodeRef nodeRef, List<Map<String, Collection<MetadataKey.MetadataKeyRelated>>> mapping, Collection<MetadataKey> targetKeys, Mode mode, Object value, Object targetValue, boolean reverseMapping, MappingCallback callback) {
		ArrayList<String> valueMapped = new ArrayList<>();
		if(value instanceof String || value instanceof Collection) {
			if(value instanceof String) {
				value = Collections.singletonList(value);
			}
			((Collection<?>) value).stream().forEach((v) -> {
				if(callback != null) {
					Serializable mapped = mapValue(nodeRef, (String) v, null, targetKeys, reverseMapping, callback);
					if (mapped != null) {
						if (mapped instanceof Collection) {
							valueMapped.addAll((Collection<? extends String>) mapped);
						} else {
							valueMapped.add((String) mapped);
						}
					}
					return;
				}
				for(Map<String, Collection<MetadataKey.MetadataKeyRelated>> m: mapping) {
					Serializable mapped = mapValue(nodeRef, (String) v, m, targetKeys, reverseMapping, null);
					if (mapped != null) {
						if (mapped instanceof Collection) {
							if(((Collection<?>) mapped).isEmpty()) {
								continue;
							}
							valueMapped.addAll((Collection<? extends String>) mapped);
						} else {
							valueMapped.add((String) mapped);
						}
						return;
					}
				}
			});

		} else if(value == null) {

		} else {
			logger.error("Unable to map a property of type " + value.getClass() + " via valuespace, node " + nodeRef);
		}
		HashSet<String> target = new HashSet<>();
		if(mode.equals(Mode.Merge)) {
			if(targetValue == null) {
			} else if (targetValue instanceof String) {
				target.add((String) targetValue);
			} else if (targetValue instanceof Collection) {
				target.addAll((Collection<? extends String>) targetValue);
			}
			target.addAll(valueMapped);
		}
		target.addAll(valueMapped);
		return target;
	}

	/**
	 *
	 * @param nodeRef
	 * @param value
	 * @param metadataKeyRelated
	 * Data structure containting sourceId -> [relation with target id[, relation with target id, ...]]
	 * @param targetKeys
	 * @param reverseMapping
	 * When true: the "value" is supposed to have on of the relation target ids. It will be mapped matching source ids of this link
	 * When false: The "value" is supposed to be the source id. It will be mapped to target ids of this link. This mode is much faster because of the given data structure
	 * @return
	 */
	private static Serializable mapValue(NodeRef nodeRef, String value, Map<String, Collection<MetadataKey.MetadataKeyRelated>> metadataKeyRelated, Collection<MetadataKey> targetKeys, boolean reverseMapping, MappingCallback callback) {
		if(callback != null) {
			return callback.onMap(nodeRef, value, reverseMapping);
		}
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
		if(targetKeys != null) {
			mappedKeys = mappedKeys.stream().filter((keyId) -> targetKeys.stream().anyMatch(
					key -> key.getKey().equals(keyId) ||
							(key.getAlternativeKeys() != null && key.getAlternativeKeys().contains(keyId))
			)).collect(Collectors.toList());
		}
		if(mappedKeys.size() == 1) {
			return mappedKeys.iterator().next();
		}
		return (Serializable) mappedKeys;
	}
}
