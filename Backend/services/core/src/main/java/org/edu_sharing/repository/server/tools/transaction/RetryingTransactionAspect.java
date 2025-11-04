package org.edu_sharing.repository.server.tools.transaction;

import lombok.RequiredArgsConstructor;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 101)
public class RetryingTransactionAspect {

    private final RetryingTransactionHelper retryingTransactionHelper;

    @Around("@annotation(RetryingTransaction)")
    public Object runInRetryingTransaction(ProceedingJoinPoint joinPoint) throws NoSuchMethodException {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Class<?> targetClass = joinPoint.getTarget().getClass();
        Method method = targetClass.getMethod(signature.getName(), signature.getParameterTypes());
        RetryingTransaction annotation = AnnotationUtils.findAnnotation(method, RetryingTransaction.class);

        return retryingTransactionHelper.doInTransaction(() -> {
            try {
                return joinPoint.proceed();
            } catch (RuntimeException e) {
                throw e;
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }, annotation.readonly(), annotation.requiresNew());
    }
}
