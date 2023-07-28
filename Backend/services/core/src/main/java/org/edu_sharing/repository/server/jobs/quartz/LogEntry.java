package org.edu_sharing.repository.server.jobs.quartz;

import org.apache.log4j.Level;

public class LogEntry{
    String className;
    Level level;
    long date;
    String message;

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