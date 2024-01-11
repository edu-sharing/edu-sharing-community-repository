package org.edu_sharing.spring;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationContextFactory implements ApplicationContextAware {

//	static String appContextResource = "/org/edu_sharing/spring/application-context.xml";
	
	//using the eager init method, no synchronized is needed
	@Getter
	static ApplicationContext applicationContext;//= new ClassPathXmlApplicationContext(appContextResource);

	@Override
	public void setApplicationContext(@NotNull ApplicationContext applicationContext) throws BeansException {
		ApplicationContextFactory.applicationContext = applicationContext;
	}
}
