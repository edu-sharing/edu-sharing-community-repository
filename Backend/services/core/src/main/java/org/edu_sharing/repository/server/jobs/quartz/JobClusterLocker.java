package org.edu_sharing.repository.server.jobs.quartz;

/**
 * default implementation used in none cluster environments
 */
public class JobClusterLocker {

    public boolean tryLock(String lock){
        return true;
    }

    public void releaseLock(String lock){

    }

    public interface ClusterSingelton{}
}
