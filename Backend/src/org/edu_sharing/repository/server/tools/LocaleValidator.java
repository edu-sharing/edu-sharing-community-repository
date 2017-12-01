package org.edu_sharing.repository.server.tools;

public class LocaleValidator {

	public static boolean validate(String locale){
		//set the locale
	   
	    if(locale != null && locale.matches("[a-z][a-z]_[A-Z][A-Z]")){
	    	return true;
	    }
	    
	    return false;
	}
}
