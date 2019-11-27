package org.edu_sharing.service.permission;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.authentication.Context;
import org.edu_sharing.repository.server.tools.security.SignatureVerifier;
import org.edu_sharing.service.nodeservice.NodeServiceInterceptor;

import java.util.HashMap;

public class PermissionServiceInterceptor implements MethodInterceptor {

    public void init(){

    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        String methodName=invocation.getMethod().getName();
        if(methodName.equals("hasPermission") || methodName.equals("hasAllPermissions")) {
            String nodeId = (String) invocation.getArguments()[2];
            if(methodName.equals("hasPermission")){
                boolean result = (boolean) invocation.proceed();
                // to improve performance, if node seems to have already valid permission, return true
                if(result)
                    return result;
            }
            else if(methodName.equals("hasAllPermissions")){
                HashMap<String, Boolean> result = (HashMap<String, Boolean>) invocation.proceed();
                // to improve performance, if node seems to have any valid permissions, return true
                if(result.values().stream().anyMatch((v) -> v)){
                    return result;
                }
            }

            return NodeServiceInterceptor.handleInvocation(nodeId, invocation,false);
        }
        return invocation.proceed();
    }
}
