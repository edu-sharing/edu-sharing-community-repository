package org.edu_sharing.repository.server.tools;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoBaseClient;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;

public class NodeTool {

	public String createOrGetNodeByName(MCAlfrescoBaseClient client, String nodeId, String[] path) throws Throwable {

		if (path.length > 0) {
			
			String name = path[0];
			
			NodeRef child = NodeServiceFactory.getLocalService().getChild(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId, CCConstants.CCM_TYPE_MAP, CCConstants.CM_NAME, name);
			
			if (child == null) {
				HashMap<String, Serializable> _props = new HashMap<>();
				_props.put(CCConstants.CM_NAME, name);
				nodeId = NodeServiceFactory.getLocalService().createNodeBasic(nodeId, CCConstants.CCM_TYPE_MAP, _props);
			} else {
				nodeId = child.getId();
			}

			return createOrGetNodeByName(client, nodeId, Arrays.copyOfRange(path,  1, path.length));
		}
		
		return nodeId;
		
	}
	
}
