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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.fileupload.FileItem;

public abstract class HelperAbstract implements Helper {
	
	abstract public HashMap<String, Object> execute(HashMap<String, Object> params, HashMap<String, String> authenticatioInfo);

	public FileItem getFileItem(String property, List items){
		for (int i = 0; i < items.size(); i++) {
			FileItem item = (FileItem) items.get(i);
			if(item.getFieldName().equals(property)){
				return item;
			}
		}
		return null;
	}
	
	public List<FileItem> getFileItems(String property, List items){
		List<FileItem> list = new ArrayList<FileItem>();
		for (int i = 0; i < items.size(); i++) {
			FileItem item = (FileItem) items.get(i);
			
			if(item.getFieldName().equals(property)){
				list.add(item);		
			}
		}
		return list;
	}

}
