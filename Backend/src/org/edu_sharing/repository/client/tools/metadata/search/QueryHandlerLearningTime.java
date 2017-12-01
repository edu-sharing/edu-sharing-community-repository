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
import org.edu_sharing.repository.client.tools.CCConstants;


public class QueryHandlerLearningTime extends QueryHandlerAbstract {

		@Override
		public String getStatement(MetadataSetQuery mdsq, HashMap<MetadataSetQueryProperty, String[]> propValue) {
			
			String statement = mdsq.getStatement();
			
			String learningTimeFrom = getValue("from", propValue);
			String learningTimeTo = getValue("to", propValue);
			String unit =  getValue("unit", propValue);
				
			if (learningTimeFrom != null && !learningTimeFrom.trim().equals("") && learningTimeTo != null && !learningTimeTo.trim().equals("")) {
				try {
			
					 
					int fromUser = Integer.parseInt(learningTimeFrom.trim());
					int toUser = Integer.parseInt(learningTimeTo.trim());
					Long millisecFrom = null;
					Long millisecTo = null;
					if (unit.equals(CCConstants.UI_LB_LT_MINUTES_KEY)) {
						millisecFrom = new Long(getMillis(0, fromUser));
						millisecTo = new Long(getMillis(0, toUser));
					} else {
						// CCConstants.CCConstants.UI_LB_LT_HOURS_KEY is default
						millisecFrom = new Long(getMillis(fromUser, 0));
						millisecTo = new Long(getMillis(toUser, 0));
					}
					
					
					statement = statement.replace("${from}", millisecFrom.toString());
					statement = statement.replace("${to}", millisecTo.toString());
					
					return statement;


				} catch (Exception e) {

				}
			}
			
			return null;
		}
		
		private long getMillis(long hours, long minutes) {
			long result = (hours * 3600000) + (minutes * 60000);
			return result;
		}
}
