package org.edu_sharing.service.model;

import org.alfresco.service.cmr.repository.StoreRef;
import org.edu_sharing.restservices.shared.Contributor;

import java.util.List;
import java.util.Map;

public interface NodeRef {

    String getOwner();

    String getType();
    default org.alfresco.service.cmr.repository.NodeRef asAlfrescoNodeRef() {
        return new org.alfresco.service.cmr.repository.NodeRef(new StoreRef(
                getStoreProtocol(), getStoreId()
        ), getNodeId());
    }

    Origin getOrigin();

    void setOrigin(Origin origin);

    void setOwner(String owner);

    Map<NodeRefImpl.Relation, NodeRef> getRelations();

    void setRelations(Map<NodeRefImpl.Relation, NodeRef> relations);

    void setContributors(List<Contributor> contributors);

    List<Contributor> getContributors();

    void setChildren(List<NodeRef> children);

    List<NodeRef> getChildren();

    interface Preview {
        String getMimetype();

        byte[] getData();

        String getType();

        Boolean getIcon();
    }

    String getRepositoryId();

    void setRepositoryId(String repositoryId);

    String getStoreProtocol();

    void setStoreProtocol(String storeProtocol);

    String getStoreId();

    void setStoreId(String storeId);

    String getNodeId();

    void setNodeId(String nodeId);

    Map<String, Object> getProperties();

    void setProperties(Map<String, Object> properties);

    void setPreview(Preview preview);

    Preview getPreview();

    Map<String, Boolean> getPermissions();

    void setPermissions(Map<String, Boolean> permissions);

    Boolean getIsPublic();

    void setIsPublic(Boolean aPublic);

    void setAspects(List<String> aspects);

    List<String> getAspects();

    void setUsedInCollections(List<CollectionRef> usedInCollections);

    List<CollectionRef> getUsedInCollections();

    /**
     * origin this ref was fetched from
     */
    enum Origin {
        Alfresco,
        Elasticsearch,
    }
}
