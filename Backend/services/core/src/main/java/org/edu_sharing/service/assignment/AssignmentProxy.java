package org.edu_sharing.service.assignment;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AssignmentProxy {

    @Around(value = "execution(* org.edu_sharing.service.assignment.AssignmentDaoFactory*.*(..))")
    public Object around(ProceedingJoinPoint pjp) {
        return AuthenticationUtil.runAsSystem(() -> {
            try {
                return pjp.proceed();
            } catch (RuntimeException e) {
                throw e;
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        });
    }
}
