package org.edu_sharing.repository.server.jobs.quartz;

import org.alfresco.repo.cache.SimpleCache;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.edu_sharing.repository.server.tools.security.AllSessions;
import org.edu_sharing.restservices.ltiplatform.v13.model.LoginInitiationSessionObject;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class LTISessionObjectCleanupJob extends AbstractJobMapAnnotationParams{

    Logger logger = Logger.getLogger(LTISessionObjectCleanupJob.class);

    @JobFieldDescription(description = "lifetime in minutes of LoginInitiationSessionObject's after last access. Should be larger than tomcat session lifetime.")
    Integer cleanupAfterInactivityMinutes = 120;

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        if(jobExecutionContext.getJobDetail().getJobDataMap().containsKey("cleanupAfterInactivityMinutes")) {
            cleanupAfterInactivityMinutes = jobExecutionContext.getJobDetail().getJobDataMap().getInt("cleanupAfterInactivityMinutes");
        }

        List<String> cleanupKeys = new ArrayList<>();

        SimpleCache<String, LoginInitiationSessionObject> userLTISessions = AllSessions.getUserLTISessions();
        for(String key : userLTISessions.getKeys()){
            LoginInitiationSessionObject loginInitiationSessionObject = userLTISessions.get(key);
            logger.info("loginInitiationSessionObject.getLastAccessed():" + loginInitiationSessionObject.getLastAccessed() +" diff ms:" + (System.currentTimeMillis() - loginInitiationSessionObject.getLastAccessed()));
            int lastAccessDuration = (int)TimeUnit.MILLISECONDS.toMinutes((System.currentTimeMillis() - loginInitiationSessionObject.getLastAccessed()) );
            logger.info("lastAccessDuration:" + lastAccessDuration + " cleanupAfterInactivityMinutes:"+cleanupAfterInactivityMinutes);
            if(lastAccessDuration > cleanupAfterInactivityMinutes){
                cleanupKeys.add(key);
            }
        }

        logger.info("will cleanup " + cleanupKeys.size() +" LoginInitiationSessionObject's");

        cleanupKeys.stream().forEach(k -> userLTISessions.remove(k));
    }
}
