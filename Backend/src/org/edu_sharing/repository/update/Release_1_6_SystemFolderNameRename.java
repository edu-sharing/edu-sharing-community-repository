package org.edu_sharing.repository.update;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.security.AuthorityType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.MCAlfrescoBaseClient;
import org.edu_sharing.repository.server.tools.I18nServer;
import org.edu_sharing.repository.server.tools.forms.DuplicateFinder;
import org.springframework.context.ApplicationContext;

public class Release_1_6_SystemFolderNameRename implements Update {

	public static final String ID = "Release_1_6_SystemFolderNameRename";
	
	public static final String description = "UPDATE AUF 1.6. Renames \"Meine Favoriten\", \"Meine Gruppen\", \"Meine Dateien\" to an i18n value corresponding to the locale set in user.language and user.country. if not set en_EN will be used. Attention SystemFolderNameToDisplayName update must be done before!";
	
	private static Log logger = LogFactory.getLog(Release_1_6_SystemFolderNameRename.class);
	
	PrintWriter out = null;
	MCAlfrescoBaseClient mcAlfrescoBaseClient = null;
	
	ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
	ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
	
	int counter = 0;
	
	public Release_1_6_SystemFolderNameRename(PrintWriter out) {
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
	public void test() {
		run(true);
	}
	
	@Override
	public String getDescription() {
		return description;
	}
	
	@Override
	public String getId() {
		return ID;
	}
	
	public void run(boolean test) {
		logger.info("test:" + test);
		out.println("test:" + test);
		try {

			Set<String> users = serviceRegistry.getAuthorityService().getAllAuthorities(AuthorityType.USER);
			for (String user : users) {
				
				logger.info("processing user:"+user);
				out.println("processing user:"+user);
				String homefolderId = mcAlfrescoBaseClient.getHomeFolderID(user);
				HashMap<String, HashMap<String, Object>> children = mcAlfrescoBaseClient.getChildren(homefolderId);
				for (Map.Entry<String, HashMap<String, Object>> entry : children.entrySet()) {
					String mapType = (String) entry.getValue().get(CCConstants.CCM_PROP_MAP_TYPE);
					String folderId = (String) entry.getValue().get(CCConstants.SYS_PROP_NODE_UID);
					if(mapType != null){
						if(mapType.equals(CCConstants.CCM_VALUE_MAP_TYPE_DOCUMENTS)){
							updateFolder(folderId,children, CCConstants.I18n_USERFOLDER_DOCUMENTS, test );
						}
						
						if(mapType.equals(CCConstants.CCM_VALUE_MAP_TYPE_FAVORITE)){
							updateFolder(folderId,children, CCConstants.I18n_USERFOLDER_FAVORITES, test );
						}
													  
						if(mapType.equals(CCConstants.CCM_VALUE_MAP_TYPE_EDUGROUP)){
							updateFolder(folderId,children, CCConstants.I18n_USERFOLDER_GROUPS, test );
						}
					}
				}
			}
		}catch (Throwable e) {
			logger.error(e.getMessage(), e);
			out.println(e.getMessage());
		}
	}
	
	private void updateFolder(String folderId, HashMap suroundingNodes, String i8nKey, boolean test) throws Exception {

		HashMap newProps = new HashMap();
		
		//get an alfresco installation locale corresponding name 
		String displayName = I18nServer.getTranslationDefaultResourcebundle(i8nKey);

		String uniqueValue = new DuplicateFinder().getUniqueValue(suroundingNodes, folderId, CCConstants.CM_NAME, displayName);
		newProps.put(CCConstants.CM_NAME, uniqueValue);
		
		HashMap i18nTitle = new HashMap();
		i18nTitle.put("de_DE", I18nServer.getTranslationDefaultResourcebundle(i8nKey, "de_DE"));
		i18nTitle.put("en_EN", I18nServer.getTranslationDefaultResourcebundle(i8nKey, "en_EN"));
		i18nTitle.put("en_US", I18nServer.getTranslationDefaultResourcebundle(i8nKey, "en_US"));
		newProps.put(CCConstants.CM_PROP_C_TITLE, i18nTitle);
		

		logger.info("folderId:" + folderId + " setting uniqueValue for name:" + uniqueValue);
		out.println("folderId:" + folderId + " setting uniqueValue for name:" + uniqueValue);
		if (!test) {
			mcAlfrescoBaseClient.updateNode(folderId, newProps);
		}
		counter++;
	}
	
}
