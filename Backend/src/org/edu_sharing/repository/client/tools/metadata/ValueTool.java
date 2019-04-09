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

import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.StringTool;


public class ValueTool {

	public static String[] getMultivalue(String mvValue){
		String[] result = null;
		if (mvValue.contains(CCConstants.MULTIVALUE_SEPARATOR)) {
			result = mvValue.split(StringTool.escape(CCConstants.MULTIVALUE_SEPARATOR));
		} else {
			result = new String[]{mvValue};
		}
		return result;
	}
	public static String toMultivalue(String[] multivalue){
		return StringUtils.join(multivalue,CCConstants.MULTIVALUE_SEPARATOR);
	}
}
