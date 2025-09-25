package org.edu_sharing.service.collection;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.edu_sharing.repository.client.rpc.ACE;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.service.model.NodeRef;
import org.edu_sharing.service.search.model.SortDefinition;

import java.io.InputStream;
import java.util.List;

public class CollectionServiceAdapter implements CollectionService {

    @Override
    public Collection create(String collectionId, Collection collection) throws Throwable {
        return null;
    }

    @Override
    public void update(Collection collection) {

    }

    @Override
    public void remove(String collectionId) {

    }

    @Override
    public List<AssociationRef> getChildrenProposal(String parentId) {
        return List.of();
    }

    @Override
    public void proposeForCollection(String collectionId, String originalNodeId, String sourceRepositoryId) {

    }

    @Override
    public String addToCollection(String collectionId, String originalNodeId, String sourceRepositoryId, boolean allowDuplicate) throws Throwable {
        return "";
    }

    @Override
    public void removeFromCollection(String collectionId, String nodeId) {

    }

    @Override
    public void move(String toCollection, String nodeId) {

    }

    @Override
    public List<NodeRef> getChildren(String parentId, String scope, SortDefinition sortDefinition, List<String> filter) {
        return List.of();
    }

    @Override
    public List<NodeRef> getRecentForCurrentUser() {
        return List.of();
    }

    @Override
    public SearchResultNodeRef getRoot(String scope, SortDefinition sortDefinition, int skipCount, int maxItems) {
        return null;
    }

    @Override
    public Collection get(NodeRef collection, boolean fetchCounts, boolean resolveUsernames, BoolQuery readPermissionsQuery) {
        return null;
    }

    @Override
    public void removePreviewImage(String collectionId) {

    }

    @Override
    public void setOrder(String parentId, String[] nodes) {

    }

    @Override
    public void updateAndSetScope(Collection collection) {

    }

    @Override
    public String getCollectionHomeParent() {
        return "";
    }

    @Override
    public String getHomePath() {
        return "";
    }

    @Override
    public Collection createAndSetScope(String parentId, Collection collection) {
        return null;
    }

    @Override
    public void updateScope(org.alfresco.service.cmr.repository.NodeRef ref, List<ACE> permissions) {

    }

    @Override
    public void setPinned(String[] collections) {

    }

    @Override
    public void writePreviewImage(String collectionId, InputStream is, String mimeType) {

    }

    @Override
    public List<NodeRef> getReferenceObjects(String nodeId) {
        return List.of();
    }

    @Override
    public List<org.alfresco.service.cmr.repository.NodeRef> getReferenceObjectsSync(String nodeId) {
        return List.of();
    }

    @Override
    public List<org.alfresco.service.cmr.repository.NodeRef> getCollectionProposals(String nodeId, CCConstants.PROPOSAL_STATUS status) {
        return List.of();
    }
}
