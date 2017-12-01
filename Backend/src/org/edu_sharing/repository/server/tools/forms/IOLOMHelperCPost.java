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

import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.forms.VCardTool;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.MCAlfrescoBaseClient;
import org.edu_sharing.repository.server.RepoFactory;

public class IOLOMHelperCPost extends HelperAbstract{
	
	Logger logger = Logger.getLogger(IOLOMHelperCPost.class);
	
	public HashMap<String, Object> execute(HashMap<String, Object> params, HashMap<String, String> authenticatioInfo) {

		final String nodeId = (String)params.get(CCConstants.NODEID);
		String repId = (String)params.get(CCConstants.REPOSITORY_ID);
		String nodeType = (String)params.get(CCConstants.NODETYPE);
		if(nodeType == null || !nodeType.equals(CCConstants.CCM_TYPE_IO)) return null;
		
		try{
			MCAlfrescoBaseClient mcbaseClient = (MCAlfrescoBaseClient)RepoFactory.getInstance(repId, authenticatioInfo);
			
			HashMap<String,Object> ioProps = new HashMap<String,Object>();
		
			//Contributer stuff
			String creator = mcbaseClient.getProperty(MCAlfrescoAPIClient.storeRef.getProtocol(), MCAlfrescoAPIClient.storeRef.getIdentifier(), nodeId, CCConstants.CM_PROP_C_CREATOR);
			if(creator != null){
					
				//creator as author
				HashMap<String,String> userInfo = mcbaseClient.getUserInfo(creator);
				String givenName = userInfo.get(CCConstants.PROP_USER_FIRSTNAME);
				String surename = userInfo.get(CCConstants.PROP_USER_LASTNAME);
				String email = userInfo.get(CCConstants.PROP_USER_EMAIL);
				if(surename == null || surename.equals("")) surename = userInfo.get(CCConstants.PROP_USERNAME);
		
				HashMap<String,String> vcardMap = new HashMap<String,String>();
				vcardMap.put(CCConstants.VCARD_GIVENNAME, givenName);
				vcardMap.put(CCConstants.VCARD_SURNAME, surename);
				vcardMap.put(CCConstants.VCARD_EMAIL, email);
				String vcardString = VCardTool.hashMap2VCard(vcardMap);
				
				//the IO Replication stuff for CCM_PROP_IO_REPL_METADATACONTRIBUTER_CREATOR and CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_AUTHOR
				ArrayList<String> replContributer = new ArrayList<String>();
				replContributer.add(vcardString);
				
				ioProps.put(CCConstants.CCM_PROP_IO_REPL_METADATACONTRIBUTER_CREATOR, vcardString);
				ioProps.put(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_AUTHOR, vcardString);
				
			}
			
			//Technical Location			
			String techLocValue = "ccrep://"+repId+"/"+nodeId;
			ioProps.put(CCConstants.LOM_PROP_TECHNICAL_LOCATION, techLocValue);
			logger.info("start update node");
			mcbaseClient.updateNode(nodeId, ioProps);
			logger.info("finish update node");
				
			
		}catch(Throwable e){
			logger.error(e.getMessage(),e);
		}

		return null;
	}
	
}
