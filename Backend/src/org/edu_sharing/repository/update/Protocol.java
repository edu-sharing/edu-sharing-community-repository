package org.edu_sharing.repository.update;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;

import org.alfresco.model.ContentModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.MCAlfrescoBaseClient;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.UserEnvironmentTool;
import org.springframework.context.ApplicationContext;

public class Protocol {
	
	ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
	ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
	
	MCAlfrescoBaseClient mcAlfrescoBaseClient = new MCAlfrescoAPIClient();
	
	private static Log logger = LogFactory.getLog(Protocol.class);
	
	public HashMap<String,Object> getSysUpdateEntry(String updaterId) throws Throwable{
		
		HashMap<String,String> authInfo = new HashMap<String,String>();
		authInfo.put(CCConstants.AUTH_USERNAME, serviceRegistry.getAuthenticationService().getCurrentUserName());
		authInfo.put(CCConstants.AUTH_TICKET, serviceRegistry.getAuthenticationService().getCurrentTicket());
		String eduSystemFolderUpdate = null;
		HashMap<String,Object> updateInfo = null;
		
		eduSystemFolderUpdate = new UserEnvironmentTool(ApplicationInfoList.getHomeRepository().getAppId(), authInfo).getEdu_SharingSystemFolderUpdate();
		updateInfo = mcAlfrescoBaseClient.getChild(eduSystemFolderUpdate, CCConstants.CCM_TYPE_SYSUPDATE, CCConstants.CCM_PROP_SYSUPDATE_ID, updaterId);
			
		
		return updateInfo;
	}
	
	public void writeSysUpdateEntry(String updaterId) throws Throwable{
		HashMap<QName,Serializable> updateInfoProps = new HashMap<QName,Serializable>();
		
		updateInfoProps.put(ContentModel.PROP_NAME, updaterId);
		updateInfoProps.put(QName.createQName(CCConstants.CCM_PROP_SYSUPDATE_ID),updaterId);
		updateInfoProps.put(QName.createQName(CCConstants.CCM_PROP_SYSUPDATE_DATE),new Date());
		
		NodeService nodeService = serviceRegistry.getNodeService();
		HashMap<String,String> authInfo = new HashMap<String,String>();
		authInfo.put(CCConstants.AUTH_USERNAME, serviceRegistry.getAuthenticationService().getCurrentUserName());
		authInfo.put(CCConstants.AUTH_TICKET, serviceRegistry.getAuthenticationService().getCurrentTicket());
		
		String eduSystemFolderUpdate = new UserEnvironmentTool(ApplicationInfoList.getHomeRepository().getAppId(), authInfo).getEdu_SharingSystemFolderUpdate();
		nodeService.createNode(new NodeRef(MCAlfrescoAPIClient.storeRef,eduSystemFolderUpdate), QName.createQName(CCConstants.CM_ASSOC_FOLDER_CONTAINS),QName.createQName(updaterId), QName.createQName(CCConstants.CCM_TYPE_SYSUPDATE), updateInfoProps);
	}
}
