package org.edu_sharing.repository.server.tools;

import org.apache.log4j.Logger;

import java.util.concurrent.Callable;

public class LogTime {
    public static Logger logger=Logger.getLogger(LogTime.class);

    // how long a execution should minimum take to be logged with the specific level
    private static int THRESHOLD_DEBUG=100;
    private static int THRESHOLD_INFO=500;
    private static int THRESHOLD_WARN=5000;
    public static<T> T log(String name,Callable<T> callable) throws Exception {
        long time=System.currentTimeMillis();
        T result=callable.call();
        logInternal(name,time);
        return result;
    }
    public static void log(String name,Runnable runnable) {
        long time=System.currentTimeMillis();
        runnable.run();
        logInternal(name,time);
    }
    private static void logInternal(String name,long time) {
        time=System.currentTimeMillis()-time;
        String message="Executing \""+name+"\" took "+time+"ms";
        if(time>THRESHOLD_WARN){
            logger.warn(message);
        }
        else if(time>THRESHOLD_INFO){
            logger.info(message);
        }
        else if(time>THRESHOLD_DEBUG){
            logger.debug(message);
        }
    }
}
