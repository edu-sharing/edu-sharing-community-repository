package org.edu_sharing.repository.update;

import java.io.PrintWriter;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;

public abstract class UpdateAbstract implements Update {

	Logger logger = null;
	
	PrintWriter out = null;
	
	
	protected void logInfo(String message){
		logger.info(message);
		if(out != null){
			out.println(message);
		}
	}
	protected void logDebug(String message){
		logger.debug(message);
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
	
	protected void executeWithProtocolEntry() {
		try{
			
			Protocol protocol = new Protocol();
			HashMap<String,Object> updateInfo = protocol.getSysUpdateEntry(this.getId());
			if(updateInfo == null){
				run();
			}else{
				logInfo("update" +this.getId()+ " already done at "+updateInfo.get(CCConstants.CCM_PROP_SYSUPDATE_DATE));
			}
			
		}catch(Throwable e){
			logError(e.getMessage(),e);
		}
	}
	
	public abstract void run();
	
}
