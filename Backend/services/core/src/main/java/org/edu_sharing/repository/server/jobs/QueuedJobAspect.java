package org.edu_sharing.repository.server.jobs;

import com.google.common.base.Defaults;
import lombok.Setter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.codehaus.jackson.map.ObjectMapper;
import org.edu_sharing.repository.server.jobs.annotations.Queued;
import org.edu_sharing.repository.server.jobs.ibatis.JobQueueMapper;
import org.edu_sharing.util.CheckedFunction;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.format.datetime.standard.DurationFormatterUtils;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Date;
import java.util.Objects;

@Aspect
@Component
public class QueuedJobAspect {

    private JobQueueMapper jobQueueMapper;
    @Setter

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public void setJobQueueMapper(JobQueueMapper jobQueueMapper) {
        this.jobQueueMapper = jobQueueMapper;
    }

    @Around("@annotation(org.edu_sharing.repository.server.jobs.annotations.Queued)")
    public Object queuedJob(ProceedingJoinPoint joinPoint) throws Throwable {
        JobQueueContext jobQueueContext = JobQueueContextHolder.getJobQueueContext();
        if (jobQueueContext.isDisableQueuing()) {
            return joinPoint.proceed();
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Queued annotation = signature.getMethod().getAnnotation(Queued.class);

        Object target = joinPoint.getTarget();
        Class<?> targetClass = AopUtils.getTargetClass(target);
        Object[] args = joinPoint.getArgs();
        String user = AuthenticationUtil.getRunAsUser();

        JobQueueEntry entry = new JobQueueEntry(
                null,
                annotation.unique(),
                annotation.group(),
                new Date(),
                null,
                JobStatus.PENDING,
                DurationFormatterUtils.detectAndParse(annotation.ttl()),
                Objects.hash(targetClass.getName(), signature.getMethod().getName(), Arrays.deepHashCode(args), user),
                targetClass,
                signature.getMethod().getName(),
                signature.getMethod().getParameterTypes(),
                Arrays.stream(args).map(CheckedFunction.wrap(objectMapper::writeValueAsString)).toArray(String[]::new),
                user
        );

        try {
            jobQueueMapper.insert(entry);
            jobQueueContext.addQueuedJob(entry);
        } catch (DuplicateKeyException t){
            throw new DuplicateJobException("Job was already scheduled", t);
        }
        return Defaults.defaultValue(signature.getMethod().getReturnType());
    }
}
