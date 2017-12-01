package org.edu_sharing.service.remote;

import java.io.InputStream;
import java.util.HashMap;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.MCAlfrescoBaseClient;
import org.edu_sharing.repository.server.authentication.Context;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceImpl;
import org.edu_sharing.service.permission.PermissionServiceFactory;

public class RemoteObjectService {

	Logger logger = Logger.getLogger(RemoteObjectService.class);

	/**
	 * creates and returns remoteObjectId when it's a 3dParty repo
	 * returns nodeId if its homeRepo
	 * @param repositoryId
	 * @param nodeId
	 * @return
	 */
	public String getRemoteObject(String repositoryId, String nodeId) {

		return AuthenticationUtil.runAsSystem(new RunAsWork<String>() {

			@Override
			public String doWork() throws Exception {
				try {
					ApplicationInfo repInfo = ApplicationInfoList.getRepositoryInfoById(repositoryId);
					if (is3dPartyRepository(repInfo)) {
						logger.info("repository " + repInfo.getAppId() + "is not HomeNode and No Alfresco");

						MCAlfrescoBaseClient mcAlfrescoBaseClient = new MCAlfrescoAPIClient();

						org.edu_sharing.service.permission.PermissionService permissionService = PermissionServiceFactory
								.getPermissionService(ApplicationInfoList.getHomeRepository().getAppId());
						String remoteObjectFolderId = null;
						remoteObjectFolderId = getRemoteObjectsFolder();
		
						// schau nach ob remote object schon existiert
						mcAlfrescoBaseClient.setResolveRemoteObjects(false);
						HashMap<String, Object> remoteObjectProps = mcAlfrescoBaseClient.getChild(remoteObjectFolderId,
								CCConstants.CCM_TYPE_REMOTEOBJECT, CCConstants.CCM_PROP_REMOTEOBJECT_NODEID, nodeId);

						String remoteObjectNodeId = null;
						if (remoteObjectProps == null) {
							logger.info("found no remote object for remote node id:" + nodeId + " creating new one");
							// RemoteObject erstellen als admin
							remoteObjectProps = new HashMap<String, Object>();
							remoteObjectProps.put(CCConstants.CCM_PROP_REMOTEOBJECT_NODEID, nodeId);
							remoteObjectProps.put(CCConstants.CCM_PROP_REMOTEOBJECT_REPOSITORY_TYPE,
									repInfo.getRepositoryType());
							remoteObjectProps.put(CCConstants.CCM_PROP_REMOTEOBJECT_REPOSITORYID, repInfo.getAppId());

							remoteObjectNodeId = mcAlfrescoBaseClient.createNode(remoteObjectFolderId,
									CCConstants.CCM_TYPE_REMOTEOBJECT, remoteObjectProps);
							NodeService nodeService = NodeServiceFactory.getNodeService(repInfo.getAppId());
							InputStream content = nodeService.getContent(StoreRef.PROTOCOL_WORKSPACE,
									StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), nodeId,
									CCConstants.CM_PROP_CONTENT);
							HashMap<String, Object> properties = nodeService.getProperties(StoreRef.PROTOCOL_WORKSPACE,
									StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), nodeId);
							if (content != null) {
								// Store content from remote repo in node
								NodeServiceFactory.getLocalService().writeContent(
										StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, remoteObjectNodeId, content,
										(String) properties.get(CCConstants.LOM_PROP_TECHNICAL_FORMAT), "UTF-8",
										CCConstants.CM_PROP_CONTENT);
							}
						} else {
							remoteObjectNodeId = (String) remoteObjectProps.get(CCConstants.SYS_PROP_NODE_UID);
						}

						// CLEANUP english?
						// read rechte für den eigentlichen user auf das remote Object
						// setzen, damit render service das checken kann

						String userName = (String) Context.getCurrentInstance().getRequest().getSession()
								.getAttribute(CCConstants.AUTH_USERNAME);
						permissionService.setPermissions(remoteObjectNodeId, userName,
								new String[] { CCConstants.PERMISSION_ALL, CCConstants.PERMISSION_CC_PUBLISH }, true);

						return remoteObjectNodeId;

					} else {
						return nodeId;
					}

				} catch (Throwable t) {
					throw new Exception(t);
				}

			}
		});

	}
	
	public HashMap<String,Object> getRemoteObjectProperties(String repositoryId, String nodeId){
		try {
			String tmpNodeId = getRemoteObject(repositoryId, nodeId);
			if(nodeId.equals(tmpNodeId)) {
				return NodeServiceFactory.getNodeService(repositoryId).getProperties(MCAlfrescoAPIClient.storeRef.getProtocol(), MCAlfrescoAPIClient.storeRef.getIdentifier(), tmpNodeId);	
			}else {
				HashMap<String,Object> props = new NodeServiceImpl(repositoryId).getProperties(MCAlfrescoAPIClient.storeRef.getProtocol(), MCAlfrescoAPIClient.storeRef.getIdentifier(), tmpNodeId);
				props.putAll(NodeServiceFactory.getNodeService(repositoryId).getProperties(MCAlfrescoAPIClient.storeRef.getProtocol(), MCAlfrescoAPIClient.storeRef.getIdentifier(), tmpNodeId));
				return props;
			}
		}catch(Throwable e) {
			logger.error(e.getMessage(), e);
			return null;
		}
	}

	private String getRemoteObjectsFolder() throws Throwable, Exception {

		MCAlfrescoAPIClient mcAlfrescoBaseClient = new MCAlfrescoAPIClient();

		String remoteObjectFolderId = null;
		String companyHomeId = mcAlfrescoBaseClient.getCompanyHomeNodeId();
		HashMap<String, Object> defaultRemoteFolderProps = mcAlfrescoBaseClient.getChild(companyHomeId,
				CCConstants.CCM_TYPE_MAP, CCConstants.CM_NAME, CCConstants.CC_DEFAULT_REMOTEOBJECT_FOLDER_NAME);
		if (defaultRemoteFolderProps == null) {

			HashMap newDefaultRemoteFolderProps = new HashMap();
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
		return remoteObjectFolderId;

	}

	private boolean is3dPartyRepository(ApplicationInfo repInfo) {
		return !repInfo.ishomeNode() && !ApplicationInfo.REPOSITORY_TYPE_ALFRESCO.equals(repInfo.getRepositoryType());
	}

}
