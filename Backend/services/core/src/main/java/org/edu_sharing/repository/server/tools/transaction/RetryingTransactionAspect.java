package org.edu_sharing.repository.server.tools.transaction;

import lombok.RequiredArgsConstructor;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE+101)
public class RetryingTransactionAspect {

    private final RetryingTransactionHelper retryingTransactionHelper;

    @Around("@annotation(RetryingTransaction)")
    public Object runAsSystem(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        RetryingTransaction annotation = signature.getMethod().getAnnotation(RetryingTransaction.class);

        return retryingTransactionHelper.doInTransaction(() -> {
            try {
                return joinPoint.proceed();
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }, annotation.readonly(), annotation.requiresNew());
    }
}
