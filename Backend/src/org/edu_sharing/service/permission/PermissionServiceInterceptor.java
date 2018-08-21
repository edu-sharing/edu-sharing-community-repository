package org.edu_sharing.service.permission;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.authentication.Context;
import org.edu_sharing.repository.server.tools.security.SignatureVerifier;
import org.edu_sharing.service.nodeservice.NodeServiceInterceptor;

public class PermissionServiceInterceptor implements MethodInterceptor {

    public void init(){

    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        String methodName=invocation.getMethod().getName();
        if(methodName.equals("hasPermission") || methodName.equals("hasAllPermissions")) {
            String nodeId = (String) invocation.getArguments()[2];
            System.out.println("invoke "+methodName+" "+nodeId);
            return NodeServiceInterceptor.handleInvocation(nodeId, invocation);
        }
        return invocation.proceed();
    }
}
