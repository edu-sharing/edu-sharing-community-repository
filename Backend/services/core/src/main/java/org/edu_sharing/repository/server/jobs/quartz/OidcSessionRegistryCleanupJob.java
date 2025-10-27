package org.edu_sharing.repository.server.jobs.quartz;

import lombok.extern.slf4j.Slf4j;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.spring.security.openid.persistence.OidcUserSessionMapper;
import org.edu_sharing.spring.tomcat.SessionChecker;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

@Slf4j
@JobDescription(description = "cleanup entries managed by MyBatisOidcSessionRegistry")
public class OidcSessionRegistryCleanupJob extends AbstractInterruptableJob {

    @Autowired
    SessionChecker sessionChecker;

    @Autowired
    private OidcUserSessionMapper mapper;

    @Override
    protected void executeInterruptable(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        mapper.findAll().forEach(session -> {
            try {
                if(!sessionChecker.isSessionAlive(session.getSessionId())){
                    log.info("Session {} does not exists or expired. will cleanup", session.getSessionId());
                    mapper.deleteBySessionId(session.getSessionId());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
