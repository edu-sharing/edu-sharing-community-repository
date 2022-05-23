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
package org.edu_sharing.alfrescocontext;

import org.alfresco.util.RuntimeSystemPropertiesSetter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.edu_sharing.alfrescocontext.loader.AlfrescoContextClassLoaderUtil;


public class MVRuntimeSystemPropertiesSetter extends RuntimeSystemPropertiesSetter {
	
	private static Log logger = LogFactory.getLog(RuntimeSystemPropertiesSetter.class );
	
	public MVRuntimeSystemPropertiesSetter(){
		super();
		
		logger.info("using MVRuntimeSystemPropertiesSetter!!!!!");
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		AlfrescoContextClassLoaderUtil.setClassLoader(loader);
	}
}
