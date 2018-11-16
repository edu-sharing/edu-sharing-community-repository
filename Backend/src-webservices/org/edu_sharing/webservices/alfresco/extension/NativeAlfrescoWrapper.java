/**
 * NativeAlfrescoWrapper.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package org.edu_sharing.webservices.alfresco.extension;

public interface NativeAlfrescoWrapper extends java.rmi.Remote {
    public void setProperty(java.lang.String nodeId, java.lang.String property, java.lang.String value) throws java.rmi.RemoteException;
    public java.lang.String getProperty(java.lang.String storeProtocol, java.lang.String storeIdentifier, java.lang.String nodeId, java.lang.String property) throws java.rmi.RemoteException;
    public java.util.HashMap getProperties(java.lang.String nodeId) throws java.rmi.RemoteException;
    public org.edu_sharing.repository.client.rpc.ACL getPermissions(java.lang.String nodeId) throws java.rmi.RemoteException;
    public java.lang.String getType(java.lang.String nodeId) throws java.rmi.RemoteException;
    public java.lang.String getPath(java.lang.String nodeID) throws java.rmi.RemoteException;
    public java.util.HashMap getUserInfo(java.lang.String userName) throws java.rmi.RemoteException;
    public org.edu_sharing.webservices.alfresco.extension.SearchResult search(org.edu_sharing.webservices.alfresco.extension.SearchCriteria[] searchCriterias, java.lang.String metadatasetId, int start, int nrOfResults, java.lang.String[] facettes) throws java.rmi.RemoteException;
    public void removeNode(java.lang.String nodeId, java.lang.String fromId) throws java.rmi.RemoteException;
    public void copyNode(java.lang.String nodeId, java.lang.String toNodeId, boolean copyChildren) throws java.rmi.RemoteException;
    public java.util.HashMap getChildren(java.lang.String parentID, java.lang.String type) throws java.rmi.RemoteException;
    public java.util.HashMap getChild(java.lang.String parentId, java.lang.String type, java.lang.String property, java.lang.String value) throws java.rmi.RemoteException;
    public java.util.HashMap getPropertiesExt(java.lang.String storeProtocol, java.lang.String storeId, java.lang.String nodeId) throws java.rmi.RemoteException;
    public org.edu_sharing.webservices.alfresco.extension.SearchResult searchSolr(java.lang.String query, int startIdx, int nrOfresults, java.lang.String[] facettes, int facettesMinCount, int facettesLimit) throws java.rmi.RemoteException;
    public java.util.HashMap getChildrenCheckPermissions(java.lang.String parentID, java.lang.String[] permissionsOnChild) throws java.rmi.RemoteException;
    public java.lang.String createNode(java.lang.String parentID, java.lang.String nodeTypeString, java.lang.String childAssociation, java.util.HashMap props) throws java.rmi.RemoteException;
    public java.lang.String createNodeAtomicValues(java.lang.String parentID, java.lang.String nodeTypeString, java.lang.String childAssociation, java.util.HashMap props) throws java.rmi.RemoteException;
    public void updateNodeAtomicValues(java.lang.String nodeId, java.util.HashMap properties) throws java.rmi.RemoteException;
    public boolean isAdmin(java.lang.String username) throws java.rmi.RemoteException;
    public java.util.HashMap hasPermissions(java.lang.String userId, java.lang.String[] permissions, java.lang.String nodeId) throws java.rmi.RemoteException;
    public boolean hasPermissionsSimple(java.lang.String nodeId, java.lang.String[] permissions) throws java.rmi.RemoteException;
    public void updateNode(java.lang.String nodeId, java.util.HashMap properties) throws java.rmi.RemoteException;
    public java.lang.String getCompanyHomeNodeId() throws java.rmi.RemoteException;
    public java.util.HashMap getPropertiesSimple(java.lang.String nodeId) throws java.rmi.RemoteException;
    public java.lang.String[] searchNodeIds(java.lang.String store, java.lang.String luceneQuery, java.lang.String permission) throws java.rmi.RemoteException;
    public org.edu_sharing.webservices.alfresco.extension.RepositoryNode[] searchNodes(java.lang.String store, java.lang.String luceneQuery, java.lang.String permission, java.lang.String[] propertiesToReturn) throws java.rmi.RemoteException;
    public java.lang.String validateTicket(java.lang.String ticket) throws java.rmi.RemoteException;
    public org.edu_sharing.webservices.alfresco.extension.RepositoryNode[] getVersionHistory(java.lang.String nodeId) throws java.rmi.RemoteException;
    public org.edu_sharing.service.nodeservice.model.GetPreviewResult getPreviewUrl(java.lang.String storeProtocol, java.lang.String storeIdentifier, java.lang.String nodeId) throws java.rmi.RemoteException;
    public void createShare(java.lang.String nodeId, java.lang.String[] emails, long expiryDate) throws java.rmi.RemoteException;
    public org.edu_sharing.repository.client.rpc.Share[] getShares(java.lang.String nodeId) throws java.rmi.RemoteException;
    public boolean isOwner(java.lang.String nodeId, java.lang.String user) throws java.rmi.RemoteException;
    public java.lang.String[] getMetadataSets() throws java.rmi.RemoteException;
    public org.edu_sharing.webservices.alfresco.extension.SearchResult findGroups(java.lang.String searchWord, java.lang.String eduGroupNodeId, int from, int nrOfResults) throws java.rmi.RemoteException;
    public org.edu_sharing.webservices.alfresco.extension.SearchResult findUsers(org.edu_sharing.webservices.alfresco.extension.KeyValue[] searchProps, java.lang.String eduGroupNodeId, int from, int nrOfResults) throws java.rmi.RemoteException;
    public org.edu_sharing.webservices.alfresco.extension.KeyValue[] getEduGroupContextOfNode(java.lang.String nodeId) throws java.rmi.RemoteException;
    public boolean hasToolPermission(java.lang.String toolPermission) throws java.rmi.RemoteException;
    public void setUserDefinedPreview(java.lang.String nodeId, byte[] content, java.lang.String fileName) throws java.rmi.RemoteException;
    public void removeUserDefinedPreview(java.lang.String nodeId) throws java.rmi.RemoteException;
    public java.lang.String guessMimetype(java.lang.String filename) throws java.rmi.RemoteException;
    public java.lang.String[] searchNodeIdsLimit(java.lang.String luceneString, int limit) throws java.rmi.RemoteException;
    public void removeAspect(java.lang.String nodeId, java.lang.String aspect) throws java.rmi.RemoteException;
    public void removeGlobalAspectFromGroup(java.lang.String groupNodeId) throws java.rmi.RemoteException;
    public org.edu_sharing.repository.client.rpc.Notify[] getNotifyList(java.lang.String nodeId) throws java.rmi.RemoteException;
    public void revertVersion(java.lang.String nodeId, java.lang.String verLbl) throws java.rmi.RemoteException;
    public void createVersion(java.lang.String nodeId, java.util.HashMap properties) throws java.rmi.RemoteException;
    public java.util.HashMap hasAllPermissions(java.lang.String nodeId, java.lang.String[] permissions) throws java.rmi.RemoteException;
    public java.util.HashMap hasAllPermissionsExt(java.lang.String storeProtocol, java.lang.String storeId, java.lang.String nodeId, java.lang.String[] permissions) throws java.rmi.RemoteException;
    public java.lang.String getHomeFolderID(java.lang.String username) throws java.rmi.RemoteException;
    public void addPermissionACEs(java.lang.String nodeId, org.edu_sharing.repository.client.rpc.ACE[] aces) throws java.rmi.RemoteException;
    public void removePermissionACEs(java.lang.String nodeId, org.edu_sharing.repository.client.rpc.ACE[] aces) throws java.rmi.RemoteException;
    public void setPermissionsBasic(java.lang.String nodeId, java.lang.String _authority, java.lang.String[] permissions, boolean changeInherit, boolean inheritPermission) throws java.rmi.RemoteException;
    public void removePermissions(java.lang.String nodeId, java.lang.String _authority, java.lang.String[] _permissions) throws java.rmi.RemoteException;
    public void executeAction(java.lang.String nodeId, java.lang.String actionName, java.lang.String actionId, java.util.HashMap parameters, boolean async) throws java.rmi.RemoteException;
    public void createAssociation(java.lang.String fromID, java.lang.String toID, java.lang.String association) throws java.rmi.RemoteException;
    public void createChildAssociation(java.lang.String from, java.lang.String to, java.lang.String assocType, java.lang.String assocName) throws java.rmi.RemoteException;
    public void moveNode(java.lang.String newParentId, java.lang.String childAssocType, java.lang.String nodeId) throws java.rmi.RemoteException;
    public void removeAssociation(java.lang.String fromID, java.lang.String toID, java.lang.String association) throws java.rmi.RemoteException;
    public void removeRelationsForNode(java.lang.String nodeId, java.lang.String nodeParentId) throws java.rmi.RemoteException;
    public void removeRelations(java.lang.String parentID) throws java.rmi.RemoteException;
    public void addAspect(java.lang.String nodeId, java.lang.String aspect) throws java.rmi.RemoteException;
    public java.lang.String getGroupFolderId() throws java.rmi.RemoteException;
    public java.lang.String getRepositoryRoot() throws java.rmi.RemoteException;
    public java.util.HashMap getChildenByProps(java.lang.String parentId, java.lang.String type, java.util.HashMap props) throws java.rmi.RemoteException;
    public java.util.HashMap getChildrenByType(java.lang.String nodeId, java.lang.String type) throws java.rmi.RemoteException;
    public java.util.HashMap getChildrenByAssociation(java.lang.String storeString, java.lang.String nodeId, java.lang.String association) throws java.rmi.RemoteException;
    public java.util.HashMap getParents(java.lang.String nodeID, boolean primary) throws java.rmi.RemoteException;
    public java.util.HashMap getAssocNode(java.lang.String nodeID, java.lang.String association) throws java.rmi.RemoteException;
    public java.util.HashMap getChildRecursive(java.lang.String parentId, java.lang.String type, java.util.HashMap props) throws java.rmi.RemoteException;
    public java.util.HashMap getChildrenRecursive(java.lang.String parentId, java.lang.String type) throws java.rmi.RemoteException;
    public java.lang.String[] getAssociationNodeIds(java.lang.String nodeID, java.lang.String association) throws java.rmi.RemoteException;
    public java.lang.String[] getUserNames() throws java.rmi.RemoteException;
    public org.edu_sharing.webservices.alfresco.extension.UserDetails[] getUserDetails(java.lang.String[] userNames) throws java.rmi.RemoteException;
    public void setUserDetails(org.edu_sharing.webservices.alfresco.extension.UserDetails[] userDetails) throws java.rmi.RemoteException;
    public void deleteUser(java.lang.String[] userNames) throws java.rmi.RemoteException;
    public java.lang.String[] getGroupNames() throws java.rmi.RemoteException;
    public org.edu_sharing.webservices.alfresco.extension.GroupDetails[] getGroupDetails(java.lang.String[] groupNames) throws java.rmi.RemoteException;
    public org.edu_sharing.webservices.alfresco.extension.GroupDetails setGroupDetails(org.edu_sharing.webservices.alfresco.extension.GroupDetails groupDetails) throws java.rmi.RemoteException;
    public void deleteGroup(java.lang.String[] groupNames) throws java.rmi.RemoteException;
    public java.lang.String[] getMemberships(java.lang.String groupName) throws java.rmi.RemoteException;
    public void addMemberships(java.lang.String groupName, java.lang.String[] members) throws java.rmi.RemoteException;
    public void removeMemberships(java.lang.String groupName, java.lang.String[] members) throws java.rmi.RemoteException;
    public void removeAllMemberships(java.lang.String[] groupNames) throws java.rmi.RemoteException;
    public boolean isSubOf(java.lang.String type, java.lang.String parentType) throws java.rmi.RemoteException;
    public boolean hasContent(java.lang.String nodeId, java.lang.String contentProp) throws java.rmi.RemoteException;
    public void writeContent(java.lang.String nodeID, byte[] content, java.lang.String mimetype, java.lang.String encoding, java.lang.String property) throws java.rmi.RemoteException;
    public void setUserPassword(java.lang.String userName, java.lang.String password) throws java.rmi.RemoteException;
    public void invalideTicket(java.lang.String ticket) throws java.rmi.RemoteException;
    public void bindEduGroupFolder(java.lang.String groupName, java.lang.String folderId) throws java.rmi.RemoteException;
    public java.lang.String findNodeByPath(java.lang.String path) throws java.rmi.RemoteException;
    public java.lang.String[] getAspects(java.lang.String storeProtocol, java.lang.String storeId, java.lang.String nodeId) throws java.rmi.RemoteException;
    public void setOwner(java.lang.String nodeId, java.lang.String username) throws java.rmi.RemoteException;
    public void setPermissions(java.lang.String nodeId, org.edu_sharing.repository.client.rpc.ACE[] aces) throws java.rmi.RemoteException;
    public void removeChild(java.lang.String parentID, java.lang.String childID, java.lang.String association) throws java.rmi.RemoteException;
}
