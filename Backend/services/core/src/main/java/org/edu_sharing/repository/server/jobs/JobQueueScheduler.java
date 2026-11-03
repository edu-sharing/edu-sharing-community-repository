package org.edu_sharing.repository.server.jobs;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.edu_sharing.repository.server.jobs.ibatis.JobQueueMapper;
import org.edu_sharing.util.CheckedFunction;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobQueueScheduler {

    private final JobQueueMapper jobQueueMapper;
    private final BeanFactory beanFactory;

    @Setter
    @Value("${jobs.primaryHostname:}")
    private String primaryHostname;

    @Scheduled(fixedDelayString = "${jobs.cleanUpInterval}")
    public void purgeOldJobs() {
        if(!isPrimaryRepository()){
            return;
        }

        log.debug("purging old jobs");
        JobQueueEntry[] jobQueueEntries = jobQueueMapper.deleteExpired();
        if (jobQueueEntries.length != 0) {
            log.info("purged jobs: {}", Arrays.stream(jobQueueEntries).map(JobQueueEntry::toString).collect(Collectors.joining("\n")));
        } else {
            log.debug("no old jobs to purge");
        }
    }

    private boolean isPrimaryRepository() {
        if (StringUtils.isNotBlank(primaryHostname)) {
            try {
                return Arrays.asList(
                        InetAddress.getLocalHost().getHostName(),
                        InetAddress.getLocalHost().getHostName().split("\\.")[0]
                ).contains(primaryHostname);
            } catch (UnknownHostException e) {
                log.warn("Could not resolve hostname", e);
                return false;
            }
        } else {
            log.debug("No primaryHostname key, assuming no cluster, jobs are active on this repository");
            return true;
        }

    }

    /**
     * runs the queued jobs one after another until the queue is empty, the interval only applies while it is empty
     * (otherwise every job, even a tiny one, would wait for the next interval)
     */
    @Scheduled(fixedDelayString = "${jobs.fetchNextInterval}")
    public void retrieveJobFromJobQueue() {
        while (runNextJob()) {
            // next one
        }
    }

    /**
     * @return false if the queue was empty
     */
    boolean runNextJob() {
        JobQueueEntry nextJob = jobQueueMapper.getNext();
        if (nextJob == null) {
            log.debug("no next job");
            return false;
        }
        log.debug("next job: {}", nextJob);
        ObjectMapper objectMapper = new ObjectMapper();

        JobQueueContextHolder.clear();
        JobQueueContext jobQueueContext = JobQueueContextHolder.getJobQueueContext();
        try {
            Object target = beanFactory.getBean(nextJob.getBean());
            Method method = ReflectionUtils.findMethod(AopUtils.getTargetClass(target), nextJob.getMethod(), nextJob.paramTypes);

            if (method == null) {
                log.error("no method found for target: {} method: {}", nextJob.getBean(), nextJob.getMethod());
                return true;
            }
            log.debug("found method: {}", method);

            jobQueueContext.setDisableQueuing(true);
            jobQueueContext.setRunningJob(nextJob);

            Object[] params = IntStream.range(0, nextJob.getParams().length)
                    .mapToObj(i -> new MethodParam(nextJob.getParams()[i], nextJob.getParamTypes()[i]))
                    .map(CheckedFunction.wrap(x -> objectMapper.readValue(x.jsonValue(), x.type())))
                    .toArray();

            AuthenticationUtil.runAs(
                    () -> ReflectionUtils.invokeMethod(method, target, params),
                    nextJob.getUser());

        } catch (Throwable e) {
            log.error("failed to execute job: {} with: {}", nextJob, e.getMessage(), e);
        } finally {
            jobQueueContext.setRunningJob(null);
            jobQueueContext.setDisableQueuing(false);
            jobQueueMapper.delete(nextJob);
        }
        return true;
    }

    record MethodParam(String jsonValue, Class<?> type) {
    }
}
