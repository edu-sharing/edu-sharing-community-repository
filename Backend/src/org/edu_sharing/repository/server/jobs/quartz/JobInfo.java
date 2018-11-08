package org.edu_sharing.repository.server.jobs.quartz;

import org.apache.log4j.Level;
import org.quartz.JobDetail;

import java.util.ArrayList;
import java.util.List;

public class JobInfo {
    private static final int MAX_LOG_ENTRIES = 5000;

    public JobInfo(JobDetail jobDetail) {
        setJobDetail(jobDetail);
        setStartTime(System.currentTimeMillis());
        setStatus(Status.Running);
    }

    public enum Status{
        Running,
        Failed,
        Aborted,
        Finished
    }
    public static class LogEntry{
        private String className;
        private Level level;
        private long date;
        private String message;

        public LogEntry(Level level, long date, String className, String message) {
            this.level=level;
            this.date = date;
            this.className = className;
            this.message=message;
        }

        public String getClassName() {
            return className;
        }

        public void setClassName(String className) {
            this.className = className;
        }

        public long getDate() {
            return date;
        }

        public void setDate(long date) {
            this.date = date;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Level getLevel() {
            return level;
        }

        public void setLevel(Level level) {
            this.level = level;
        }
    }
    private long startTime,finishTime;
    private Status status;
    private Level worstLevel=Level.ALL;
    private List<LogEntry> log=new ArrayList<>();
    private JobDetail jobDetail;


    public JobDetail getJobDetail() {
        return jobDetail;
    }

    public void setJobDetail(JobDetail jobDetail) {
        this.jobDetail = jobDetail;
    }

    public void addLog(LogEntry entry) {
        this.log.add(entry);
        if(entry.level.isGreaterOrEqual(worstLevel)){
            worstLevel=entry.level;
        }
        if(this.log.size()>MAX_LOG_ENTRIES){
            this.log.remove(0);
        }
    }

    public Level getWorstLevel() {
        return worstLevel;
    }

    public List<LogEntry> getLog() {
        return log;
    }

    public void setLog(List<LogEntry> log) {
        this.log = log;
    }

    public long getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(long finishTime) {
        this.finishTime = finishTime;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }


    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }
}
