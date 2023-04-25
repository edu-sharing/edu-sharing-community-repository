package org.edu_sharing.repository.update;

import java.io.PrintWriter;
import java.util.*;

import javax.transaction.UserTransaction;

import org.alfresco.service.ServiceRegistry;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.springframework.context.ApplicationContext;

public abstract class UpdateAbstract implements Update {

	Logger logger = null;
	
	PrintWriter out = null;
	
	ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
	ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);

	static Set<String> currentlyRunningUpdates = Collections.synchronizedSet(new HashSet<>());
	
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

	@Override
	public void execute() {
		executeWithProtocolEntry();
	}

	protected void executeWithProtocolEntry() {
		if (currentlyRunningUpdates.contains(this.getId())) {
			logger.error("update " + this.getId() + " is already running. stop processing");
			return;
		}
		logger.info("started " + this.getId());
		currentlyRunningUpdates.add(this.getId());
		try {

			Protocol protocol = new Protocol();
			HashMap<String, Object> updateInfo = null;

			try {
				updateInfo = protocol.getSysUpdateEntry(this.getId());
			} catch (Throwable e) {
				// TODO Auto-generated catch block
				logError(e.getMessage(), e);
			}
			if (updateInfo == null) {
				UserTransaction transaction = serviceRegistry.getTransactionService().getNonPropagatingUserTransaction();
				try {
					transaction.begin();
					run();
					// this may cause a currently unknown rollback of the whole transaction
					//protocol.writeSysUpdateEntry(getId());
					transaction.commit();
					protocol.writeSysUpdateEntry(getId());
				} catch (Throwable e) {
					this.logError(e.getMessage(), e);
					try {
						transaction.rollback();
					} catch (Exception e2) {
						this.logError(e.getMessage(), e2);
					}
				}
			} else {
				logInfo("update" + this.getId() + " already done at " + updateInfo.get(CCConstants.CCM_PROP_SYSUPDATE_DATE));
			}
		}finally {
			currentlyRunningUpdates.remove(this.getId());
		}
	}

	public void executeWithProtocolEntryNoGlobalTx(){
		if (currentlyRunningUpdates.contains(this.getId())) {
			logger.error("update " + this.getId() + " is already running. stop processing");
			return;
		}
		logger.info("started " + this.getId());
		currentlyRunningUpdates.add(this.getId());
		try {
			Protocol protocol = new Protocol();
			HashMap<String,Object> updateInfo = null;

			try {
				updateInfo = protocol.getSysUpdateEntry(this.getId());
			} catch (Throwable e) {
				// TODO Auto-generated catch block
				logError(e.getMessage(), e);
			}
			if(updateInfo == null){
				boolean result = runAndReport();
				if(result) {
					try {
						protocol.writeSysUpdateEntry(getId());
					} catch (Throwable throwable) {
						logError("error writing protocol entry",throwable);
					}
				}else{
					logError("Update failed or not completed",null);
				}
			}else{
				logInfo("update" +this.getId()+ " already done at "+updateInfo.get(CCConstants.CCM_PROP_SYSUPDATE_DATE));
			}
		}finally {
			currentlyRunningUpdates.remove(this.getId());
		}
	}


	public boolean runAndReport(){
		return false;
	}
	
	public void run() throws Throwable {
		runAndReport();
	}
	
}
