package org.edu_sharing.repository.server.jobs.quartz;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.*;
import org.apache.log4j.spi.LoggingEvent;
import org.edu_sharing.repository.server.tools.cache.ShibbolethSessionsCache;

import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JobLogger extends ConsoleAppender {

    public static final List<String> IGNORABLE_JOBS = new ArrayList<>();
    static{
        IGNORABLE_JOBS.add(SystemStatisticJob.class.getName());
        IGNORABLE_JOBS.add("org.edu_sharing.repository.server.jobs.quartz.ClusterInfoJob");
    }

    public static String getLogsForJob(String className){
        String result="";
        //logs.get(className);
        return result;
    }

    public JobLogger() {
        init();
    }

    public JobLogger(Layout layout) {
        super(layout);
        init();
    }

    private void init() {
    }

    @Override
    protected void subAppend(LoggingEvent event) {
        //super.subAppend(event);
        if(event.getLoggerName().equals(JobHandler.class.getName())){
            return;
        }
        try {
            for(JobInfo job : JobHandler.getInstance().getAllJobs()){
                if(!job.getStatus().equals(JobInfo.Status.Running))
                    continue;
                String clazz=job.getJobDetail().getJobClass().getName();
                String message=event.getRenderedMessage();
                if(event.getThrowableStrRep()!=null){
                    message+="\n\n" + StringUtils.join(event.getThrowableStrRep(),"\n");
                }
                if(clazz.equals(event.getLoggerName())){
                    job.addLog(new JobInfo.LogEntry(event.getLevel(),event.timeStamp,event.getLoggerName(),message));
                }
                // importer job mapping
                if(clazz.equals(ImporterJob.class.getName()) && event.getLoggerName().startsWith("org.edu_sharing.repository.server.importer")){
                    job.addLog(new JobInfo.LogEntry(event.getLevel(),event.timeStamp,event.getLoggerName(),message));
                }
            }
        } catch (Exception e) {

        }
    }
}
