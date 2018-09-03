package org.edu_sharing.service.nodeservice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.RepoFactory;
import org.edu_sharing.repository.server.tools.NameSpaceTool;
import org.edu_sharing.service.search.model.SortDefinition;

public class NodeServiceHelper {
	/**
	 * Clean the CM_NAME property so it does not cause an org.alfresco.repo.node.integrity.IntegrityException
	 * @param cmNameReadableName
	 * @return
	 */
	public static String cleanupCmName(String cmNameReadableName){
		// replace chars that can lead to an
		// org.alfresco.repo.node.integrity.IntegrityException
		cmNameReadableName = cmNameReadableName.replaceAll(
			RepoFactory.getEdusharingProperty(CCConstants.EDU_SHARING_PROPERTIES_PROPERTY_VALIDATOR_REGEX_CM_NAME), "_");

		//replace ending dot with nothing
		//cmNameReadableName = cmNameReadableName.replaceAll("\\.$", "");
		cmNameReadableName = cmNameReadableName.replaceAll("[\\.]*$", "").trim();
		return cmNameReadableName;
	}

	/**
	 * enable or disable the create version for the node
	 * Note:Only works for local nodes!
	 */
	public static void setCreateVersion(String nodeId, boolean create) {
		new MCAlfrescoAPIClient().setProperty(nodeId, CCConstants.CCM_PROP_IO_CREATE_VERSION, create);
	}

	public static HashMap<String, String[]> transformShortToLongProperties(HashMap<String, String[]> properties) {

		/**
		 * shortNames to long names
		 */
		HashMap<String,String[]>  propsLongKeys = (HashMap<String,String[]>)new NameSpaceTool<String[]>()
				.transformKeysToLongQname(properties);
		
		HashMap<String, String[]> result = new HashMap<String, String[]>();

		for (Map.Entry<String,String[]> property : propsLongKeys.entrySet()) {
			if(result.containsKey(property.getKey())) continue;
			
			result.put(property.getKey(), property.getValue());
		}

		return result;
	}

	public static List<Map<String,Object>> getSubobjects(NodeService service, String nodeId) throws Throwable {
		List<Map<String,Object>> result = new ArrayList<>();
		List<String> filter=new ArrayList<>();
		filter.add("files");
		SortDefinition sort=new SortDefinition();
		sort.addSortDefinitionEntry(new SortDefinition.SortDefinitionEntry(CCConstants.getValidLocalName(CCConstants.CCM_PROP_CHILDOBJECT_ORDER),true));
		sort.addSortDefinitionEntry(new SortDefinition.SortDefinitionEntry(CCConstants.getValidLocalName(CCConstants.CM_NAME),true));
		List<ChildAssociationRef> childs=service.getChildrenChildAssociationRefAssoc(nodeId,null,filter,sort);
		for(ChildAssociationRef child : childs) {
			NodeRef ref = child.getChildRef();
			HashMap<String, Object> props = service.getProperties(ref.getStoreRef().getProtocol(),ref.getStoreRef().getIdentifier(),ref.getId());
			result.add(props);
		}
		return result;		
	}
}
