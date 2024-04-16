package org.edu_sharing.repository.server;


import org.alfresco.service.cmr.repository.NodeRef;

public class NodeRefVersion {
    private NodeRef nodeRef;
    private String version;

    public NodeRefVersion(NodeRef nodeRef, String version) {
        this.nodeRef = nodeRef;
        this.version = version;
    }

    public NodeRef getNodeRef() {
        return nodeRef;
    }

    public void setNodeRef(NodeRef nodeRef) {
        this.nodeRef = nodeRef;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
