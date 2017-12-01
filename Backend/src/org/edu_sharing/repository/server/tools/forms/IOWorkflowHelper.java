package org.edu_sharing.repository.server.tools.forms;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetFormsPanel;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetFormsProperty;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.StringTool;
import org.edu_sharing.repository.client.tools.metadata.ValueTool;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.RepoFactory;
import org.edu_sharing.repository.server.authentication.Context;
import org.edu_sharing.repository.server.tools.DateTool;
import org.json.JSONException;
import org.json.JSONObject;


public class IOWorkflowHelper extends org.edu_sharing.repository.server.tools.forms.HelperAbstract{

	Logger logger = Logger.getLogger(IOWorkflowHelper.class);
	
	public HashMap<String, Object> execute(HashMap<String, Object> params, HashMap<String, String> authenticatioInfo){
		
		final String nodeId = (String)params.get(CCConstants.NODEID);
		MCAlfrescoAPIClient apiClient = new MCAlfrescoAPIClient();
		
		try{
			
			MetadataSetFormsPanel panel = RepoFactory.getDefaultMetadataSet().getMetadataSetForm(CCConstants.CCM_TYPE_IO, null).getMetadataSetPanel("Workflow");
			
			if(panel == null){
				logger.error("no mds panel for Name 'Workflow found'");
				return null;
			}
			
			String modifier = apiClient.getProperty(MCAlfrescoAPIClient.storeRef.getProtocol(), 
					MCAlfrescoAPIClient.storeRef.getIdentifier(), 
					nodeId, 
					CCConstants.CM_PROP_C_MODIFIER);
			HashMap<String,String> userInfo = apiClient.getUserInfo(modifier);
			String givenName = userInfo.get(CCConstants.PROP_USER_FIRSTNAME);
			String surename = userInfo.get(CCConstants.PROP_USER_LASTNAME);
			String email = userInfo.get(CCConstants.PROP_USER_EMAIL);
			if(surename == null || surename.equals("")) surename = userInfo.get(CCConstants.PROP_USERNAME);
			
			HashMap<String,Object> properties = apiClient.getProperties(nodeId);
			
			JSONObject protocolEntry = new JSONObject();
			
			JSONObject jsonObject = new JSONObject();
			
			String locale = (String)Context.getCurrentInstance().getRequest().getSession().getAttribute(CCConstants.AUTH_LOCALE);
			
			
			String receiver = (String)properties.get(CCConstants.CCM_PROP_WF_RECEIVER);
			if(receiver == null || receiver.trim().equals("")){
				return null;
			}
			
			
			{
				
				String editor = givenName + " " + surename;
				
			
				String key = "editor";
				String label = "";//I18nServer.getTranslationDefaultResourcebundle("dialog_inviteusers_perm_desc_editor", locale);
				String value = editor;
				String displayValue = value;
				setHistoryAttribut(jsonObject,key,label,value,displayValue);
			}
			
			{
				String key = "timestamp";
				String label = "";//I18nServer.getTranslationDefaultResourcebundle("btn_date", locale);
				
				Date date =  new Date();
				long value = date.getTime();
				String displayValue = new DateTool().formatDate(new Long(value), DateFormat.LONG, DateFormat.SHORT);
				setHistoryAttribut(jsonObject,key,label,new Long(value).toString(),displayValue);
			}
			
			{
				
				receiver = (receiver == null) ? "" : receiver; 
				receiver = receiver.replaceAll(StringTool.escape(CCConstants.MULTIVALUE_SEPARATOR), ",");
				
				
				MetadataSetFormsProperty mdsProp = panel.getProperty(CCConstants.CCM_PROP_WF_RECEIVER);
				if(mdsProp == null){
					logger.error("no mds prop found for " + CCConstants.CCM_PROP_WF_RECEIVER);
				}
				String key = "receiver";
				String label = "übergeben an"; //mdsProp.getLabel().getValue(locale);
				String value = receiver;
				String displayValue = value;
				setHistoryAttribut(jsonObject,key,label,value,displayValue);
				
			}
			
			{
				String status = (String)properties.get(CCConstants.CCM_PROP_WF_STATUS);
				
				MetadataSetFormsProperty mdsProp = panel.getProperty(CCConstants.CCM_PROP_WF_STATUS);
				if(mdsProp == null){
					logger.error("no mds prop found for " + CCConstants.CCM_PROP_WF_STATUS);
				}
				String key = "status";
				String label = "neuer Status";
				String value = status;
				String displayValue = mdsProp.getValueOfValueSpace(value, locale);
				setHistoryAttribut(jsonObject,key,label,value,displayValue);
				
			}
			
			{
				String instructions = (String)properties.get(CCConstants.CCM_PROP_WF_INSTRUCTIONS);
				
				MetadataSetFormsProperty mdsProp = panel.getProperty(CCConstants.CCM_PROP_WF_INSTRUCTIONS);
				if(mdsProp == null){
					logger.error("no mds prop found for " + CCConstants.CCM_PROP_WF_INSTRUCTIONS);
				}
				String key = "instructions";
				String label = mdsProp.getLabel().getValue(locale);
				String value = instructions;
				String displayValue = value;
				setHistoryAttribut(jsonObject,key,label,value,displayValue);
				
			}
		
			protocolEntry = jsonObject;
			
			System.out.println("protocolEntry:"+protocolEntry.toString());
			
			String protocolEntries = apiClient.getProperty(MCAlfrescoAPIClient.storeRef.getProtocol(), 
					MCAlfrescoAPIClient.storeRef.getIdentifier(), 
					nodeId, 
					CCConstants.CCM_PROP_WF_PROTOCOL);
			
			System.out.println("protocolEntries:" + protocolEntries);
			
			if(protocolEntries != null){
				for(String tm : new ValueTool().getMultivalue(protocolEntries)){
					System.out.println("protocolEnt:" + tm);
				}	
			}
			List<String> protocolEntryList = (protocolEntries == null) ? new ArrayList<String>() : new ArrayList<String>(Arrays.asList(new ValueTool().getMultivalue(protocolEntries)));
			protocolEntryList.add(0,protocolEntry.toString());
			
			HashMap<String,Object> propsToSafe = new HashMap<String,Object>();
			propsToSafe.put(CCConstants.CCM_PROP_WF_PROTOCOL, protocolEntryList);
			apiClient.updateNode(nodeId, propsToSafe);
			
			//clear instructions
			apiClient.setProperty(nodeId, CCConstants.CCM_PROP_WF_INSTRUCTIONS, "");
			
		}catch(Throwable e){
			e.printStackTrace();
		}
		
		return null;
	}

	private void setHistoryAttribut(JSONObject historyEntry, String key, String label, String value, String displayValue) throws JSONException{
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("key",key);
		jsonObject.put("label",label);
		jsonObject.put("value",value);
		jsonObject.put("displayValue",displayValue);
		historyEntry.put(historyEntry.length()+"", jsonObject);
	}
}
