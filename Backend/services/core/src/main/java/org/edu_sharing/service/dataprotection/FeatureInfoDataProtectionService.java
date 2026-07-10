package org.edu_sharing.service.dataprotection;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.edu_sharing.repository.server.jobs.quartz.DataProtectionExportJob;
import org.edu_sharing.repository.server.jobs.quartz.JobHandler;
import org.edu_sharing.restservices.about.v1.model.FeatureInfo;
import org.edu_sharing.spring.conditions.ConditionalOnProperty;
import org.quartz.Trigger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.Map;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "repository.dataprotection.enabled", havingValue = "true")
public class FeatureInfoDataProtectionService implements FeatureInfo {

    private final JobHandler jobHandler;

    @Value("${repository.dataprotection.cronExpression:Cron[0 * * * * ?]}")
    private String cronExpression;

    @PostConstruct
    public void init() {
        String jobName = DataProtectionExportJob.class.getName();
        Trigger trigger;
        try {
            trigger = jobHandler.getTriggerFromString(jobName, cronExpression);
            jobHandler.add(new JobHandler.JobConfig(DataProtectionExportJob.class, trigger, Map.of(),jobName));
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public FeatureInfo.Features getId() {
        return Features.dataprotection;
    }
}
