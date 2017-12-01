package org.edu_sharing.repository.update;

import java.io.PrintWriter;

import org.apache.log4j.Logger;

public abstract class UpdateAbstract implements Update {

	Logger logger = null;
	
	PrintWriter out = null;
	
	
	protected void logInfo(String message){
		logger.info(message);
		if(out != null){
			out.println(message);
		}
	}
	
	protected void logError(String message, Throwable e){
		logger.error(message,e);
		if(out != null){
			if(e != null){
				out.println(e.getMessage());
			}else{
				out.println(message);
			}
		}
	}
	
}
