package org.edu_sharing.repository.server.jobs.quartz;

import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.AbstractConfiguration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
public class JobLoggerConfiguration implements ApplicationContextAware {

    @Setter
    ApplicationContext applicationContext;

    @PostConstruct
    void init(){
        Appender jobLogger = JobLogger.createAppender("JobLogger", null, applicationContext);
        jobLogger.start();

        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        AbstractConfiguration config = (AbstractConfiguration) context.getConfiguration();
        config.addAppender(jobLogger);
        LoggerConfig rootLogger = config.getRootLogger();
        rootLogger.addAppender(jobLogger, null, null);
        context.updateLoggers();
    }

}
