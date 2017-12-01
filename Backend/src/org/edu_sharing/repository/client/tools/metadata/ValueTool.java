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
package org.edu_sharing.repository.client.tools.metadata;

import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.StringTool;


public class ValueTool {

	public String[] getMultivalue(String mvValue){
		String[] result = null;
		if (mvValue.contains(CCConstants.MULTIVALUE_SEPARATOR)) {
			result = mvValue.split(StringTool.escape(CCConstants.MULTIVALUE_SEPARATOR));
		} else {
			result = new String[]{mvValue};
		}
		return result;
	}	
	
}
