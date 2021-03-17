package org.edu_sharing.alfresco.monitoring;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.transaction.TransactionService;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.springframework.context.ApplicationContext;

public class MBeanSupport {

    private final ClassLoader beanClassLoader = Thread.currentThread().getContextClassLoader();
    private final Logger logger = Logger.getLogger(MBeanSupport.class);

    protected <T> T doWork(final AuthenticationUtil.RunAsWork<T> callback) {
        ClassLoader jmxClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(beanClassLoader);
            return AuthenticationUtil.runAs(callback, AuthenticationUtil.getSystemUserName());
        } finally {
            Thread.currentThread().setContextClassLoader(jmxClassLoader);
        }
    }

    protected <T> T doTxReadOnlyWork(final RetryingTransactionHelper.RetryingTransactionCallback<T> callback) {
        ClassLoader jmxClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(beanClassLoader);
            return AuthenticationUtil.runAs(() -> {
                try {
                    ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
                    ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
                    TransactionService transactionService = serviceRegistry.getTransactionService();
                    boolean readOnly = transactionService.isReadOnly();
                    boolean requiresNew = !readOnly && AlfrescoTransactionSupport.getTransactionReadState() == AlfrescoTransactionSupport.TxnReadState.TXN_READ_ONLY;
                    return transactionService.getRetryingTransactionHelper().doInTransaction(callback, true, requiresNew);
                } catch (Exception e) {
                    logger.error(e);
                    throw e;
                }
            }, AuthenticationUtil.getSystemUserName());
        } finally {
            Thread.currentThread().setContextClassLoader(jmxClassLoader);
        }
    }
}
