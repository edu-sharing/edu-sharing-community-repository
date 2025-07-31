package org.edu_sharing.service.lifecycle;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;

import java.util.HashMap;

public class Utils {

    public NodeRef getNodeRef(String parentId, String type, String name) throws Throwable {
        NodeRef protocolNodeRef = lookup(parentId, name);
        if(protocolNodeRef == null){
            HashMap<String, String[]> props = new HashMap<>();
            props.put(CCConstants.CM_NAME, new String[]{name});
            String nodeId = NodeServiceFactory.getLocalService().createNode(parentId,type, props);
            protocolNodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,nodeId);
        }
        return protocolNodeRef;
    }

    public boolean exists(String parentId, String name) {
        boolean exists = false;
        if (lookup(parentId, name) != null) {
            exists = true;
        }
        return exists;
    }

    private NodeRef lookup(String parentId, String name) {
        NodeRef protocolNodeRef = NodeServiceFactory.getLocalService().getChild(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,parentId, CCConstants.CCM_TYPE_IO,CCConstants.CM_NAME, name);
        return protocolNodeRef;
    }
}
