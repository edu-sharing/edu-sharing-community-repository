package org.edu_sharing.service.bulk;


import org.alfresco.model.ContentModel;
import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.*;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionHistory;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.policy.NodeCustomizationPolicies;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.metadataset.v2.tools.MetadataHelper;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.alfresco.service.search.CMISSearchHelper;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BulkServiceImpl implements BulkService {
	private static final String PRIMARY_FOLDER_NAME = "SYNC_OBJ";
	static NodeService nodeServiceAlfresco = (NodeService) AlfAppContextGate.getApplicationContext().getBean("alfrescoDefaultDbNodeService");
	static ServiceRegistry serviceRegistry = (ServiceRegistry) AlfAppContextGate.getApplicationContext().getBean(ServiceRegistry.SERVICE_REGISTRY);
	static VersionService versionServiceAlfresco = serviceRegistry.getVersionService();
	static Repository repositoryHelper = (Repository) AlfAppContextGate.getApplicationContext().getBean("repositoryHelper");
	NodeService dbNodeService = (NodeService)AlfAppContextGate.getApplicationContext().getBean("alfrescoDefaultDbNodeService");

	private static Logger logger = Logger.getLogger(BulkServiceImpl.class);
	private final List<String> propsToClean;
	private NodeRef primaryFolder;



	/**
	 * get or create the folder
	 * @param parent
	 * @param name
	 * @param propertiesNative Provide the properties of the created child. This will be taken into account when setting the metadataset id of the folder
	 * @return
	 */

	public NodeRef getOrCreate(NodeRef parent, String name, HashMap<String, Object> propertiesNative){
		name = NodeServiceHelper.cleanupCmName(name);
		NodeRef node = nodeServiceAlfresco.getChildByName(parent, ContentModel.ASSOC_CONTAINS, name);
		if(node == null){
			Map<QName, Serializable> props=new HashMap<>();
			props.put(ContentModel.PROP_NAME, name);
			if(propertiesNative!=null){
				props.put(QName.createQName(CCConstants.CM_PROP_METADATASET_EDU_METADATASET),
						(Serializable)propertiesNative.get(CCConstants.CM_PROP_METADATASET_EDU_METADATASET));
				props.put(QName.createQName(CCConstants.CM_PROP_METADATASET_EDU_FORCEMETADATASET),
						(Serializable)propertiesNative.get(CCConstants.CM_PROP_METADATASET_EDU_FORCEMETADATASET));
			}
			return serviceRegistry.getNodeService().createNode(parent, ContentModel.ASSOC_CONTAINS,
					QName.createQName(name),
					QName.createQName(CCConstants.CCM_TYPE_MAP),
					props).getChildRef();
		}
		return node;
	}

	public BulkServiceImpl(){
		primaryFolder = getOrCreate(repositoryHelper.getCompanyHome(), PRIMARY_FOLDER_NAME, null);


		// all props which might be overriden through the user, reset them and clean them
		propsToClean = new ArrayList<>();
		propsToClean.addAll(Arrays.asList(NodeCustomizationPolicies.SAFE_PROPS));
		propsToClean.addAll(Arrays.asList(NodeCustomizationPolicies.LICENSE_PROPS));
		propsToClean.addAll(CCConstants.getLifecycleContributerPropsMap().values());
		propsToClean.addAll(CCConstants.getMetadataContributerPropsMap().values());
		/*
		for(QName aspect : dictionaryService.getAllAspects()){
			propsToClean.addAll(
					dictionaryService.getAspect(aspect).getProperties().keySet().stream()
							.map(QName::toString).collect(Collectors.toList())
			);
		}*/
	}
	@Override
	public NodeRef sync(String group, List<String> match, List<String> groupBy, String type, List<String> aspects, HashMap<String, String[]> properties, boolean resetVersion) throws Throwable {
		if(match == null || match.size() == 0){
			throw new IllegalArgumentException("match should contain at least 1 property");
		}
		HashMap<String, String[]> propertiesFiltered = properties.entrySet().stream().filter((e) -> match.contains(e.getKey())).
				collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
						(a, b) -> b, HashMap::new));
		if(propertiesFiltered.size() != match.size()){
			throw new IllegalArgumentException("match contained property names that did not exist in the properties section. Please check that you provide data for all match properties.");
		}
		properties = NodeServiceHelper.transformShortToLongProperties(properties);
		NodeRef existing = find(propertiesFiltered);
		HashMap<String, Object> propertiesNative = NodeServiceHelper.getPropertiesSinglevalue(properties);
		propertiesNative.put(CCConstants.CM_NAME, NodeServiceHelper.cleanupCmName((String)(propertiesNative.get(CCConstants.CM_NAME))) + "_" + System.currentTimeMillis());
		propertiesNative.remove(CCConstants.CCM_PROP_IO_VERSION_COMMENT);
		if(existing == null) {
			NodeRef groupFolder = getOrCreate(primaryFolder, group, propertiesNative);
			if(groupBy != null && groupBy.size()>0){
				if(groupBy.size() == 1){
					groupFolder = getOrCreate(groupFolder, propertiesNative.get(CCConstants.getValidGlobalName(groupBy.get(0))).toString(), propertiesNative);
				} else {
					throw new IllegalArgumentException("groupBy currently only supports exactly one value");
				}
			}
			// clean up and remove "null" values since they will result in weird data otherwise
			propertiesNative = new HashMap<>(propertiesNative.entrySet().stream().filter((e) -> e.getValue() != null).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
			// add a default comment for bulk import
			propertiesNative.put(CCConstants.CCM_PROP_IO_VERSION_COMMENT, CCConstants.VERSION_COMMENT_BULK_CREATE);
			existing = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,
					NodeServiceFactory.getLocalService().createNodeBasic(
							groupFolder.getId(),
							CCConstants.getValidGlobalName(type),
							propertiesNative
					));
			// 2. versioning (use the regular service for proper versioning)
			NodeServiceFactory.getLocalService().createVersion(existing.getId());
		}else{
			String blocked = NodeServiceHelper.getProperty(existing, CCConstants.CCM_PROP_IO_IMPORT_BLOCKED);
			if (Boolean.parseBoolean(blocked)) {
				throw new IllegalStateException("The given node was blocked for any updates and should not be reimported");
			}
			HashMap<String, Object> propertiesKeep = checkInternalOverrides(propertiesNative, existing);
			if(resetVersion){
				versionServiceAlfresco.deleteVersionHistory(existing);
			}
			propertiesNative = getCleanProps(existing, propertiesNative);
			propertiesNative.put(CCConstants.CCM_PROP_IO_VERSION_COMMENT, resetVersion ? CCConstants.VERSION_COMMENT_BULK_CREATE : CCConstants.VERSION_COMMENT_BULK_UPDATE);
			NodeServiceFactory.getLocalService().updateNodeNative(existing.getId(), propertiesNative);
			// version the previous state
			NodeServiceFactory.getLocalService().createVersion(existing.getId());
			if(propertiesKeep != null){
				propertiesKeep = getCleanProps(existing, propertiesKeep);
				propertiesKeep.put(CCConstants.CCM_PROP_IO_VERSION_COMMENT, CCConstants.VERSION_COMMENT_BULK_UPDATE_RESYNC);
				NodeServiceFactory.getLocalService().updateNodeNative(existing.getId(), propertiesKeep);
				// 2. versioning
				NodeServiceFactory.getLocalService().createVersion(existing.getId());
			}
		}
		if(aspects != null) {
			NodeRef finalExisting = existing;
			aspects.forEach((a) -> NodeServiceFactory.getLocalService().addAspect(finalExisting.getId(), CCConstants.getValidGlobalName(a)));
		}
		return existing;

	}
	private List<String> getAllAvailableProperties(NodeRef nodeRef) throws Exception {

		/*HashMap<String, Serializable> cleanProps = new HashMap<>();
		propsToClean.forEach((k) -> cleanProps.put(k, null));
		return cleanProps;*/
		return Stream.concat(MetadataHelper.getWidgetsByNode(nodeRef).stream().map((w) -> CCConstants.getValidGlobalName(w.getId())).
				filter(Objects::nonNull),
				propsToClean.stream())
				.collect(Collectors.toList());
	}
	/**
	 * alfresco will not override "removed" props, so we try to clean up via the mds
	 * @return
	 */
	private HashMap<String, Object> getCleanProps(NodeRef nodeRef, HashMap<String, Object> props) throws Exception {
		HashMap<String, Object> mergedProps = new HashMap<>();
		getAllAvailableProperties(nodeRef).forEach((k) -> mergedProps.put(k, null));
		mergedProps.putAll(props);
		mergedProps.put(CCConstants.CCM_PROP_IO_CREATE_VERSION, false);
		return mergedProps;
	}
	/**
	 * This method takes new properties as an input, and checks which properties might be already "touched" internally
	 * (meaning not by the bulk import itself) and will always keep the latest version of the internal ones
	 * @param propertiesIn
	 * @param nodeRef
	 * @return
	 */
	private HashMap<String, Object> checkInternalOverrides(HashMap<String, Object> propertiesIn, NodeRef nodeRef) throws Exception {
		VersionHistory history = versionServiceAlfresco.getVersionHistory(nodeRef);
		if(history == null){
			return null;
		}
		List<Version> versions = new ArrayList<>(history.getAllVersions());
		Collections.reverse(versions);
		logger.debug(propertiesIn.get(CCConstants.CM_NAME));
		Map<String, Serializable> importerProps = null;
		HashMap<String, Object> modifiedProps = new HashMap<>();
		boolean changed = false;
		for(Version version : versions){
			Map<String, Serializable> currentProps = version.getVersionProperties();
			String vname = (String) currentProps.get(CCConstants.CCM_PROP_IO_VERSION_COMMENT);
			if(vname.equals(CCConstants.VERSION_COMMENT_BULK_CREATE)){
				importerProps = version.getVersionProperties();
				continue;
			}
			if(importerProps != null){
				if(vname.equals(CCConstants.VERSION_COMMENT_BULK_UPDATE)){
					Map<String, Object> diffs = getPropDiffs(importerProps, currentProps);
					for (String diff : diffs.keySet()) {
						importerProps.put(diff, currentProps.get(diff));
					}
				} else if(vname.equals(CCConstants.VERSION_COMMENT_BULK_UPDATE_RESYNC)){
					// we do nothing for these, these are just resynced once from previous changes
				} else {
					modifiedProps.putAll(getPropDiffs(importerProps, currentProps));
				}
			}
		}
		HashMap<String, Object> returnProps = new HashMap<>(propertiesIn);
		if(importerProps!=null) {
			Collection<String> widgets = getAllAvailableProperties(nodeRef);
			// copy all new props which are untouched
			for (Map.Entry<String, Object> entry : modifiedProps.entrySet()) {
				if(!widgets.contains(entry.getKey())){
					continue;
				}
				logger.info("Property " + entry.getKey() + " ignored since it was modified outside of the bulk api");
				returnProps.put(entry.getKey(), entry.getValue());
				changed = true;
			}
		}
		if(changed) {
			return returnProps;
		}
		return null;
	}

	/**
	 * Return all props that have a different value inside the "diff" map
	 */
	private HashMap<String, Object> getPropDiffs(Map<String, Serializable> base, Map<String, Serializable> diff) {
		HashMap<String, Object> diffs = new HashMap<>();
		//diffs.putAll(getPropDiffsOneWay(base,diff));
		//diffs.putAll(getPropDiffsOneWay(diff,base));
		for(Map.Entry<String, Serializable> entry : diff.entrySet()){
			Serializable entryDiff = base.get(entry.getKey());
			if(Objects.equals(entryDiff,entry.getValue())){
				continue;
			}
			diffs.put(entry.getKey(),entry.getValue());
		}
		return diffs;
	}

	@Override
	public NodeRef find(HashMap<String, String[]> properties) throws Exception {
		CMISSearchHelper.CMISSearchData data = new CMISSearchHelper.CMISSearchData();
		// this uses SOLR and is not synchronized!
		// data.inTree = primaryFolder.getId();

		List<NodeRef> result = CMISSearchHelper.fetchNodesByTypeAndFilters(CCConstants.CCM_TYPE_IO,
				NodeServiceHelper.getPropertiesSinglevalue(
						NodeServiceHelper.transformShortToLongProperties(properties)
				),data);

		result = result.stream().filter((r) -> {
			Path path = dbNodeService.getPath(r);
			for (Path.Element p : path) {
				if (p instanceof Path.ChildAssocElement) {
					NodeRef ref = ((Path.ChildAssocElement) p).getRef().getParentRef();
					if (primaryFolder.equals(ref)) {
						return true;
					}
				}
			}
			return false;
		}).collect(Collectors.toList());
		if(result.size()==1){
			return result.get(0);
		}else if(result.size()>1){
			StringBuilder props = new StringBuilder();
			for (Map.Entry<String, String[]> entry : properties.entrySet()) {
				props.append(entry.getKey()).append(":").append(entry.getValue()[0]).append(" ");
			}
			throw new Exception("The given properties ("+props+") matched more than 1 node (" + result.size() + "). Please check your criterias and make sure they match unique data");
		}
		return null;
	}
}