package org.edu_sharing.service.nodeservice.aspects;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.edu_sharing.service.AspectConstants;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.annotation.NodeOriginal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

@Aspect
@Order(AspectConstants.ORDER.NodeOriginalAspect)
@Component
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class NodeOriginalAspect {
    final NodeService nodeService;
    @Around("@annotation(org.edu_sharing.service.nodeservice.annotation.NodeManipulation)")
    public Object mapNodeOriginal(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Class<?> targetClass = joinPoint.getTarget().getClass();
        Method method = targetClass.getMethod(signature.getName(), signature.getParameterTypes());
        Object[] args = joinPoint.getArgs();

        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            if (parameter.getAnnotation(NodeOriginal.class) != null) {
                Object arg = args[i];
                args[i] = nodeService.getOriginalNode((String) arg).getId();
            }
        }
        return joinPoint.proceed(args);
    }
}
