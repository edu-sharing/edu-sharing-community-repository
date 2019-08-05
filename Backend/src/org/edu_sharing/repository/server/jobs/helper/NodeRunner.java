package org.edu_sharing.repository.server.jobs.helper;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * This is a class that will do a task for all nodes with a given type (or all) in a given folder
 */
public class NodeRunner {
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

        List<NodeRef> nodes;
        if(runAsSystem)
            nodes = AuthenticationUtil.runAsSystem(()->nodeService.getChildrenRecursive(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,startFolder,types));
        else
            nodes = nodeService.getChildrenRecursive(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,startFolder,types);

        Predicate<? super NodeRef> callFilter=(ref)->{
            if(filter==null)
                return true;
            if(runAsSystem){
                return AuthenticationUtil.runAsSystem(()->filter.test(ref));
            }
            else{
                return filter.test(ref);
            }
        };
        Consumer<? super NodeRef> callTask=(ref)->{
            if(runAsSystem){
                AuthenticationUtil.runAsSystem(()->{
                    task.accept(ref);
                    return null;
                });
            }
            else{
                task.accept(ref);
            }
        };
        if(threaded)
            nodes.parallelStream().filter(callFilter).forEach(callTask);
        else
            nodes.stream().filter(callFilter).forEach(callTask);
        return nodes.size();

    }
}
