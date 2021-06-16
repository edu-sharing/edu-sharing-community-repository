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
package org.edu_sharing.alfrescocontext.gate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;


public class AlfAppContextGate implements ApplicationContextAware {
	
	
	private static Log logger = LogFactory.getLog(AlfAppContextGate.class);
	
	private static ApplicationContext applicationContext = null;
	
	public AlfAppContextGate(){
		logger.info("AlfAppContextGate Constructor ");
	}
	
	/**
	 * @return the applicationContext
	 */
	public static ApplicationContext getApplicationContext() {
		logger.debug("AlfAppContextGate.getApplicationContext pplicationContext:"+applicationContext);
		return applicationContext;
	}

	@Override
	public void setApplicationContext(ApplicationContext _applicationContext) throws BeansException {
		logger.debug("AlfAppContextGate.setApplicationContext "+_applicationContext);
		applicationContext = _applicationContext;
		
	}
}
