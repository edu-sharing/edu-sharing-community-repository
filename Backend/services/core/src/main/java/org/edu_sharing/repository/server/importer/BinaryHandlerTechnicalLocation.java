package org.edu_sharing.repository.server.importer;

import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.AuthenticationTool;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.RepoFactory;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.w3c.dom.Node;

public class BinaryHandlerTechnicalLocation implements BinaryHandler{
	
	Logger logger = Logger.getLogger(BinaryHandlerTechnicalLocation.class);
	
	MCAlfrescoAPIClient mcAlfrescoAPIClient = null;
	
	public BinaryHandlerTechnicalLocation()  throws Throwable {
		ApplicationInfo homeRep = ApplicationInfoList.getHomeRepository();
		AuthenticationTool authTool = RepoFactory.getAuthenticationToolInstance(homeRep.getAppId());
		HashMap<String, String> authInfo = authTool.createNewSession(homeRep.getUsername(), homeRep.getPassword());
		mcAlfrescoAPIClient = (MCAlfrescoAPIClient) RepoFactory.getInstance(homeRep.getAppId(), authInfo);
	}
	
	@Override
	public void safe(String alfrescoNodeId, RecordHandlerInterfaceBase recordHandler, Node nodeRecord) {
		String technicalLocation = (String) recordHandler.getProperties().get(CCConstants.LOM_PROP_TECHNICAL_LOCATION);
		if(technicalLocation!=null)
			technicalLocation = technicalLocation.trim();
		if(technicalLocation != null && technicalLocation.startsWith("http")){
			
			try{
				URL url = new URL(technicalLocation);
				URLConnection uc = url.openConnection();		
				mcAlfrescoAPIClient.writeContent(MCAlfrescoAPIClient.storeRef, alfrescoNodeId, uc.getInputStream(), uc.getContentType(), null, CCConstants.CM_PROP_CONTENT);
			
			}catch(Throwable e){
				logger.error(e.getMessage(), e);
			}
			
		}else{
			logger.error("don't know where to get this:"+technicalLocation);
		}
	}
}
