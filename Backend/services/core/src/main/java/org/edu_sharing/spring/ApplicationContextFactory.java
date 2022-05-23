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
}
