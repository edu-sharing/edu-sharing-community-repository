package org.edu_sharing.service.lifecycle;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;

import java.util.HashMap;

public class Utils {

    public NodeRef getNodeRef(String parentId, String type, String name) throws Throwable {
        NodeRef protocolNodeRef = lookup(parentId,type, name);
        if(protocolNodeRef == null){
            HashMap<String, String[]> props = new HashMap<>();
            props.put(CCConstants.CM_NAME, new String[]{name});
            String nodeId = NodeServiceFactory.getInstance().getLocalService().createNode(parentId,type, props);
            protocolNodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,nodeId);
        }
        return protocolNodeRef;
    }

    public boolean exists(String parentId, String type, String name) {
        boolean exists = false;
        if (lookup(parentId,type, name) != null) {
            exists = true;
        }
        return exists;
    }

    private NodeRef lookup(String parentId, String type, String name) {
        NodeRef protocolNodeRef = NodeServiceFactory.getInstance().getLocalService().getChild(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,parentId, type,CCConstants.CM_NAME, name);
        return protocolNodeRef;
    }
}
