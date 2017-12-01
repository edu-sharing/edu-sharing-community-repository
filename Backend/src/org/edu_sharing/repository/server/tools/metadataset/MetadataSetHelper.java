package org.edu_sharing.repository.server.tools.metadataset;

import java.util.HashMap;
import java.util.Map;

import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetQuery;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetQueryProperty;
import org.edu_sharing.repository.client.tools.UrlTool;



public class MetadataSetHelper {
	
	/**
	 * find out if it's an extended search by watching out for filled criterias with toggle = true;
	 * @param mdsSearchData
	 * @return
	 */
	public boolean isExtendedSearch(HashMap<MetadataSetQuery, HashMap<MetadataSetQueryProperty, String[]>>  mdsSearchData){
		
		for(Map.Entry<MetadataSetQuery, HashMap<MetadataSetQueryProperty, String[]>> entry : mdsSearchData.entrySet()){
			for(Map.Entry<MetadataSetQueryProperty, String[]> propEntry : entry.getValue().entrySet()){
				if(propEntry.getValue() != null && propEntry.getValue().length > 0){
					//get serverside representation for security reasons
					MetadataSetQueryProperty prop = (MetadataSetQueryProperty)MetadataCache.getMetadataSetProperty(propEntry.getKey().getId());
					if(prop.getToggle() == true){
						return true;
					}
				}
			}
		}
		
		return false;
	}
	
	public String getStatistikQueryString(HashMap<MetadataSetQuery, HashMap<MetadataSetQueryProperty, String[]>>  mdsSearchData){
		
		String criteriaString = "";
		for(Map.Entry<MetadataSetQuery, HashMap<MetadataSetQueryProperty, String[]>> entry : mdsSearchData.entrySet()){
			for(Map.Entry<MetadataSetQueryProperty, String[]> propEntry : entry.getValue().entrySet()){
		
				if(propEntry.getValue() != null && propEntry.getValue().length > 0){
					
					String paramVal = "";
					for(String value : propEntry.getValue()){
						
						if(value != null && !value.trim().equals("")){
							
							// we cant do this on serverside value = com.google.gwt.http.client.URL.encodeQueryString(value);
							paramVal = (paramVal.trim().equals(""))? value: paramVal + ","+value;
						}
					}
					if(!paramVal.trim().equals("")){
						String criteriaName = (propEntry.getKey().getInit_by_get_param() != null && !propEntry.getKey().getInit_by_get_param().equals(""))? propEntry.getKey().getInit_by_get_param() : propEntry.getKey().getName();
						criteriaString = UrlTool.setParam(criteriaString, criteriaName +"[#]"+propEntry.getKey().getWidget()  , paramVal);
					}
				}
			}
		}
		
		return criteriaString;
	}
}
