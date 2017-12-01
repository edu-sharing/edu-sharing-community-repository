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

public aspect TrackingCreate {
	
	Logger logger = Logger.getLogger(TrackingCreate.class);
	
	
	pointcut nodeCreate(CCCreate ccCreate, String parentID, String nodeType, String association): target(ccCreate) && args(parentID,nodeType,association) && execution(void CCCreate.processClass(String,String,String));
	
	after(CCCreate ccCreate, String parentID, String nodeType, String association) returning: nodeCreate(ccCreate,parentID,nodeType,association){
		
		String user = ccCreate.username;
		String repositoryId = ccCreate.repositoryId;
		String ticket = ccCreate.ticket;
		
		logger.info("user:"+user+" nodeType:"+nodeType +" repositoryId:"+repositoryId+" ticket:"+ticket);
		
		if(nodeType.equals(CCConstants.CCM_TYPE_IO) || nodeType.equals(CCConstants.CCM_TYPE_MAP)){
			
			ACTIVITY activity = ACTIVITY.NODE_CREATE;
			PLACE place = PLACE.UPLOAD;
			
			ArrayList<CONTEXT_ITEM> contextList = new ArrayList<CONTEXT_ITEM>();
			contextList.add(new CONTEXT_ITEM(TrackingEvent.CONTEXT.REPOSITORY_ID, repositoryId));
			contextList.add(new CONTEXT_ITEM(TrackingEvent.CONTEXT.NODE_TYPE, nodeType));
			//contextList.add(new CONTEXT_ITEM(TrackingEvent.CONTEXT.CONTENT_REFERENCE, (String)_props.get(CCConstants.SYS_PROP_NODE_UID)));
			//contextList.add(new CONTEXT_ITEM(TrackingEvent.CONTEXT.USER_ID, user));
			
			HashMap<String,String> authInfo = new HashMap<String,String>();
			authInfo.put(CCConstants.AUTH_TICKET, ticket);
			authInfo.put(CCConstants.AUTH_USERNAME, user);
			
			TrackingService.track(activity, contextList.toArray(new CONTEXT_ITEM[contextList.size()]), place, authInfo);
		}else{
			logger.debug("will not log " + nodeType);
		}
		
		
	}
	
	/**
	 * Node create Tracking
	 * @param repClient
	 * @param nodeId
	 * @param _props
	 */
	/*pointcut nodeCreate(MCAlfrescoBaseClient repClient, String parentID, String nodeTypeString, HashMap<String, Object> _props): target(repClient) && args(parentID, nodeTypeString, _props) && execution(String MCAlfrescoBaseClient.createNode(String, String, HashMap<String, Object>));
	
	after(MCAlfrescoBaseClient repClient, String parentID, String nodeTypeString, HashMap<String, Object> _props) returning: nodeCreate(repClient, parentID, nodeTypeString, _props){
		logger.info("starting parentID:"+parentID +" nodeType:"+nodeTypeString);
		
		if(nodeTypeString.equals(CCConstants.CCM_TYPE_IO) || nodeTypeString.equals(CCConstants.CCM_TYPE_MAP)){
		
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
			
			ACTIVITY activity = ACTIVITY.NODE_CREATE;
			PLACE place = PLACE.UPLOAD;
			
			ArrayList<CONTEXT_ITEM> contextList = new ArrayList<CONTEXT_ITEM>();
			contextList.add(new CONTEXT_ITEM(TrackingEvent.CONTEXT.REPOSITORY_ID, (String)_props.get(CCConstants.REPOSITORY_ID)));
			contextList.add(new CONTEXT_ITEM(TrackingEvent.CONTEXT.NODE_TYPE, nodeTypeString));
			//contextList.add(new CONTEXT_ITEM(TrackingEvent.CONTEXT.CONTENT_REFERENCE, (String)_props.get(CCConstants.SYS_PROP_NODE_UID)));
			//if(userName != null){
			//	contextList.add(new CONTEXT_ITEM(TrackingEvent.CONTEXT.USER_ID, userName));
			//}
			TrackingService.track(activity, contextList.toArray(new CONTEXT_ITEM[contextList.size()]), place, authInfo);
		}else{
			logger.debug("will not log " + nodeTypeString);
		}
		logger.info("returning");
		
	}*/
	
}
