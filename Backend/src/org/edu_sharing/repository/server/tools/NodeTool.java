package org.edu_sharing.repository.server.tools;

import java.util.Arrays;
import java.util.HashMap;

import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoBaseClient;

public class NodeTool {

	public String createOrGetNodeByName(MCAlfrescoBaseClient client, String nodeId, String[] path) throws Throwable {

		if (path.length > 0) {
			
			String name = path[0];
			
			HashMap<String, Object> child = client.getChild(nodeId, CCConstants.CCM_TYPE_MAP, CCConstants.CM_NAME, name);
			
			if (child == null) {
				
				HashMap<String, Object> _props = new HashMap<String, Object>();
				_props.put(CCConstants.CM_NAME, name);
				
				nodeId = client.createNode(nodeId, CCConstants.CCM_TYPE_MAP, _props);
				
			} else {
				
				nodeId = child.get(CCConstants.SYS_PROP_NODE_UID).toString();
			}

			return createOrGetNodeByName(client, nodeId, Arrays.copyOfRange(path,  1, path.length));
		}
		
		return nodeId;
		
	}
	
}
