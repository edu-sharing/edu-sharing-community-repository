package org.edu_sharing.repository.server.jobs;

public class JobQueueContextHolder {
    private static final ThreadLocal<JobQueueContext> context = new ThreadLocal<>();

    public static JobQueueContext getJobQueueContext() {
        JobQueueContext jobQueueContext = context.get();
        if (jobQueueContext == null) {
            jobQueueContext = new JobQueueContext();
            context.set(jobQueueContext);
        }
        return jobQueueContext;
    }

    public static void clear() {
        context.remove();
    }

}

