package org.edu_sharing.repository.server;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tracking.TrackingEvent;
import org.edu_sharing.repository.client.tracking.TrackingEvent.ACTIVITY;
import org.edu_sharing.repository.client.tracking.TrackingEvent.CONTEXT_ITEM;
import org.edu_sharing.repository.client.tracking.TrackingEvent.PLACE;
import org.edu_sharing.repository.server.tracking.TrackingService;

public aspect TrackingUpdate {
	
	Logger logger = Logger.getLogger(TrackingUpdate.class);
	
	
	pointcut nodeUpdate(CCUpdate ccUpdate, String nodeID, String nodeType): target(ccUpdate) && execution(void CCUpdate.processClass(String, String)) && args(nodeID,nodeType);
	
	after(CCUpdate ccUpdate, String nodeID, String nodeType) returning: nodeUpdate(ccUpdate, nodeID, nodeType){
		
		String repositoryId = ccUpdate.repositoryId;
		String user = ccUpdate.authInfo.get(CCConstants.AUTH_USERNAME);
		
		if(nodeType.equals(CCConstants.CCM_TYPE_IO) || nodeType.equals(CCConstants.CCM_TYPE_MAP)){
			
			ACTIVITY activity = ACTIVITY.NODE_UPDATE;
			PLACE place = PLACE.UPLOAD;
			
			ArrayList<CONTEXT_ITEM> contextList = new ArrayList<CONTEXT_ITEM>();
			contextList.add(new CONTEXT_ITEM(TrackingEvent.CONTEXT.REPOSITORY_ID, repositoryId));
			contextList.add(new CONTEXT_ITEM(TrackingEvent.CONTEXT.NODE_TYPE, nodeType));
			contextList.add(new CONTEXT_ITEM(TrackingEvent.CONTEXT.CONTENT_REFERENCE, nodeID));
			//contextList.add(new CONTEXT_ITEM(TrackingEvent.CONTEXT.USER_ID, user));
			
			TrackingService.track(activity, contextList.toArray(new CONTEXT_ITEM[contextList.size()]), place, ccUpdate.authInfo);
		}else{
			logger.debug("will not log " + nodeType);
		}
		
	}
	
	/**
	 * Node update Tracking
	 * 
	 * @param repClient
	 * @param nodeId
	 * @param _props
	 */
	/*pointcut nodeUpdate(MCAlfrescoBaseClient repClient, String nodeId, HashMap<String, Object> _props): target(repClient) && args(nodeId, _props) && execution(void MCAlfrescoBaseClient.updateNode(String, HashMap<String, Object>));
	
	after(MCAlfrescoBaseClient repClient, String nodeId, HashMap<String, Object> _props) returning: nodeUpdate(repClient, nodeId, _props){
		logger.info("starting nodeId:"+nodeId);
		
		String nodeType = null;
		try{
			HashMap<String, Object> tmpProps = repClient.getProperties(nodeId);
			nodeType = (String)tmpProps.get(CCConstants.NODETYPE);
		}catch(Throwable e){
			logger.error(e.getMessage(), e);
		}
		logger.info("nodeType:"+nodeType);
		if(nodeType != null && (nodeType.equals(CCConstants.CCM_TYPE_MAP) || nodeType.equals(CCConstants.CCM_TYPE_IO))){
			String userName = null;
			String repId = null;
			HashMap<String, String> authInfo = null;
			if(repClient instanceof MCAlfrescoAPIClient){
				userName = ((MCAlfrescoAPIClient)repClient).authenticationInfo.get(CCConstants.AUTH_USERNAME);
				authInfo = ((MCAlfrescoAPIClient)repClient).authenticationInfo;
				repId = ((MCAlfrescoAPIClient)repClient).repId;
				logger.info("its an API Client");
			}else if(repClient instanceof MCAlfrescoWSClient){
				userName = ((MCAlfrescoWSClient)repClient).authenticationInfo.get(CCConstants.AUTH_USERNAME);
				authInfo = ((MCAlfrescoWSClient)repClient).authenticationInfo;
				repId =  (((MCAlfrescoWSClient)repClient).initialRepInfo != null)? ((MCAlfrescoWSClient)repClient).initialRepInfo.getAppId() : null;
				logger.info("its an WS Client");
			}
			
			ACTIVITY activity = ACTIVITY.NODE_UPDATE;
			
			PLACE place = PLACE.UPLOAD;
			
			ArrayList<CONTEXT_ITEM> contextList = new ArrayList<CONTEXT_ITEM>();
			contextList.add(new CONTEXT_ITEM(TrackingEvent.CONTEXT.REPOSITORY_ID, repId));
			contextList.add(new CONTEXT_ITEM(TrackingEvent.CONTEXT.CONTENT_REFERENCE, nodeId));
			contextList.add(new CONTEXT_ITEM(TrackingEvent.CONTEXT.NODE_TYPE, nodeType));
			//if(userName != null){
			//	contextList.add(new CONTEXT_ITEM(TrackingEvent.CONTEXT.USER_ID, userName));
			//}
			
			TrackingService.track(activity, contextList.toArray(new CONTEXT_ITEM[contextList.size()]), place, authInfo);
		}else{
			logger.debug("will not log " + nodeType);
		}
		logger.info("returning");
	}*/
}
