package org.edu_sharing.alfresco.authentication.subsystems;

import java.util.Date;
import java.util.List;

import org.alfresco.repo.security.authentication.AuthenticationException;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport.TxnReadState;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.springframework.dao.ConcurrencyFailureException;

public class SubsystemChainingAuthenticationService extends org.alfresco.repo.security.authentication.subsystems.SubsystemChainingAuthenticationService {

	Logger logger = Logger.getLogger(SubsystemChainingAuthenticationService.class);
	
	
	static ThreadLocal<String> successFullAuthenticationMethod = new ThreadLocal<String>();
	static ThreadLocal<String> currentPath = new ThreadLocal<>();
	
	NodeService nodeService;
	PersonService personService;
	
	TransactionService transactionService;
	
	 /**
     * {@inheritDoc}
     */
    public void authenticate(String userName, char[] password) throws AuthenticationException
    {
        preAuthenticationCheck(userName);
        List<AuthenticationService> usableAuthenticationServices = getUsableAuthenticationServices();
        int counter = usableAuthenticationServices.size();
        for (AuthenticationService authService : usableAuthenticationServices)
        {
            try
            {
            	    counter--;
                authService.authenticate(userName, password);
                if (logger.isDebugEnabled())
                {
                    logger.debug("authenticate "+userName+" with "+getId(authService)+" SUCCEEDED");
                }
                successFullAuthenticationMethod.set(getId(authService));
                setEsLastLoginToNow(userName);
                
                return;
            }
            catch (AuthenticationException e)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("authenticate "+userName+" with "+getId(authService)+(counter == 0 ? " FAILED (end of chain)" : " failed (try next in chain)"));
                }
                // Ignore and chain
            }
        }
        throw new AuthenticationException("Failed to authenticate");

    }
    
    public void setEsLastLoginToNow(String userName) {
    	NodeRef nodeRefPerson = personService.getPerson(userName,false);
    	// we won't do this for the guest
        if(userName!=null && userName.equals(ApplicationInfoList.getHomeRepository().getGuest_username()))
            return;
        RunAsWork<Void> runAs = new RunAsWork<Void>() {
        	@Override
        	public Void doWork() throws Exception {
        		//alfresco share login is in readOnlyMode, so check to prevent exception
        		if (AlfrescoTransactionSupport.getTransactionReadState() == TxnReadState.TXN_READ_WRITE) {
        			
        			RetryingTransactionCallback<Void> txnWork = new RetryingTransactionCallback<Void>() {
						public Void execute() throws Exception {
							try {
								nodeService.setProperty(nodeRefPerson, QName.createQName(CCConstants.PROP_USER_ESLASTLOGIN), new Date());
							}catch(ConcurrencyFailureException e) {
								logger.info("failed to set EsLastLogin for user " + userName + " cause of " + e.getClass().getSimpleName());
							}
							return null;
						}
					};
					return transactionService.getRetryingTransactionHelper().doInTransaction(txnWork, false);
        		}
        		return null;
        	}
        };
        AuthenticationUtil.runAsSystem(runAs);
    }
	
    public static void setSuccessFullAuthenticationMethod(String successFullAuthenticationMethod) {
		SubsystemChainingAuthenticationService.successFullAuthenticationMethod.set(successFullAuthenticationMethod);
	}
    
    public static String getSuccessFullAuthenticationMethod() {
		return successFullAuthenticationMethod.get();
	}

    public static void setCurrentPath(String currentPath) {
        SubsystemChainingAuthenticationService.currentPath.set(currentPath);
    }

    public static String getCurrentPath() {
        return currentPath.get();
    }

    public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}
    
    public void setPersonService(PersonService personService) {
		this.personService = personService;
	}
    
    public void setTransactionService(TransactionService transactionService) {
		this.transactionService = transactionService;
	}
}
