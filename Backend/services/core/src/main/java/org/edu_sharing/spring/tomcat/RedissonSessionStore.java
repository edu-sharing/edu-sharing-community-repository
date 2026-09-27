package org.edu_sharing.spring.tomcat;

import org.apache.catalina.Manager;
import org.redisson.api.RMap;
import org.redisson.tomcat.RedissonSessionManager;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Access to the redis backed tomcat session store.
 *
 * Isolated in an own class so that the redisson classes are only resolved when redis session management
 * is actually in use - the jars live in tomcat/lib and are optional.
 */
final class RedissonSessionStore {

    private static final String MANAGER_CLASS = "org.redisson.tomcat.RedissonSessionManager";

    private RedissonSessionStore() {
    }

    /**
     * Class name based on purpose, so that no redisson class has to be loaded for the check itself.
     */
    static boolean isSupported(Manager manager) {
        for (Class<?> clazz = manager.getClass(); clazz != null; clazz = clazz.getSuperclass()) {
            if (MANAGER_CLASS.equals(clazz.getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether the session hashes still exist in redis. This is an {@code EXISTS} per id and nothing else:
     * no session is loaded into the local session manager, no timestamp and no ttl is written back.
     *
     * The key name is taken from {@link RedissonSessionManager#getMap(String)} so that a configured
     * {@code keyPrefix} is applied automatically.
     */
    static Set<String> findMissing(Manager manager, Collection<String> sessionIds) {
        RedissonSessionManager sessionManager = (RedissonSessionManager) manager;
        Set<String> missing = new HashSet<>();
        for (String sessionId : sessionIds) {
            RMap<String, Object> session = sessionManager.getMap(sessionId);
            if (!session.isExists()) {
                missing.add(sessionId);
            }
        }
        return missing;
    }
}
