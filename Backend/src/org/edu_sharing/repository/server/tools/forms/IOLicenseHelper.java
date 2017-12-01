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
package org.edu_sharing.repository.server.tools.forms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.alfresco.service.cmr.security.PermissionService;
import org.apache.commons.fileupload.FileItem;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetModelProperty;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.forms.CCForms;
import org.edu_sharing.repository.server.MCAlfrescoBaseClient;
import org.edu_sharing.repository.server.RepoFactory;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.StringTool;
import org.edu_sharing.service.permission.PermissionServiceFactory;
import org.edu_sharing.service.toolpermission.ToolPermissionService;
import org.edu_sharing.service.toolpermission.ToolPermissionServiceFactory;

public class IOLicenseHelper extends HelperAbstract {

	Logger logger = Logger.getLogger(IOLicenseHelper.class);

	@Override
	public HashMap<String, Object> execute(HashMap<String, Object> params, HashMap<String, String> authenticatioInfo) {
		logger.info("starting");

		String nodeId = (String) params.get(CCConstants.NODEID);
		String repositoryId = (String) params.get(CCConstants.REPOSITORY_ID);
		List items = (List) params.get("ITEMS");
		FileItem fileItem = getFileItem(
				CCForms.getFormEleNameByProp(CCConstants.CCM_TYPE_IO, CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY), items);

		FileItem ccVersion = getFileItem(CCForms.getFormEleNameByProp(CCConstants.CCM_TYPE_IO, CCConstants.CCM_PROP_IO_COMMONLICENSE_CC_VERSION), items);
		FileItem ccLanguage = getFileItem(CCForms.getFormEleNameByProp(CCConstants.CCM_TYPE_IO, CCConstants.CCM_PROP_IO_COMMONLICENSE_CC_LOCALE), items);
		if (fileItem != null) {
			String licenseKey = StringTool.getEncoded(fileItem.get());
			
			FileItem releaseFI = getFileItem(CCConstants.AUTHORITY_GROUP_EVERYONE,items);
			Boolean release = null;
			
			if(releaseFI != null){
				String releaseStr = StringTool.getEncoded(releaseFI.get());
				if(releaseStr != null && (releaseStr.equals("on") || new Boolean(releaseStr))){
					release = true;
				}else{
					release = false;
				}
				
			}else{
				release = false;
			}
			
			logger.info("licenseKey: " + licenseKey);
			try {
				MCAlfrescoBaseClient mcAlfrescoBaseClient = (MCAlfrescoBaseClient) RepoFactory.getInstance(
						repositoryId, authenticatioInfo);
					
				ToolPermissionService tps = ToolPermissionServiceFactory.getInstance();
				if(!tps.hasToolPermission(CCConstants.CCM_VALUE_TOOLPERMISSION_LICENSE)){
					return null;
				}

				//only if user is allowed to change permissions
				if (mcAlfrescoBaseClient.hasPermissions(nodeId, new String[] { PermissionService.CHANGE_PERMISSIONS })) {
					
					//set licenses here and not in metadataset so we can check for changepermission permission
					
						mcAlfrescoBaseClient.setProperty(nodeId, CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY, new ServletValueTool().getValue(fileItem, MetadataSetModelProperty.DATATYPE_STRING));
					
						
						String ccVersionStr = ccVersion.getString();
						if(ccVersionStr != null && !ccVersionStr.equals("")){
							mcAlfrescoBaseClient.setProperty(nodeId, CCConstants.CCM_PROP_IO_COMMONLICENSE_CC_VERSION, new ServletValueTool().getValue(ccVersion, MetadataSetModelProperty.DATATYPE_STRING));
						}
						
						String ccLanguageStr = (ccLanguage != null) ? ccLanguage.getString() : null;
						if(ccLanguageStr != null && !ccLanguageStr.trim().equals("")){
							mcAlfrescoBaseClient.setProperty(nodeId, CCConstants.CCM_PROP_IO_COMMONLICENSE_CC_LOCALE, new ServletValueTool().getValue(ccLanguage, MetadataSetModelProperty.DATATYPE_STRING));
						}
					{
						FileItem fi = getFileItem(CCForms.getFormEleNameByProp(CCConstants.CCM_TYPE_IO, CCConstants.CCM_PROP_IO_COMMONLICENSE_QUESTIONSALLOWED), items);
						
						String value = new ServletValueTool().getValue(fi, MetadataSetModelProperty.DATATYPE_STRING);
						mcAlfrescoBaseClient.setProperty(nodeId, CCConstants.CCM_PROP_IO_COMMONLICENSE_QUESTIONSALLOWED, value);
					}
					
					{
						FileItem fi = getFileItem(CCForms.getFormEleNameByProp(CCConstants.CCM_TYPE_IO, CCConstants.LOM_PROP_RIGHTS_RIGHTS_DESCRIPTION), items);
						mcAlfrescoBaseClient.setProperty(nodeId, CCConstants.LOM_PROP_RIGHTS_RIGHTS_DESCRIPTION, new ServletValueTool().getValue(fi, MetadataSetModelProperty.DATATYPE_STRING));
					}
					
					{
						FileItem fi = getFileItem(CCForms.getFormEleNameByProp(CCConstants.CCM_TYPE_IO, CCConstants.CCM_PROP_IO_LICENSE_TITLE_OF_WORK), items);
						mcAlfrescoBaseClient.setProperty(nodeId, CCConstants.CCM_PROP_IO_LICENSE_TITLE_OF_WORK, new ServletValueTool().getValue(fi, MetadataSetModelProperty.DATATYPE_STRING));
					}
					
					{
						FileItem fi = getFileItem(CCForms.getFormEleNameByProp(CCConstants.CCM_TYPE_IO, CCConstants.CCM_PROP_IO_LICENSE_SOURCE_URL), items);
						mcAlfrescoBaseClient.setProperty(nodeId, CCConstants.CCM_PROP_IO_LICENSE_SOURCE_URL, new ServletValueTool().getValue(fi, MetadataSetModelProperty.DATATYPE_STRING));
					}
					
					{
						FileItem fi = getFileItem(CCForms.getFormEleNameByProp(CCConstants.CCM_TYPE_IO, CCConstants.CCM_PROP_IO_LICENSE_PROFILE_URL), items);
						mcAlfrescoBaseClient.setProperty(nodeId, CCConstants.CCM_PROP_IO_LICENSE_PROFILE_URL, new ServletValueTool().getValue(fi, MetadataSetModelProperty.DATATYPE_STRING));
					}
					
					
					/**
					 * check if to handle the ALL_AUTHORITIES permission
					 */
					FileItem fiControlRelease = getFileItem(CCConstants.VIRT_PROP_CONTROLRELEASE, items);
					if(fiControlRelease != null){
						
						String controlRelease = StringTool.getEncoded(fiControlRelease.get());
						logger.info("controlRelease:"+controlRelease);
						if(controlRelease.equals("false")){
							return null;
						}
					}
				
					
					if (licenseKey.trim().equals("")) {
						logger.info("no license");						
						logger.info("will remove rights");
						try{
							org.edu_sharing.service.permission.PermissionService permissionService = PermissionServiceFactory.getPermissionService(ApplicationInfoList.getHomeRepository().getAppId());
							
							
							permissionService.removePermissions(nodeId, PermissionService.ALL_AUTHORITIES, new String[] {
								CCConstants.PERMISSION_READ, CCConstants.PERMISSION_CONSUMER, CCConstants.PERMISSION_CC_PUBLISH });
						}catch(java.lang.IllegalStateException e){
							if(!"Can not delete from this acl in a node context SHARED".equals(e.getMessage())){
								logger.error(e.getMessage(),e);
							}
						}

					} else {

						// Creative Commons License
						ArrayList<String> permToSet = new ArrayList<String>();

						ArrayList<String> permToRemove = new ArrayList<String>();
						if (licenseKey.contains(CCConstants.COMMON_LICENSE_CC_BY)) {
							
							if(release){
								permToSet.add(CCConstants.PERMISSION_READ);
								permToSet.add(CCConstants.PERMISSION_CC_PUBLISH);
							}else{
								permToRemove.add(CCConstants.PERMISSION_READ);
								permToRemove.add(CCConstants.PERMISSION_CONSUMER);
								permToRemove.add(CCConstants.PERMISSION_CC_PUBLISH);
							}
									
						} else if (licenseKey.contains(CCConstants.COMMON_LICENSE_EDU_NC)) {
							permToSet.add(CCConstants.PERMISSION_READ);
							permToSet.add(CCConstants.PERMISSION_CC_PUBLISH);
						} else if (licenseKey.contains(CCConstants.COMMON_LICENSE_EDU_P_NR)) {
							permToSet.add(CCConstants.PERMISSION_READ);
							permToRemove.add(CCConstants.PERMISSION_CC_PUBLISH);
						} else if (licenseKey.contains(CCConstants.COMMON_LICENSE_CUSTOM)) {
							permToSet.add(CCConstants.PERMISSION_READ);
							permToRemove.add(CCConstants.PERMISSION_CC_PUBLISH);
						}

						org.edu_sharing.service.permission.PermissionService permissionService = PermissionServiceFactory.getPermissionService(repositoryId);
						
						if (permToSet.size() > 0) {
							permissionService.setPermissions(nodeId, PermissionService.ALL_AUTHORITIES,
									permToSet.toArray(new String[permToSet.size()]), null);
						}

						if (permToRemove.size() > 0) {
							permissionService.removePermissions(nodeId, PermissionService.ALL_AUTHORITIES,
									permToRemove.toArray(new String[permToRemove.size()]));
						}
					}
				}
			} catch (Throwable e) {
				logger.error(e.getMessage(), e);
			}
		}
		return null;
	}
}
