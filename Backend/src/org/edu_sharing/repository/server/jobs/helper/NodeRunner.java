package org.edu_sharing.repository.server.jobs.helper;

import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.server.tools.cache.RepositoryCache;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.springframework.context.ApplicationContext;

import javax.transaction.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * This is a class that will do a task for all nodes with a given type (or all) in a given folder
 */
public class NodeRunner {

    Logger logger = Logger.getLogger(NodeRunner.class);

    private NodeService nodeService = NodeServiceFactory.getLocalService();

    /**
     * The task that will be called for each node
     */
    private Consumer<NodeRef> task;

    /**
     * A filter (optional) which should return true for all nodes where the task should actually be processed for
     */
    private Predicate<? super NodeRef> filter;

    /**
     * The start folder, defaults to company home
     */
    private String startFolder=nodeService.getCompanyHome();

    /**
     * The types of nodes that should be processed (or null for all)
     */
    private List<String> types;

    /**
     * run as alfresco system user
     */
    private boolean runAsSystem = false;

    /**
     * Threading enabled
     */
    private boolean threaded = true;

    /**
     * Shall the modified date of the edited nodes stay the same
     * (Enabling this will disable the behaviour filters while running)
     * Required transaction to be set to Local or Global
     */
    private boolean keepModifiedDate;

    /**
     * Shall a transaction be spawned for the task
     * This WILL NOT WORK in conjunction with threaded=true
     */
    private TransactionMode transaction=TransactionMode.None;


    private ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
    ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
    private BehaviourFilter policyBehaviourFilter = (BehaviourFilter) applicationContext.getBean("policyBehaviourFilter");

    public Consumer<NodeRef> getTask() {
        return task;
    }

    public void setTask(Consumer<NodeRef> task) {
        this.task = task;
    }

    public Predicate<? super NodeRef> getFilter() {
        return filter;
    }

    public void setFilter(Predicate<? super NodeRef> filter) {
        this.filter = filter;
    }

    public String getStartFolder() {
        return startFolder;
    }

    public void setStartFolder(String startFolder) {
        this.startFolder = startFolder;
    }

    public List<String> getTypes() {
        return types;
    }

    public void setTypes(List<String> types) {
        this.types = types;
    }

    public boolean isThreaded() {
        return threaded;
    }

    public void setThreaded(boolean threaded) {
        this.threaded = threaded;
    }

    public boolean isRunAsSystem() {
        return runAsSystem;
    }

    public void setRunAsSystem(boolean runAsSystem) {
        this.runAsSystem = runAsSystem;
    }

    /**
     * runs the job for each node
     * @return the number of nodes that have been processed (also if they may have been filtered in the filter expression)
     */
    public int run(){
        if(task==null)
            throw new IllegalArgumentException("No task has been set yet");

        UserTransaction userTransaction = null;
        if(transaction.equals(TransactionMode.Global)){
            userTransaction = serviceRegistry.getTransactionService().getNonPropagatingUserTransaction();
            try {
                userTransaction.begin();
            } catch (NotSupportedException|SystemException e) {
                logger.error(e.getMessage(), e);
            }
        }
        try {
            List<NodeRef> nodes;
            if (runAsSystem)
                nodes = AuthenticationUtil.runAsSystem(() -> nodeService.getChildrenRecursive(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, startFolder, types));
            else
                nodes = nodeService.getChildrenRecursive(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, startFolder, types);

            Predicate<? super NodeRef> callFilter = (ref) -> {
                if (filter == null)
                    return true;
                if (runAsSystem) {
                    return AuthenticationUtil.runAsSystem(() -> filter.test(ref));
                } else {
                    return filter.test(ref);
                }
            };
            Consumer<? super NodeRef> callTask = (ref) -> {
                UserTransaction userTransactionLocal = null;
                if(transaction.equals(TransactionMode.Local)){
                    userTransactionLocal = serviceRegistry.getTransactionService().getNonPropagatingUserTransaction();
                    try {
                        userTransactionLocal.begin();
                    } catch (NotSupportedException|SystemException e) {
                        logger.error(e.getMessage(), e);
                    }
                }
                if(keepModifiedDate)
                    policyBehaviourFilter.disableBehaviour(ref);
                if (runAsSystem) {
                    AuthenticationUtil.runAsSystem(() -> {
                        task.accept(ref);
                        new RepositoryCache().remove(ref.getId());
                        return null;
                    });
                } else {
                    task.accept(ref);
                    new RepositoryCache().remove(ref.getId());
                }
                policyBehaviourFilter.enableBehaviour(ref);
                if(transaction.equals(TransactionMode.Local)){
                    try {
                        userTransactionLocal.commit();
                    } catch (Exception e) {
                        logger.error(e.getMessage(),e);
                        try {
                            userTransactionLocal.rollback();
                        } catch (SystemException e1) {
                            logger.error(e1.getMessage(),e1);
                        }
                    }
                }
            };
            if (threaded)
                nodes.parallelStream().filter(callFilter).forEach(callTask);
            else
                nodes.stream().filter(callFilter).forEach(callTask);
            return nodes.size();
        }
        finally{
            if(transaction.equals(TransactionMode.Global)){
                try {
                    userTransaction.commit();
                } catch (Exception e) {
                    logger.error(e.getMessage(),e);
                    try {
                        userTransaction.rollback();
                    } catch (SystemException e1) {
                        logger.error(e1.getMessage(),e1);
                    }
                }
            }

        }
    }

    public void setKeepModifiedDate(boolean keepModifiedDate) {
        this.keepModifiedDate = keepModifiedDate;
    }

    public boolean isKeepModifiedDate() {
        return keepModifiedDate;
    }

    public TransactionMode isTransaction() {
        return transaction;
    }

    public void setTransaction(TransactionMode transaction) {
        this.transaction = transaction;
    }
    public enum TransactionMode{
        None, // no transactions
        Global, // for whole task
        Local // per node
    }
}
