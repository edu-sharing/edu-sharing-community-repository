package org.edu_sharing.repository.server.jobs.quartz;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

import java.util.*;

@Plugin(
    name = "JobLogger",
    category = Core.CATEGORY_NAME,
    elementType = Appender.ELEMENT_TYPE)
public class JobLogger extends AbstractAppender {

    public static final List<String> IGNORABLE_JOBS = new ArrayList<>();
    static{
        IGNORABLE_JOBS.add(SystemStatisticJob.class.getName());
        IGNORABLE_JOBS.add("org.edu_sharing.repository.server.jobs.quartz.ClusterInfoJob");
    }

    public JobLogger(String name, Filter filter) {
        super(name, filter, null);
    }

    @PluginFactory
    public static JobLogger createAppender(
            @PluginAttribute("name") String name,
            @PluginElement("Filter") Filter filter) {
        return new JobLogger(name, filter);
    }

    @Override
    public void append(LogEvent event) {
        if(event.getLoggerName().equals(JobHandler.class.getName())){
            return;
        }
        try {
            for(JobInfo job : JobHandler.getInstance().getAllJobs()){
                if(!job.getStatus().equals(JobInfo.Status.Running))
                    continue;
                String clazz=job.getJobDetail().getJobClass().getName();
                String message=event.getMessage().getFormattedMessage();
                if(event.getThrown()!=null){
                    message+="\n\n" + StringUtils.join(event.getThrown().getStackTrace(),"\n");
                }
                if(clazz.equals(event.getLoggerName())){
                    job.addLog(new JobInfo.LogEntry(org.apache.log4j.Level.toLevel(event.getLevel().name()),event.getInstant().getEpochMillisecond(),event.getLoggerName(),message));
                }
                // importer job mapping
                if(clazz.equals(ImporterJob.class.getName()) && event.getLoggerName().startsWith("org.edu_sharing.repository.server.importer")){
                    job.addLog(new JobInfo.LogEntry(org.apache.log4j.Level.toLevel(event.getLevel().name()),event.getInstant().getEpochMillisecond(),event.getLoggerName(),message));
                }
            }
        } catch (Exception e) {

        }
    }
}
