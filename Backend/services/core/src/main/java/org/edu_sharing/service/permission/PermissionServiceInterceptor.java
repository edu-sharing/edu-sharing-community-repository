package org.edu_sharing.service.permission;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.nodeservice.NodeServiceInterceptor;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class PermissionServiceInterceptor implements MethodInterceptor {

    public void init(){

    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        String methodName=invocation.getMethod().getName();
        if(methodName.equals("hasPermission") || methodName.equals("hasAllPermissions")) {
            String nodeId = (String) invocation.getArguments()[2];
            Object data = invocation.getArguments()[3];
            if(methodName.equals("hasPermission")){
                boolean result = (boolean) invocation.proceed();
                // to improve performance, if node seems to have already valid permission, return true
                if(result)
                    return result;
                if(!CCConstants.getUsagePermissions().contains((String) data)){
                    return false;
                }
                return NodeServiceInterceptor.handleInvocation(nodeId, invocation,false);
            }
            else if(methodName.equals("hasAllPermissions")){
                HashMap<String, Boolean> result = (HashMap<String, Boolean>) invocation.proceed();
                // to improve performance, if node seems to have any valid permissions, return true
                if(result.values().stream().anyMatch((v) -> v)){
                    return result;
                }
                // fetch all permissions but only allow the onces that are allowed for usages
                result = (HashMap<String, Boolean>) NodeServiceInterceptor.handleInvocation(nodeId, invocation,false);
                return new HashMap<String, Boolean>(result.entrySet().stream().map((e) -> {
                    e.setValue(e.getValue() && CCConstants.getUsagePermissions().contains(e.getKey()));
                    return e;
                }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
            }

        }
        return invocation.proceed();
    }
}
