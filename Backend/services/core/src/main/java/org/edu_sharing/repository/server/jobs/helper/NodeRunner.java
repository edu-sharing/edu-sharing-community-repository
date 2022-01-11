package org.edu_sharing.repository.server.jobs.helper;

import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.server.importer.OAIPMHLOMImporter;
import org.edu_sharing.repository.server.tools.cache.RepositoryCache;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.RecurseMode;
import org.springframework.context.ApplicationContext;

import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

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
     * Note: Currently, only the task is threaded, not previous filter actions
     * The thread pool uses the same Thread Size as the @OAIPMHLOMImporter
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

    /**
     * How to recurse across elements
     * Default will only recurse into sub-folders, but not sub-elements (e.g. childobjects)
     */
    private RecurseMode recurseMode = RecurseMode.Folders;

    /**
     * shall the cache for each processed node be invalidated/cleared?
     */
    private boolean invalidateCache = true;


    private ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
    ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
    private BehaviourFilter policyBehaviourFilter = (BehaviourFilter) applicationContext.getBean("policyBehaviourFilter");

    private String lucene = null;
    private StoreRef luceneStore = StoreRef.STORE_REF_WORKSPACE_SPACESSTORE;

    public NodeRunner() {
	}
    
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

    public boolean isInvalidateCache() {
        return invalidateCache;
    }

    public void setInvalidateCache(boolean invalidateCache) {
        this.invalidateCache = invalidateCache;
    }

    public void setLucene(String lucene) {
        this.lucene = lucene;
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


            if(lucene == null || lucene.trim().equals("")) {
                if (runAsSystem)
                    nodes = AuthenticationUtil.runAsSystem(() -> nodeService.getChildrenRecursive(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, startFolder, types, recurseMode));
                else
                    nodes = nodeService.getChildrenRecursive(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, startFolder, types, recurseMode);
            }else{
                logger.info("collection nodes by lucene:"+lucene);
                if (runAsSystem)
                    nodes = AuthenticationUtil.runAsSystem(() -> new NodeCollectorLucene(lucene, luceneStore).getNodes());
                else
                    nodes = new NodeCollectorLucene(lucene, luceneStore).getNodes();
            }
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

                if(transaction.equals(TransactionMode.LocalRetrying)){
                    serviceRegistry.getRetryingTransactionHelper().doInTransaction(() -> {
                        runTask(ref);
                        return null;
                    });
                }
                else {
                    runTask(ref);
                }
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
            Stream<NodeRef> filteredStream = nodes.stream().filter(callFilter);
            if (threaded) {
                ExecutorService executor = Executors.newFixedThreadPool(OAIPMHLOMImporter.getThreadCount());
                List<Callable<Void>> threads=new ArrayList<>();
                filteredStream.forEach((ref)->{
                    threads.add(()-> {
                        callTask.accept(ref);
                        return null;
                    });
                });
                try {
                    executor.invokeAll(threads);
                    executor.shutdown();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            else
                filteredStream.forEach(callTask);
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

    private void runTask(NodeRef ref) {
        try {
            if (keepModifiedDate)
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
        }finally {
            policyBehaviourFilter.enableBehaviour(ref);
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

    public RecurseMode getRecurseMode() {
        return recurseMode;
    }

    public void setRecurseMode(RecurseMode recurseMode) {
        this.recurseMode = recurseMode;
    }

    public void setLuceneStore(StoreRef luceneStore) {
        this.luceneStore = luceneStore;
    }

    public StoreRef getLuceneStore() {
        return luceneStore;
    }

    public enum TransactionMode{
        None, // no transactions
        Global, // for whole task
        Local, // per node
        LocalRetrying // per node, with retrying transaction
    }
}
