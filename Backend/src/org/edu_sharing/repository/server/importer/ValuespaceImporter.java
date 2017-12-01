package org.edu_sharing.repository.server.importer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import org.edu_sharing.repository.client.rpc.metadataset.MetadataSet;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetBaseProperty;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetFormsForm;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetFormsPanel;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetFormsProperty;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetValueKatalog;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSets;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.AuthenticationTool;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.MCAlfrescoBaseClient;
import org.edu_sharing.repository.server.RepoFactory;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.UserEnvironmentTool;
import org.edu_sharing.service.suggest.SuggestDAOSearchImpl;

public class ValuespaceImporter {

	private final ApplicationInfo appInfo;
	
	private final AuthenticationTool authTool;
	
	private final String locale;
	
	private static int LAST_HASH_CODE = 0;
	
	public ValuespaceImporter(String locale) throws Throwable {
		
		this.appInfo  = ApplicationInfoList.getHomeRepository();			
		this.authTool = RepoFactory.getAuthenticationToolInstance(appInfo.getAppId());
		
		this.locale = locale;
	}
	
	public void run() throws Throwable {
		
		MetadataSets mdss = RepoFactory.getRepositoryMetadataSets().get(appInfo.getAppId());
		
		if (LAST_HASH_CODE == mdss.hashCode()) {
			return;
		} 
		
		LAST_HASH_CODE = mdss.hashCode();
				
		HashMap<String, String> authInfo = 
				authTool.createNewSession(
						appInfo.getUsername(), 
						appInfo.getPassword());
		
		MCAlfrescoAPIClient mcAlfrescoBaseClient = 
				(MCAlfrescoAPIClient) RepoFactory.getInstance(appInfo.getAppId(), authInfo);
		
		for (MetadataSet mds : mdss.getMetadataSets()) {
			
			for (MetadataSetFormsForm mdsForm : mds.getMetadataSetForms()) {
			
				for (MetadataSetFormsPanel panel : mdsForm.getPanels()) {

					for (MetadataSetFormsProperty prop : panel.getProperties()) {
						
						if (! SuggestDAOSearchImpl.class.getName().equals(
								prop.getParam(MetadataSetBaseProperty.PARAM_SUGGESTBOX_DAO))) {
							
							continue;
						}
						
						Set<String> newValues = new HashSet<String>();
						for (MetadataSetValueKatalog valuespace : prop.getValuespace()) {
							
							String value = valuespace.getValue(this.locale); 

							if (! value.trim().isEmpty()) {
								newValues.add(value);
							}
						}
						
						String folderId = getPropFolder(mcAlfrescoBaseClient, prop);
													
						Set<String> toRemoveNodeIds = new HashSet<String>();
						
						for (Entry<String, HashMap<String, Object>> entry : 
							mcAlfrescoBaseClient.getChildren(
									folderId, prop.getType()).entrySet()) {
						
							String oldValue = entry.getValue().get(prop.getName()).toString();
							
							if (! newValues.remove(oldValue))  {
								
								toRemoveNodeIds.add(entry.getKey());
							}
						}

						for (String toRemoveNodeId : toRemoveNodeIds) {
							
							mcAlfrescoBaseClient.removeNode(toRemoveNodeId, folderId);
						}
							
						for (String newValue : newValues) {
							
							String name = escapeName(newValue);
					
							HashMap<String, Object> nodeProps = new HashMap<String, Object>();
							nodeProps.put(
									CCConstants.CM_NAME, 
									name);
																							
							nodeProps.put(
									CCConstants.CM_PROP_METADATASET_EDU_METADATASET, 
									CCConstants.metadatasetsystem_id);
							
							nodeProps.put(prop.getName(), newValue);
							
							mcAlfrescoBaseClient.createNode(
									folderId, 
									prop.getType(), 
									nodeProps);
						}
					}
				}				
			}
		}
	}	
	
	private String getPropFolder(MCAlfrescoBaseClient mcAlfrescoBaseClient, MetadataSetFormsProperty prop) throws Throwable {
		
		UserEnvironmentTool uet = new UserEnvironmentTool(appInfo.getUsername());
		String valueSpaceFolderId = uet.getEdu_SharingValuespaceFolder();
		
		return getFolder(
				mcAlfrescoBaseClient, 
				getFolder(mcAlfrescoBaseClient, valueSpaceFolderId, escapeName(prop.getType())), 
				escapeName(prop.getName()));
	}

	private String getFolder(MCAlfrescoBaseClient mcAlfrescoBaseClient, String parentId, String childName) throws Throwable {
		
		HashMap<String, Object> typeProps = 
				mcAlfrescoBaseClient.getChild(
						parentId, 
						CCConstants.CCM_TYPE_MAP, 
						CCConstants.CM_NAME, 
						childName);
		
		if (typeProps == null) {
			
			typeProps = new HashMap<String, Object>();
			
			typeProps.put(
					CCConstants.CM_NAME, 
					childName);
			
			typeProps.put(
					CCConstants.CM_PROP_METADATASET_EDU_METADATASET, 
					CCConstants.metadatasetsystem_id);
			
			return mcAlfrescoBaseClient.createNode(
						parentId, 
						CCConstants.CCM_TYPE_MAP, 
						typeProps);								
		} 
		
		return typeProps.get(CCConstants.SYS_PROP_NODE_UID).toString();
	}

	private String escapeName(String name) {
		
		return name.replaceAll("[^a-zA-Z0-9]", "_");
	}
}
