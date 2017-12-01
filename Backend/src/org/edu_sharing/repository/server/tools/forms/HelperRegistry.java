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

import org.edu_sharing.repository.client.tools.CCConstants;

public class HelperRegistry {
	
	/**
	 * because of Thread safety new Objects will be returned
	 * @return
	 */
	public static HashMap<String,Helper> getPreCreateRegistry() {
		
		HashMap<String,Helper> preCreateRegistry = new HashMap<String, Helper>();
		preCreateRegistry.put(CCConstants.CCM_TYPE_MAP, new MapCreateHelperPre());
		
		return preCreateRegistry;
	}
	/**
	 *  because of Thread safety new Objects will be returned
	 * @return
	 */
	public static HashMap<String,Helper[]> getPostCreateRegistry(){
		HashMap<String,Helper[]> postCreateRegistry = new HashMap<String, Helper[]>();
	    return postCreateRegistry;
	}
	
	/**
	 *  because of Thread safety new Objects will be returned
	 *  
	 *  will be excecuted after the base properties with processtype standard,copyfromrequest
	 *  but before content or children is stored so that there can not be a conflict with async preview generation
	 * @return
	 */
	public static HashMap<String,Helper[]> getPostUpdateBaseMetadataRegistry(){
		HashMap<String,Helper[]> postUpdateRegistry = new HashMap<String, Helper[]>();
		postUpdateRegistry.put(CCConstants.CCM_TYPE_IO, new Helper[]{new IOLicenseHelper(),new IOTaxonPathHelper(), new SchoolContextHelper(),new IOPreviewHelperUPost()});
	    return postUpdateRegistry;
	}
	
	/**
	 *  because of Thread safety new Objects will be returned
	 * @return
	 */
	public static HashMap<String,Helper[]> getPostDeleteRegistry(){
		HashMap<String,Helper[]> postDeleteRegistry = new HashMap<String, Helper[]>();
		return postDeleteRegistry;
	}
}
