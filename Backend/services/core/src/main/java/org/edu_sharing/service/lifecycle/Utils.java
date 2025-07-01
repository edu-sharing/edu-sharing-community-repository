package org.edu_sharing.service.lifecycle;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;

import java.util.HashMap;

public class Utils {

    public NodeRef getNodeRef(String parentId, String name) throws Throwable {
        NodeRef parent = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,parentId);
        NodeRef protocolNodeRef = NodeServiceFactory.getLocalService().getChild(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,parent.getId(), CCConstants.CCM_TYPE_IO,CCConstants.CM_NAME, name);
        if(protocolNodeRef == null){
            HashMap<String, String[]> props = new HashMap<>();
            props.put(CCConstants.CM_NAME, new String[]{name});
            String nodeId = NodeServiceFactory.getLocalService().createNode(parentId,CCConstants.CCM_TYPE_IO, props);
            protocolNodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,nodeId);
        }
        return protocolNodeRef;
    }
}
