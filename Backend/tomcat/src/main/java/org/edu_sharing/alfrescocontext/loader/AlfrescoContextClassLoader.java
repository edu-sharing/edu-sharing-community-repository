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
package org.edu_sharing.alfrescocontext.loader;

import org.apache.catalina.loader.WebappClassLoader;

/**
 * 
 * 
 * This can be used for tomcat when accessing the alfresco webapp libs from ccsearch webapp
 */
public class AlfrescoContextClassLoader extends WebappClassLoader {
	
	public AlfrescoContextClassLoader(ClassLoader parent){
		super(AlfrescoContextClassLoaderUtil.getClassLoader());
		System.out.println("AlfrescoContextClassLoader Constructor Classloader:"+parent);
	}
}
