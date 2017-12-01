package org.edu_sharing.repository.server.tools.forms;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.QName;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.forms.VCardTool;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;

public class Test {

	public static void main(String[] args){
		NodeService nodeService = null;
		PersonService personService = null;
		NodeRef ioNodeRef = null;
		
		
		Map<QName, Serializable> ioProps = nodeService.getProperties(ioNodeRef);
		NodeRef personRef = personService.getPerson((String)ioProps.get(ContentModel.PROP_CREATOR));
		Map<QName, Serializable> userInfo = nodeService.getProperties(personRef);
		
		//creator as author
		String givenName = (String)userInfo.get(ContentModel.PROP_FIRSTNAME);
		String surename = (String)userInfo.get(ContentModel.PROP_LASTNAME);
		String email = (String)userInfo.get(CCConstants.PROP_USER_EMAIL);
		if(surename == null || surename.equals("")) surename = (String)userInfo.get(CCConstants.PROP_USERNAME);
		//logger.info(userInfo);
		HashMap<String,String> vcardMap = new HashMap<String,String>();
		vcardMap.put(CCConstants.VCARD_GIVENNAME, givenName);
		vcardMap.put(CCConstants.VCARD_SURNAME, surename);
		vcardMap.put(CCConstants.VCARD_EMAIL, email);
		String vcardString = VCardTool.hashMap2VCard(vcardMap);
		
		nodeService.setProperty(ioNodeRef, QName.createQName(CCConstants.CCM_PROP_IO_REPL_METADATACONTRIBUTER_CREATOR), vcardString);
		nodeService.setProperty(ioNodeRef, QName.createQName(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_AUTHOR), vcardString);
		
		HashMap<String, Object>  ioprops = new HashMap<String, Object>();
		String techLocValue = "ccrep://"+ApplicationInfoList.getHomeRepository().getAppId()+"/"+ioNodeRef.getId();
		nodeService.setProperty(ioNodeRef, QName.createQName(CCConstants.LOM_PROP_TECHNICAL_LOCATION), techLocValue);

		
	}
}
