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

import java.util.HashMap;
import java.util.List;

import org.apache.commons.fileupload.FileItem;

public interface Helper {
	public HashMap<String,Object> execute(HashMap<String,Object> params, HashMap<String,String> authenticatioInfo);
	
	public FileItem getFileItem(String property, List items);
}
