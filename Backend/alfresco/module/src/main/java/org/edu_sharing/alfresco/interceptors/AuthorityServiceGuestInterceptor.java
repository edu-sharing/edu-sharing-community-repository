package org.edu_sharing.alfresco.interceptors;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;

public class AuthorityServiceGuestInterceptor implements MethodInterceptor {

    Logger logger = Logger.getLogger(AuthorityServiceGuestInterceptor.class);

    @Override
    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
        String methodName = methodInvocation.getMethod().getName();
        if(!methodName.equals("addAuthority")){
            return methodInvocation.proceed();
        }

        String guestUsername = ApplicationInfoList.getHomeRepository().getGuest_username();
        if(guestUsername == null || guestUsername.trim().isEmpty()){
            return methodInvocation.proceed();
        }
        String childName = (String)methodInvocation.getArguments()[1];
        if(childName == null || !childName.equals(guestUsername)){
            return methodInvocation.proceed();
        }

        logger.error("someone tried to add "+guestUsername+" to a group. this is not allowed");
        return null;

    }
}
