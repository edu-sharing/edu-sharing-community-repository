package org.edu_sharing.repository.server.jobs.quartz;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.edu_sharing.spring.security.openid.persistence.OidcUserSessionMapper;
import org.edu_sharing.spring.tomcat.SessionChecker;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Set;

/**
 * Removes entries of the oidc session registry whose session does not exist anymore.
 *
 * Entries are usually removed on logout, on back channel logout and - for sessions that ran into the tomcat
 * session timeout - by the SessionListener. This job is the backstop for everything none of them sees,
 * for example sessions lost with a crashed node or a restarted redis.
 *
 * Two conditions have to hold before a row is deleted: the entry has to be older than {@link #maxAge},
 * and the session store has to confirm that the session is gone. The age alone would delete
 * entries of users that are still working with a long living session and would silently break their back
 * channel logout, so it is only used to preselect candidates and to limit the damage of a wrong lookup.
 */
@Slf4j
@JobDescription(description = "cleanup entries managed by MyBatisOidcSessionRegistry whose session does not exist anymore")
public class OidcSessionRegistryCleanupJob extends AbstractInterruptableJob {

    private static final String DEFAULT_MAX_AGE = "24 hours";
    private static final int DEFAULT_BATCH_SIZE = 1000;
    private static final int DEFAULT_MAX_ENTRIES = 50000;

    @Autowired
    private SessionChecker sessionChecker;

    @Autowired
    private OidcUserSessionMapper mapper;

    @JobFieldDescription(description = "only entries that were created longer than this ago are candidates, postgres interval syntax", sampleValue = DEFAULT_MAX_AGE)
    private String maxAge;

    @JobFieldDescription(description = "number of candidates fetched and checked per iteration")
    private Integer batchSize;

    @JobFieldDescription(description = "maximum number of candidates checked in one run, 0 for unlimited")
    private Integer maxEntries;

    @JobFieldDescription(description = "allow the cleanup without a shared session store. only correct for single node installations, on a cluster it would delete the entries of all other nodes")
    private Boolean allowLocalSessionManager;

    @Override
    protected void executeInterruptable(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        String age = StringUtils.defaultIfBlank(maxAge, DEFAULT_MAX_AGE).trim();
        int pageSize = (batchSize == null || batchSize < 1) ? DEFAULT_BATCH_SIZE : batchSize;
        int limit = (maxEntries == null || maxEntries < 0) ? DEFAULT_MAX_ENTRIES : maxEntries;

        SessionChecker.Mode mode = sessionChecker.getMode();
        log.info("starting oidc session registry cleanup, maxAge: {}, session store: {}", age, mode);

        if (mode == SessionChecker.Mode.UNAVAILABLE
                || (mode == SessionChecker.Mode.LOCAL && !Boolean.TRUE.equals(allowLocalSessionManager))) {
            log.warn("session store is '{}', it can not be determined reliably whether a session is still alive. "
                    + "nothing will be removed. set allowLocalSessionManager to true for single node installations", mode);
            return;
        }

        String cursor = "";
        int checked = 0;
        int removed = 0;

        while (!Thread.currentThread().isInterrupted()) {
            int fetch = (limit > 0) ? Math.min(pageSize, limit - checked) : pageSize;
            if (fetch < 1) {
                log.info("reached the limit of {} candidates for this run, the rest is handled by the next one", limit);
                break;
            }

            List<String> candidates = mapper.findOutdatedSessionIds(age, cursor, fetch);
            if (candidates.isEmpty()) {
                break;
            }
            cursor = candidates.get(candidates.size() - 1);
            checked += candidates.size();

            Set<String> dead = sessionChecker.findDeadSessions(candidates);
            if (!dead.isEmpty()) {
                removed += mapper.deleteBySessionIds(dead);
            }

            if (candidates.size() < fetch) {
                break;
            }
        }

        log.info("finished oidc session registry cleanup, checked {} outdated entries, removed {}", checked, removed);
    }
}
