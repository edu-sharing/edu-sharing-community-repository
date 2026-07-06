package org.edu_sharing.service.model;

import java.util.*;

import lombok.Data;
import org.alfresco.service.cmr.repository.StoreRef;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.restservices.shared.Contributor;

@Data
public class NodeRefImpl implements NodeRef {

    @Data
    public static class PreviewImpl implements Preview {
        private final String type;
        private final Boolean icon;

        private String mimetype;
        private byte[] data;

        public PreviewImpl(String mimetype, byte[] data, String type, Boolean icon) {
            this.mimetype = mimetype;
            this.data = data;
            this.type = type;
            this.icon = icon;
        }
    }

    private String repositoryId;
    private Origin origin;
    private List<CollectionRef> usedInCollections = new ArrayList<>();
    private Map<Relation, NodeRef> relations = new HashMap<>();

    private String storeProtocol;
    private String storeId;
    private String nodeId;
    private String type;
    private Preview preview;
    private Map<String, Object> properties;
    private Map<String, Boolean> permissions;

    /**
     * is this node ref publicly accessible, e.g. shared with "GROUP_EVERYONE"
     */
    private Boolean isPublic;
    private List<String> aspects;

    private String owner;
    private List<Contributor> contributors;
    private List<NodeRef> children = new ArrayList<>();

    public NodeRefImpl() {}

    public NodeRefImpl(String nodeId) {
        this.storeProtocol = StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getProtocol();
        this.storeId = StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier();
        this.nodeId = nodeId;
    }

    public NodeRefImpl(org.alfresco.service.cmr.repository.NodeRef nodeRef) {
        this.nodeId = nodeRef.getId();
        this.storeId = nodeRef.getStoreRef().getIdentifier();
        this.storeProtocol = nodeRef.getStoreRef().getProtocol();
    }

    public NodeRefImpl(String repositoryId, String storeProtocol, String storeId, String nodeId) {
        this.repositoryId = repositoryId;
        this.storeId = storeId;
        this.storeProtocol = storeProtocol;
        this.nodeId = nodeId;
    }

    public NodeRefImpl(String repositoryId, String storeProtocol, String storeId, Map<String, Object> properties) {
        this.repositoryId = repositoryId;
        this.storeId = storeId;
        this.storeProtocol = storeProtocol;
        this.nodeId = (String) properties.get(CCConstants.SYS_PROP_NODE_UID);
        this.properties = properties;
    }

    public enum Relation {
        Original
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodeRefImpl nodeRef = (NodeRefImpl) o;
        return Objects.equals(storeProtocol, nodeRef.storeProtocol) && Objects.equals(storeId, nodeRef.storeId) && Objects.equals(nodeId, nodeRef.nodeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(storeProtocol, storeId, nodeId);
    }
}
