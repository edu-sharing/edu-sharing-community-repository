package org.edu_sharing.service.scope;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.edu_sharing.service.AspectConstants;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * Enforces {@link ScopesFeature}. Runs before the permission checks, so a disabled feature always answers the same way.
 */
@Aspect
@Order(AspectConstants.ORDER.ScopesFeature)
@Component
@RequiredArgsConstructor
public class ScopesFeatureAspect {

    private final ScopesSettings scopesSettings;

    @Before("@within(org.edu_sharing.service.scope.ScopesFeature) || @annotation(org.edu_sharing.service.scope.ScopesFeature)")
    public void checkEnabled(JoinPoint joinPoint) {
        if (scopesSettings.isEnabled()) {
            return;
        }
        Method method = AopUtils.getMostSpecificMethod(((MethodSignature) joinPoint.getSignature()).getMethod(), joinPoint.getTarget().getClass());
        // method level (incl. interface methods) wins over the type level
        ScopesFeature feature = AnnotationUtils.findAnnotation(method, ScopesFeature.class);
        if (feature == null) {
            feature = AnnotationUtils.findAnnotation(joinPoint.getTarget().getClass(), ScopesFeature.class);
        }
        if (feature != null && feature.requiresEnabled()) {
            throw new ScopesDisabledException();
        }
    }
}
