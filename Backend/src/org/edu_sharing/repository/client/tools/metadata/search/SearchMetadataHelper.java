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
package org.edu_sharing.repository.client.tools.metadata.search;

import java.util.HashMap;
import java.util.Map;

import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetQueries;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetQuery;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetQueryProperty;


public class SearchMetadataHelper {
	
	/**
	 * 
	 * @param property
	 * @param statement e.g.: "@cclom\\:general_keyword:${value}"
	 * @param values
	 * @return
	 */
	public HashMap<MetadataSetQuery, HashMap<MetadataSetQueryProperty, String[]>> createSearchData(String property, String statement, String[] values){
		MetadataSetQuery mdsq = new MetadataSetQuery();
		MetadataSetQueryProperty prop = new MetadataSetQueryProperty();
		prop.setName(property);
		prop.setStatement(statement);
		final HashMap<MetadataSetQuery, HashMap<MetadataSetQueryProperty, String[]>> metadataSetSearchData = new HashMap<MetadataSetQuery, HashMap<MetadataSetQueryProperty, String[]>>();
		HashMap<MetadataSetQueryProperty, String[]> propVals = new HashMap<MetadataSetQueryProperty, String[]>();
		propVals.put(prop, values);
		metadataSetSearchData.put(mdsq, propVals);
		return metadataSetSearchData;
	}
	
	public HashMap<MetadataSetQuery, HashMap<MetadataSetQueryProperty, String[]>> createSearchData(MetadataSetQueryProperty propertyOriginal, String[] values){
		MetadataSetQuery mdsqOriginal = propertyOriginal.getParent();
		
		MetadataSetQuery mdsq = getFlatCopy(mdsqOriginal);
		MetadataSetQueryProperty property = getFlatCopy(propertyOriginal, mdsq);
		
		final HashMap<MetadataSetQuery, HashMap<MetadataSetQueryProperty, String[]>> metadataSetSearchData = new HashMap<MetadataSetQuery, HashMap<MetadataSetQueryProperty, String[]>>();
		HashMap<MetadataSetQueryProperty, String[]> propVals = new HashMap<MetadataSetQueryProperty, String[]>();
		propVals.put(property, values);
		metadataSetSearchData.put(mdsq, propVals);
		return metadataSetSearchData;
	}
	
	public String[] getValues(HashMap<MetadataSetQuery, HashMap<MetadataSetQueryProperty, String[]>>  searchData, String property){
		if(searchData != null){
			for(Map.Entry<MetadataSetQuery, HashMap<MetadataSetQueryProperty, String[]>> entry: searchData.entrySet()){
				 HashMap<MetadataSetQueryProperty, String[]> propVals = entry.getValue();
				 if(propVals != null){
					 for(Map.Entry<MetadataSetQueryProperty, String[]> propValEntry : propVals.entrySet()){
						 if(propValEntry.getKey().getName().equals(property)){
							 return propValEntry.getValue();
						 }
					 }
				 }
			}
		}
		return null;
	}
	
	/**
	 * 
	 * MetadataSetQuery flat copy
	 * create a new MetadataSetQuery Object that contains only the necessary data 
	 * - leave gui data like stylename
	 * - leave properties
	 *
	 * @param original
	 * @return
	 */
	public MetadataSetQuery getFlatCopy(MetadataSetQuery original){
		MetadataSetQuery mdsQuery = new MetadataSetQuery();
		mdsQuery.setCriteriaboxid(original.getCriteriaboxid());
		mdsQuery.setHandlerclass(original.getHandlerclass());
		mdsQuery.setJoin(original.getJoin());
		//MetadataSetQueries for statement searchword
		MetadataSetQueries mdsqueries = new MetadataSetQueries();
		if(original.getParent() != null){
			mdsqueries.setStatementsearchword(original.getParent().getStatementsearchword());
			mdsQuery.setParent(mdsqueries);
		}
		mdsQuery.setStatement(original.getStatement());
		return mdsQuery;
	}
	
	/**
	 * make a copy of the MetadataSetQueryProperty object that conains only the necessary data for searching and filling gui with values
	 * - leave valuespaces
	 * - leave gui info
	 * 
	 * @param original
	 * @param flatParent (can be null)
	 * @return
	 */
	public MetadataSetQueryProperty getFlatCopy(MetadataSetQueryProperty original, MetadataSetQuery flatParent){
		MetadataSetQueryProperty prop = new MetadataSetQueryProperty();
		
		prop.setId(original.getId());
		prop.setCopyfrom(original.getCopyfrom());
		prop.setEscape(original.getEscape().toString());
		prop.setMultiple(original.getMultiple().toString());
		prop.setMultiplejoin(original.getMultiplejoin());
		prop.setName(original.getName());
		prop.setInit_by_get_param(original.getInit_by_get_param());
		if(flatParent != null){
			prop.setParent(flatParent);
		}
		prop.setStatement(original.getStatement());
		prop.setType(original.getType());
		prop.setId(original.getId());
		prop.setWidget(original.getWidget());
		return prop;
	}
}
