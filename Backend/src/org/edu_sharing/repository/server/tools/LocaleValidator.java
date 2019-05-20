package org.edu_sharing.repository.server.tools;

import org.apache.log4j.Logger;
import org.edu_sharing.service.config.ConfigServiceFactory;

import java.util.Arrays;

public class LocaleValidator {

	private static Logger logger=Logger.getLogger(LocaleValidator.class);

	public static boolean validate(String locale){
		//set the locale
	   
	    if(locale != null && locale.matches("[a-z][a-z]_[A-Z][A-Z]")){
			// if the locale requested is found in config -> allow it
			return Arrays.stream(ConfigServiceFactory.getSupportedLanguages()).
				anyMatch(locale::startsWith);
		}
	    
	    return false;
	}
}
