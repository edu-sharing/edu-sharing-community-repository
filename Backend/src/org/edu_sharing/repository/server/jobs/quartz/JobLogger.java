package org.edu_sharing.repository.server.jobs.quartz;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.*;
import org.apache.log4j.spi.LoggingEvent;

import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JobLogger extends ConsoleAppender {

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
        System.out.println("Inited logger for jobs");
    }

    @Override
    protected void subAppend(LoggingEvent event) {
        super.subAppend(event);
        if(event.getLoggerName().equals(JobHandler.class.getName())){
            System.out.println("ignore jobhandler");
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
                    System.out.println("job found for "+event.getLoggerName());
                    job.addLog(new JobInfo.LogEntry(event.getLevel(),event.timeStamp,message));
                }
                // importer job mapping
                if(clazz.equals(ImporterJob.class.getName()) && event.getLoggerName().startsWith("org.edu_sharing.repository.server.importer")){
                    System.out.println("import job found for "+event.getLoggerName());
                    job.addLog(new JobInfo.LogEntry(event.getLevel(),event.timeStamp,message));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("no job found for "+event.getLoggerName());
    }
}
