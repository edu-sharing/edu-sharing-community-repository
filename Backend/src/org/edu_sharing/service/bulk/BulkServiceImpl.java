package org.edu_sharing.service.bulk;


import org.alfresco.model.ContentModel;
import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.search.CMISSearchHelper;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BulkServiceImpl implements BulkService {
	private static final String PRIMARY_FOLDER_NAME = "SYNC_OBJ";
	static NodeService nodeServiceAlfresco = (NodeService) AlfAppContextGate.getApplicationContext().getBean("alfrescoDefaultDbNodeService");
	static ServiceRegistry serviceRegistry = (ServiceRegistry) AlfAppContextGate.getApplicationContext().getBean(ServiceRegistry.SERVICE_REGISTRY);
	static Repository repositoryHelper = (Repository) AlfAppContextGate.getApplicationContext().getBean("repositoryHelper");

	private static Logger logger = Logger.getLogger(BulkServiceImpl.class);
	private NodeRef primaryFolder;

	public NodeRef getOrCreate(NodeRef parent, String name){
		NodeRef node = nodeServiceAlfresco.getChildByName(parent, ContentModel.ASSOC_CONTAINS, name);
		if(node == null){
			Map<QName, Serializable> props=new HashMap<>();
			props.put(ContentModel.PROP_NAME, name);
			return serviceRegistry.getNodeService().createNode(parent, ContentModel.ASSOC_CONTAINS,
					QName.createQName(name),
					QName.createQName(CCConstants.CCM_TYPE_MAP),
					props).getChildRef();
		}
		return node;
	}

	public BulkServiceImpl(){
		primaryFolder = getOrCreate(repositoryHelper.getCompanyHome(), PRIMARY_FOLDER_NAME);
	}
	@Override
	public NodeRef sync(String group, List<String> match, String type, List<String> aspects, HashMap<String, String[]> properties) throws Throwable {
		NodeRef groupFolder = getOrCreate(primaryFolder, group);
		if(match == null || match.size() == 0){
			throw new IllegalArgumentException("match should contain at last 1 property");
		}
		HashMap<String, String[]> propertiesFiltered = properties.entrySet().stream().filter((e) -> match.contains(e.getKey())).
				collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
						(a, b) -> b, HashMap::new));
		if(propertiesFiltered.size() != match.size()){
			throw new IllegalArgumentException("match contained property names that did not exist in the properties section. Please check that you provide data for all match properties.");
		}
		properties = NodeServiceHelper.transformShortToLongProperties(properties);
		NodeRef existing = find(propertiesFiltered);
		HashMap<String, String> propertiesNative = NodeServiceHelper.getPropertiesSinglevalue(properties);
		propertiesNative.put(CCConstants.CM_NAME, NodeServiceHelper.cleanupCmName(propertiesNative.get(CCConstants.CM_NAME)) + "_" + System.currentTimeMillis());
		if(existing == null) {
				existing = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,
					NodeServiceFactory.getLocalService().createNodeBasic(
							groupFolder.getId(),
							CCConstants.getValidGlobalName(type),
							propertiesNative
					));
		}else{
			NodeServiceFactory.getLocalService().updateNodeNative(existing.getId(), propertiesNative);
		}
		if(aspects != null) {
			NodeRef finalExisting = existing;
			aspects.forEach((a) -> NodeServiceFactory.getLocalService().addAspect(finalExisting.getId(), CCConstants.getValidGlobalName(a)));
		}
		return existing;

	}
	@Override
	public NodeRef find(HashMap<String, String[]> properties) throws Exception {
		List<NodeRef> result = CMISSearchHelper.fetchNodesByTypeAndFilters(CCConstants.CCM_TYPE_IO,
				NodeServiceHelper.getPropertiesSinglevalue(
						NodeServiceHelper.transformShortToLongProperties(properties)
				));
		if(result.size()==1){
			return result.get(0);
		}else if(result.size()>1){
			throw new Exception("The given properties matched more than 1 node (" + result.size() + "). Please check your criterias and make sure they match unique data");
		}
		return null;
	}
}