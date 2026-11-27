package org.edu_sharing.service.remote;

import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.MCAlfrescoBaseClient;
import org.edu_sharing.repository.server.importer.PersistentHandlerEdusharing;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.nodeservice.NodeServiceImpl;
import org.edu_sharing.service.permission.PermissionServiceFactory;
import org.edu_sharing.spring.ApplicationContextFactory;
import org.springframework.context.ApplicationContext;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RemoteObjectService {

    Logger logger = Logger.getLogger(RemoteObjectService.class);

    /**
     * creates and returns remoteObjectId when it's a 3dParty repo
     * returns nodeId if its homeRepo
     *
     * @param repositoryId
     * @param nodeId
     * @return
     */
    public String getRemoteObject(String repositoryId, String nodeId) {

        return AuthenticationUtil.runAsSystem(() -> {
            try {
                ApplicationInfo repInfo = ApplicationInfoList.getRepositoryInfoById(repositoryId);
                if (is3dPartyRepository(repInfo)) {
                    logger.info("repository " + repInfo.getAppId() + " is not HomeNode and No Alfresco");

                    MCAlfrescoBaseClient mcAlfrescoBaseClient = new MCAlfrescoAPIClient();

                    String remoteObjectFolderId = getRemoteObjectsFolder();


                    Map<String, Object> searchProps = new HashMap<>();
                    searchProps.put(CCConstants.CCM_PROP_REMOTEOBJECT_NODEID, nodeId);
                    searchProps.put(CCConstants.CCM_PROP_REMOTEOBJECT_REPOSITORYID, repInfo.getAppId());
                    PersistentHandlerEdusharing phe = new PersistentHandlerEdusharing(null, null, false);
                    phe.setImportFolderId(remoteObjectFolderId);
                    NodeRef existing = phe.getNodeIfExists(CCConstants.CCM_TYPE_REMOTEOBJECT, searchProps);
                    Map<String, Object> remoteObjectProps;
                    String remoteObjectNodeId;
                    if (existing == null) {
                        logger.info("found no remote object for remote node id:" + nodeId + " creating new one");
                        // create RemoteObject as system
                        remoteObjectProps = new HashMap<>();
                        remoteObjectProps.put(CCConstants.CCM_PROP_REMOTEOBJECT_NODEID, nodeId);
                        remoteObjectProps.put(CCConstants.CCM_PROP_REMOTEOBJECT_REPOSITORY_TYPE,
                                repInfo.getRepositoryType());
                        remoteObjectProps.put(CCConstants.CCM_PROP_REMOTEOBJECT_REPOSITORYID, repInfo.getAppId());
                        String containerId = NodeServiceHelper.getContainerId(remoteObjectFolderId, "yyyy/MM/dd");
                        remoteObjectNodeId = NodeServiceFactory.getInstance().getLocalService().createNodeBasic(containerId,
                                CCConstants.CCM_TYPE_REMOTEOBJECT, remoteObjectProps);
                        NodeService nodeService = NodeServiceFactory.getInstance().getService(repInfo.getAppId());
                        InputStream content = nodeService.getContent(StoreRef.PROTOCOL_WORKSPACE,
                                StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), nodeId,
                                null, CCConstants.CM_PROP_CONTENT);
                        Map<String, Object> properties = nodeService.getPropertiesPersisting(StoreRef.PROTOCOL_WORKSPACE,
                                StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), nodeId);
                        if (content != null) {
                            // Store content from remote repo in node
                            NodeServiceFactory.getInstance().getLocalService().writeContent(
                                    StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, remoteObjectNodeId, content,
                                    (String) properties.get(CCConstants.LOM_PROP_TECHNICAL_FORMAT), "UTF-8",
                                    CCConstants.CM_PROP_CONTENT);
                        }
                    } else {
                        remoteObjectNodeId = existing.getId();
                    }

                    // CLEANUP english?
                    // read rechte für den eigentlichen user auf das remote Object
                    // setzen, damit render service das checken kann

                    String userName = (String) Context.getCurrentInstance().getRequest().getSession()
                            .getAttribute(CCConstants.AUTH_USERNAME);
                    setPermissionsIfMissing(remoteObjectNodeId, userName,
                            new String[]{CCConstants.PERMISSION_ALL, CCConstants.PERMISSION_CC_PUBLISH}, true);

                    return remoteObjectNodeId;

                } else {
                    return nodeId;
                }

            } catch (Throwable t) {
                throw new Exception(t);
            }

        });

    }

    public Map<String, Object> getRemoteObjectProperties(String repositoryId, String nodeId) {
        try {
            String tmpNodeId = getRemoteObject(repositoryId, nodeId);
            if (nodeId.equals(tmpNodeId)) {
                return NodeServiceFactory.getInstance().getService(repositoryId).getProperties(MCAlfrescoAPIClient.storeRef.getProtocol(), MCAlfrescoAPIClient.storeRef.getIdentifier(), tmpNodeId);
            } else {
                NodeServiceImpl nodeServiceImpl = ApplicationContextFactory.getApplicationContext().getBean("nodeServiceImpl", NodeServiceImpl.class);
                Map<String, Object> props = nodeServiceImpl.getProperties(MCAlfrescoAPIClient.storeRef.getProtocol(), MCAlfrescoAPIClient.storeRef.getIdentifier(), tmpNodeId);
                props.putAll(NodeServiceFactory.getInstance().getService(repositoryId).getProperties(MCAlfrescoAPIClient.storeRef.getProtocol(), MCAlfrescoAPIClient.storeRef.getIdentifier(), tmpNodeId));
                return props;
            }
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }

    private String getRemoteObjectsFolder() throws Throwable, Exception {

        MCAlfrescoAPIClient mcAlfrescoBaseClient = new MCAlfrescoAPIClient();

        String remoteObjectFolderId = null;
        String companyHomeId = mcAlfrescoBaseClient.getCompanyHomeNodeId();
        Map<String, Object> defaultRemoteFolderProps = mcAlfrescoBaseClient.getChild(companyHomeId,
                CCConstants.CCM_TYPE_MAP, CCConstants.CM_NAME, CCConstants.CC_DEFAULT_REMOTEOBJECT_FOLDER_NAME);
        if (defaultRemoteFolderProps == null) {

            Map<String, Object> newDefaultRemoteFolderProps = new HashMap<>();
            newDefaultRemoteFolderProps.put(CCConstants.CM_NAME, CCConstants.CC_DEFAULT_REMOTEOBJECT_FOLDER_NAME);
            newDefaultRemoteFolderProps.put(CCConstants.CM_PROP_C_TITLE, CCConstants.CC_DEFAULT_REMOTEOBJECT_FOLDER_NAME);
            String newRemoteFolderId = mcAlfrescoBaseClient.createNode(companyHomeId, CCConstants.CCM_TYPE_MAP, newDefaultRemoteFolderProps);

            if (newRemoteFolderId != null) {
                remoteObjectFolderId = newRemoteFolderId;
            } else {
                logger.error("Could not create default Data Folder");
            }
        } else {
            remoteObjectFolderId = (String) defaultRemoteFolderProps.get(CCConstants.SYS_PROP_NODE_UID);
        }
        if (remoteObjectFolderId != null) {
            // allow everyone to cc publish from this folder (same as for app:company_home/ccm:remote_ios)
            setPermissionsIfMissing(remoteObjectFolderId, CCConstants.AUTHORITY_GROUP_EVERYONE,
                    new String[]{CCConstants.PERMISSION_CONSUMER, CCConstants.PERMISSION_CC_PUBLISH}, false);
        }
        return remoteObjectFolderId;

    }

    /**
     * Only writes the permissions if they are not yet set directly on the node (or the inheritance differs).
     */
    private static void setPermissionsIfMissing(String nodeId, String authority, String[] permissions, boolean inheritPermissions) throws InsufficientPermissionException {
        org.edu_sharing.service.permission.PermissionService permissionService = PermissionServiceFactory.getInstance().getLocalService();
        if (inheritPermissions == permissionService.isInherited(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), nodeId)
                && permissionService.getExplicitPermissionsForAuthority(nodeId, authority).containsAll(Arrays.asList(permissions))) {
            return;
        }
        permissionService.setPermissions(nodeId, authority, permissions, inheritPermissions);
    }

    /**
     * Returns the toolpermission (TOOLPERMISSION_REPOSITORY_&lt;appId&gt;) required to use the given local node,
     * or null if the node (or, for a collection reference, its original) is no remote object.
     * Only reads the node (as system), the toolpermission itself must be checked in the context of the calling user
     */
    public static String getRemoteRepositoryToolPermission(String nodeId) {
        return AuthenticationUtil.runAsSystem(() -> {
            org.alfresco.service.cmr.repository.NodeService nodeService = ((ServiceRegistry) AlfAppContextGate.getApplicationContext()
                    .getBean(ServiceRegistry.SERVICE_REGISTRY)).getNodeService();
            NodeRef nodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId);
            if (!nodeService.exists(nodeRef)) {
                return null;
            }
            if (nodeService.hasAspect(nodeRef, QName.createQName(CCConstants.CCM_ASPECT_COLLECTION_IO_REFERENCE))) {
                String originalId = (String) nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_IO_ORIGINAL));
                if (originalId == null) {
                    return null;
                }
                nodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, originalId);
                if (!nodeService.exists(nodeRef)) {
                    return null;
                }
            }
            if (!nodeService.hasAspect(nodeRef, QName.createQName(CCConstants.CCM_ASPECT_REMOTEREPOSITORY))) {
                return null;
            }
            String remoteRepositoryId = (String) nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_REMOTEOBJECT_REPOSITORYID));
            return remoteRepositoryId == null ? null : CCConstants.CCM_VALUE_TOOLPERMISSION_REPOSITORY_PREFIX + remoteRepositoryId;
        });
    }

    private boolean is3dPartyRepository(ApplicationInfo repInfo) {
        return !repInfo.ishomeNode() && !repInfo.isRemoteAlfresco();
    }

    public static synchronized String getOrCreateRemoteMetadataObject(String sourceRepositoryId, String originalNodeId) throws Throwable {
        String ROOT_PATH = "app:company_home/ccm:remote_ios";
        ApplicationInfo repInfo = ApplicationInfoList.getRepositoryInfoById(sourceRepositoryId);
        NodeService nsSourceRepo = NodeServiceFactory.getInstance().getService(sourceRepositoryId);
		Map<String, Object> propsIn = nsSourceRepo.getPropertiesPersisting(null, null, originalNodeId);
		if(propsIn == null || propsIn.isEmpty()) {
            throw new Exception("no properties found for source nodeId:" + originalNodeId + ", appId: " + sourceRepositoryId);
        }
        String finalRemoteNodeId = propsIn.get(CCConstants.SYS_PROP_NODE_UID) == null ? originalNodeId : propsIn.get(CCConstants.SYS_PROP_NODE_UID).toString();
        if (propsIn.containsKey(CCConstants.CM_NAME)) {
            propsIn.put(CCConstants.CM_NAME,
                    NodeServiceHelper.cleanupCmName(propsIn.get(CCConstants.CM_NAME) + "_" + UUID.randomUUID())
            );
        }
        String importMds = repInfo.getString(ApplicationInfo.KEY_IMPORT_METADATASET, null);
        if (importMds != null && !importMds.isEmpty()) {
            // use the specified metadataset for imports
            propsIn.put(CCConstants.CM_PROP_METADATASET_EDU_METADATASET, importMds);
        } else {
            // set the metadataset to keep the rendering of metadata consistent
            propsIn.put(CCConstants.CM_PROP_METADATASET_EDU_METADATASET, repInfo.getMetadatsets()[0]);
        }
        // set the wwwurl so that the rendering will redirect to the source
        // @TODO: Check behaviour for each connected repository type
        // propsIn.put(CCConstants.CCM_PROP_IO_WWWURL, propsIn.get(CCConstants.LOM_PROP_TECHNICAL_LOCATION));

        // set the metadataset to keep the rendering of metadata consistent
        propsIn.put(CCConstants.CM_PROP_METADATASET_EDU_METADATASET, repInfo.getMetadatsets()[0]);
        // We also need to store repository information for remote edu-sharing objects
        propsIn.put(CCConstants.CCM_PROP_REMOTEOBJECT_NODEID, finalRemoteNodeId);
        propsIn.put(CCConstants.CCM_PROP_REMOTEOBJECT_REPOSITORY_TYPE, repInfo.getRepositoryType());
        propsIn.put(CCConstants.CCM_PROP_REMOTEOBJECT_REPOSITORYID, repInfo.getAppId());
        // remove illegal data
        Map<String, Object> props = cleanupRemoteProperties(propsIn);
        return AuthenticationUtil.runAsSystem(() -> {
            AuthenticationUtil.pushAuthentication();
            try {
                // use system do not show these files (and path) associated associated with the current user
                AuthenticationUtil.setFullyAuthenticatedUser("System");
                Map<String, Object> searchProps = new HashMap<>();
                searchProps.put(CCConstants.CCM_PROP_REMOTEOBJECT_NODEID, finalRemoteNodeId);
                searchProps.put(CCConstants.CCM_PROP_REMOTEOBJECT_REPOSITORYID, repInfo.getAppId());
                NodeService nodeService = NodeServiceFactory.getInstance().getLocalService();
                String root = NodeServiceHelper.getContainerRootPath(ROOT_PATH);
                // allow everyone to cc publish from this folder
                setPermissionsIfMissing(root, CCConstants.AUTHORITY_GROUP_EVERYONE,
                        new String[]{CCConstants.PERMISSION_CONSUMER, CCConstants.PERMISSION_CC_PUBLISH}, false);
                PersistentHandlerEdusharing phe = new PersistentHandlerEdusharing(null, null, false);
                phe.setImportFolderId(root);
                NodeRef node = phe.getNodeIfExists(searchProps);
                /*if (nodes.size() > 1) {
                    throw new Exception("For remote node " + originalNodeId + " where found " + nodes.size() + " local objects, invalid state!");
                }*/
                if (node == null) {
                    // create
                    ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
                    ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
                    BehaviourFilter policyBehaviourFilter = (BehaviourFilter) applicationContext.getBean("policyBehaviourFilter");
                    return serviceRegistry.getRetryingTransactionHelper().doInTransaction(() -> {
                        String containerId = NodeServiceHelper.getContainerIdByPath(ROOT_PATH, "yyyy/MM/dd");
                        props.put(CCConstants.CCM_PROP_IO_VERSION_COMMENT, CCConstants.VERSION_COMMENT_REMOTE_OBJECT_INIT);
                        String nodeId = nodeService.createNodeBasic(containerId, CCConstants.CCM_TYPE_IO, props);
                        NodeRef ref = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId);

                        policyBehaviourFilter.disableBehaviour(ref);
                        // admin is the owner, not the current user
                        nodeService.setOwner(nodeId, ApplicationInfoList.getHomeRepository().getUsername());
                        NodeServiceHelper.setProperty(ref, CCConstants.CM_PROP_C_CREATOR, ApplicationInfoList.getHomeRepository().getUsername(), false);
                        NodeServiceHelper.setProperty(ref, CCConstants.CM_PROP_C_MODIFIER, ApplicationInfoList.getHomeRepository().getUsername(), false);
                        nodeService.createVersion(nodeId);
                        policyBehaviourFilter.enableBehaviour(ref);
                        return nodeId;
                    });
                } else {
                    // update in case metadata of remote source have changed
                    nodeService.updateNodeNative(node.getId(), props);
                    return node.getId();
                }
            } catch (Throwable t) {
                throw new RuntimeException(t);
            } finally {
                AuthenticationUtil.popAuthentication();
            }
        });
    }

    public static Map<String, Object> cleanupRemoteProperties(Map<String, Object> propsIn) {
        Map<String, Object> props = new HashMap<>(propsIn);
        props.remove(CCConstants.SYS_PROP_NODE_UID);
        props.remove(CCConstants.NODECREATOR_EMAIL);
        props.remove(CCConstants.NODECREATOR_FIRSTNAME);
        props.remove(CCConstants.NODECREATOR_LASTNAME);
        props.remove(CCConstants.NODEMODIFIER_EMAIL);
        props.remove(CCConstants.NODEMODIFIER_FIRSTNAME);
        props.remove(CCConstants.NODEMODIFIER_LASTNAME);
        props.remove(CCConstants.CONTENTURL);
        for (Map.Entry<String, Object> prop : propsIn.entrySet()) {
            if (prop.getKey().startsWith("{virtualproperty}")) {
                props.remove(prop.getKey());
            }
        }
        return props;
    }
}
