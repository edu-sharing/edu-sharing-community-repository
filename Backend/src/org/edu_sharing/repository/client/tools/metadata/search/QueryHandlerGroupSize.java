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

import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetQuery;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetQueryProperty;


public class QueryHandlerGroupSize extends QueryHandlerAbstract {
	@Override
	public String getStatement(MetadataSetQuery mdsq, HashMap<MetadataSetQueryProperty, String[]> propValue) {
		String statement = mdsq.getStatement();
		String from = getValue("from", propValue);
		String to = getValue("to", propValue);
		
		if (from != null && !from.trim().equals("") && to != null && !to.trim().equals("")) {
			try {
				Integer.parseInt(from.trim());
				Integer.parseInt(to.trim());
				
				statement = statement.replace("${from}", from);
				statement = statement.replace("${to}",to);
				return statement;	
			} catch (Exception e) {
				
			}
		}
		
		return null;
	}
}
