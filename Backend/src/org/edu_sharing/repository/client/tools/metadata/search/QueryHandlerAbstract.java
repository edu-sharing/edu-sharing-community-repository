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

import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetQueryProperty;


public abstract class QueryHandlerAbstract implements QueryHandler{
	
	public String getValue(String propName, HashMap<MetadataSetQueryProperty, String[]> propValue){
		for(Map.Entry<MetadataSetQueryProperty, String[]> entry: propValue.entrySet()){
			if(propName.equals(entry.getKey().getName())){
				String[] val = entry.getValue();
				if(val != null && val.length > 0 && val[0] != null){
					return val[0];
				}
			}
		}
		return null;
	}
}
