package org.edu_sharing.spring.tomcat;

import jakarta.servlet.ServletContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.Manager;
import org.apache.catalina.Session;
import org.apache.catalina.core.StandardContext;
import org.edu_sharing.alfresco.monitoring.TomcatUtil;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Read only lookups against the tomcat session store.
 *
 * Everything in here has to stay free of side effects. In particular {@link Manager#findSession(String)}
 * must not be used: on a redis backed session store it writes the current access timestamp and a fresh ttl
 * back to redis, registers a copy of the session in the local session manager and fires
 * {@code HttpSessionListener#sessionCreated} - which prolongs every session it looks at.
 */
@Slf4j
@Component
public class SessionChecker {

    public enum Mode {
        /** sessions of all cluster nodes can be resolved (redis backed session store) */
        SHARED,
        /** only sessions of the local tomcat can be resolved, which is correct for single node installations only */
        LOCAL,
        /** no session store available at all */
        UNAVAILABLE
    }

    private final ServletContext servletContext;

    public SessionChecker(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    public Mode getMode() {
        Manager manager = getManager();
        if (manager == null) {
            return Mode.UNAVAILABLE;
        }
        return RedissonSessionStore.isSupported(manager) ? Mode.SHARED : Mode.LOCAL;
    }

    /**
     * @return the subset of the given session ids whose session definitely does not exist anymore.
     * Ids that can not be judged are never part of the result, so a failing lookup never causes a deletion.
     */
    public Set<String> findDeadSessions(Collection<String> sessionIds) {
        if (sessionIds == null || sessionIds.isEmpty()) {
            return Collections.emptySet();
        }

        Manager manager = getManager();
        if (manager == null) {
            log.warn("no session manager available, no session is treated as dead");
            return Collections.emptySet();
        }

        if (RedissonSessionStore.isSupported(manager)) {
            try {
                return RedissonSessionStore.findMissing(manager, sessionIds);
            } catch (Throwable t) {
                log.warn("could not query the shared session store, no session is treated as dead", t);
                return Collections.emptySet();
            }
        }

        Session[] sessions = manager.findSessions();
        if (sessions == null) {
            return Collections.emptySet();
        }
        Set<String> alive = Arrays.stream(sessions).map(Session::getId).collect(Collectors.toSet());
        return sessionIds.stream().filter(id -> !alive.contains(id)).collect(Collectors.toSet());
    }

    private Manager getManager() {
        try {
            StandardContext context = new TomcatUtil().getContext(servletContext.getContextPath());
            if (context == null) {
                log.warn("no tomcat context found for {}", servletContext.getContextPath());
                return null;
            }
            return context.getManager();
        } catch (Throwable t) {
            log.warn("could not resolve the tomcat session manager", t);
            return null;
        }
    }
}
