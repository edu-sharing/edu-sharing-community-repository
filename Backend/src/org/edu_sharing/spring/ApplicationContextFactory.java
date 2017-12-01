package org.edu_sharing.spring;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class ApplicationContextFactory {

	static String appContextResource = "/org/edu_sharing/spring/application-context.xml";
	
	//using the eager init method, no synchronized is needed
	static ApplicationContext applicationContext = new ClassPathXmlApplicationContext(appContextResource);

	 public static ApplicationContext getApplicationContext(){
		 return applicationContext;
	 }
	 
	 
	 public static String BEAN_ID_HELPER_PRE_CREATE = "helperPreCreate";
	 
	 public static String BEAN_ID_HELPER_POST_CREATE = "helperPostCreate";
	 
	 public static String BEAN_ID_HELPER_POST_UPDATE_METADATA = "helperPostUpdateBaseMetadata";
	 
	 public static String BEAN_ID_HELPER_POST_DELETE = "helperPostDelete";
}
