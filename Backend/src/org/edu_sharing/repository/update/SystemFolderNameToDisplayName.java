/**
 *
 *  
 * 
 * 
 *	
 *
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 *
 */
package org.edu_sharing.repository.update;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.MCAlfrescoBaseClient;
import org.edu_sharing.repository.server.tools.I18nServer;
import org.edu_sharing.repository.server.tools.forms.DuplicateFinder;
import org.springframework.context.ApplicationContext;

public class SystemFolderNameToDisplayName implements Update {

	public static final String ID = "SystemFolderNameToDisplayName";

	public static final String description = "UPDATE AUF 1.4.0. System Ordner des Users (Favoritenordner, Meine Gruppen, Meine Dateien) bekommen ein lesbares CM_NAME Property. Dieses Update muss durchgeführt werden bevor User das System Nutzen sonst werden neue Systemordner angelegt.";
	
	private static Log logger = LogFactory.getLog(SystemFolderNameToDisplayName.class);

	ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
	ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
	
	NodeService nodeService = serviceRegistry.getNodeService();

	MCAlfrescoBaseClient mcAlfrescoBaseClient = null;

	int counter = 0;

	PrintWriter out = null;
	public SystemFolderNameToDisplayName(PrintWriter out) {
		this.out = out;
		try {
			mcAlfrescoBaseClient = new MCAlfrescoAPIClient();
		} catch (Throwable e) {
			logger.error(e.getMessage(), e);
			out.println(e.getMessage());
		}
	}

	@Override
	public void execute() {
		run(false);
	}

	@Override
	public String getId() {
		return ID;
	}
	
	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public void test() {
		run(true);
	}

	public void run(boolean test) {
		logger.info("test:" + test);
		out.println("test:" + test);
		try {

			Set<String> users = serviceRegistry.getAuthorityService().getAllAuthorities(AuthorityType.USER);
			for (String user : users) {

				String homefolderId = mcAlfrescoBaseClient.getHomeFolderID(user);

				
				HashMap<String, HashMap<String, Object>> children = mcAlfrescoBaseClient.getChildren(homefolderId);
				for (Map.Entry<String, HashMap<String, Object>> entry : children.entrySet()) {
					String name = (String) entry.getValue().get(CCConstants.CM_NAME);
					String type = (String) entry.getValue().get(CCConstants.NODETYPE);
					String folderId = (String) entry.getValue().get(CCConstants.SYS_PROP_NODE_UID);
					if (type.equals(CCConstants.CCM_TYPE_MAP) && name.equals(CCConstants.BASKETS_PARENT_PREFIX + user)) {

						logger.info("updateing user:" + user + " folderId:" + folderId + " folderName" + name);
						out.println("updateing user:" + user + " folderId:" + folderId + " folderName" + name);
						
						//get an alfresco installation locale corresponding name
						String userFavoritesFolderName = I18nServer.getTranslationDefaultResourcebundle(CCConstants.I18n_USERFOLDER_FAVORITES);
						updateFolder(folderId, children, CCConstants.CCM_VALUE_MAP_TYPE_FAVORITE, userFavoritesFolderName, test);

					}

					if (type.equals(CCConstants.CM_TYPE_FOLDER) && name.equals(CCConstants.CC_DEFAULT_USER_DATA_FOLDER_NAME)) {
						logger.info("updateing user:" + user + " folderId:" + folderId + " folderName" + name);
						out.println("updateing user:" + user + " folderId:" + folderId + " folderName" + name);
						//nodeService.getS
						logger.info("setting type to Map for folderId:"+folderId);
						out.println("setting type to Map for folderId:"+folderId);
						if(!test){
							serviceRegistry.getNodeService().setType(new NodeRef(new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore"),folderId), QName.createQName(CCConstants.CCM_TYPE_MAP));
						}
						
						updateFolder(folderId, children, CCConstants.CCM_VALUE_MAP_TYPE_DOCUMENTS, I18nServer.getTranslationDefaultResourcebundle(CCConstants.I18n_USERFOLDER_DOCUMENTS), test);
					}
					
					
					if (type.equals(CCConstants.CCM_TYPE_MAP) && name.equals("DEFAULTGROUPSFOLDER")) {
						logger.info("updateing user:" + user + " folderId:" + folderId + " folderName" + name);
						
						//get an alfresco installation locale corresponding name
						String userGroupsFolderName = I18nServer.getTranslationDefaultResourcebundle(CCConstants.I18n_USERFOLDER_GROUPS);
						updateFolder(folderId, children, CCConstants.CCM_VALUE_MAP_TYPE_EDUGROUP, userGroupsFolderName, test);
					}

				}

			}
			
		} catch (Throwable e) {
			logger.error(e.getMessage(), e);
			out.println(e.getMessage());
		}
		logger.info("nr of updated objects:" + counter);
		out.println("nr of updated objects:" + counter);
	}

	private void updateFolder(String folderId, HashMap suroundingNodes, String mapType, String displayName, boolean test) throws Exception {

		HashMap newProps = new HashMap();

		String uniqueValue = new DuplicateFinder().getUniqueValue(suroundingNodes, folderId, CCConstants.CM_NAME, displayName);
		newProps.put(CCConstants.CM_NAME, uniqueValue);
		newProps.put(CCConstants.CM_PROP_C_TITLE, uniqueValue);
		newProps.put(CCConstants.CCM_PROP_MAP_TYPE, mapType);

		logger.info("folderId:" + folderId + " setting uniqueValue for title and name:" + uniqueValue + " and " + CCConstants.CCM_PROP_MAP_TYPE + " " + newProps.get(CCConstants.CCM_PROP_MAP_TYPE));
		out.println("folderId:" + folderId + " setting uniqueValue for title and name:" + uniqueValue + " and " + CCConstants.CCM_PROP_MAP_TYPE + " " + newProps.get(CCConstants.CCM_PROP_MAP_TYPE));
		if (!test) {
			mcAlfrescoBaseClient.updateNode(folderId, newProps);
		}
		counter++;
	}
}
