package org.edu_sharing.repository.server.tools;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;

public class EduSharingLockHelper {
    private static void acquire(Lock lock) {
        try {
            if(!lock.tryLock(10, TimeUnit.SECONDS)) {
                throw new TimeoutException();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    /**
     * Run the given method only once at a time, also across the cluster (if enabled)
     * @param clazz The class, used for scoping the keyName given to lock
     * @param keyName The key to lock your method, e.g. your method name or dynamic data if the lock should be only for a specific parameter value
     * @param runnable The runnable to run only once at a time for the given clazz + keyName combination
     */
    public static void runSingleton(Class clazz, String keyName, Runnable runnable) {
        Lock lock = EduSharingLockManager.getLock(clazz, keyName);
        try {
            EduSharingLockHelper.acquire(lock);
            try {
                runnable.run();
            } finally {
                lock.unlock();
            }
        } finally {
            EduSharingLockManager.cleanupLock(lock);
        }
    }
}
