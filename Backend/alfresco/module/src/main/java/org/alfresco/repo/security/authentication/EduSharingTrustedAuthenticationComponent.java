package org.alfresco.repo.security.authentication;

import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;

public class EduSharingTrustedAuthenticationComponent extends
		AuthenticationComponentImpl {

	@Override
	protected void authenticateImpl(final String userName, char[] password)
			throws AuthenticationException {
		
		String normalized = getTransactionService().getRetryingTransactionHelper().doInTransaction(
                new RetryingTransactionCallback<String>()
                {
                    public String execute() throws Throwable
                    {
                        return AuthenticationUtil.runAs(new RunAsWork<String>()
                        {
                            public String doWork() throws Exception
                            {
                            	
                            	// TRUSTED => NO Password Check !!!
                            	
                                return getPersonService().getUserIdentifier(userName);
                            }
                        }, getSystemUserName(getUserDomain(userName)));
                    }
                }, true); 
		
        if (normalized == null)
        {
            setCurrentUser(userName, UserNameValidationMode.CHECK_AND_FIX);
        }
        else
        {
            setCurrentUser(normalized, UserNameValidationMode.NONE);                
        }
	}
    	
}
