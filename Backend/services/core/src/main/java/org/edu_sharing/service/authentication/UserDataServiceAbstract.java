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
package org.edu_sharing.service.authentication;

import java.io.Serializable;
import java.util.Map;

import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;

public abstract class UserDataServiceAbstract implements UserDataService {
	
	
	
	private ServiceRegistry serviceRegistry;
	
	@Override
	public void updateUser(final String repUsername, String directoryUsername) throws Exception {
		
		final Map<QName, Serializable> persProps = this.getRepositoryUserProperties(directoryUsername);
		
		 if(persProps != null && persProps.size() > 0){
			 RetryingTransactionCallback<Object> txnWork = new RetryingTransactionCallback<Object>() {
					public Object execute() throws Exception {
						serviceRegistry.getPersonService().setPersonProperties(repUsername, persProps);
						return null;
					}
			};
			TransactionService transactionService = serviceRegistry.getTransactionService();
			transactionService.getRetryingTransactionHelper().doInTransaction(txnWork, false);
		 }
	}
	

	public ServiceRegistry getServiceRegistry() {
		return serviceRegistry;
	}

	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}
	
	
}
