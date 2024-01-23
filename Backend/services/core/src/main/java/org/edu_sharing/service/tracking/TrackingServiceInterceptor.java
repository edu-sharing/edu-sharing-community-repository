package org.edu_sharing.service.tracking;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.service.nodeservice.NodeServiceInterceptor;
import org.springframework.context.ApplicationContext;

public class TrackingServiceInterceptor implements MethodInterceptor {
    static Logger logger = Logger.getLogger(TrackingServiceInterceptor.class);

    public void init() {

    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        String methodName=invocation.getMethod().getName();
        if(methodName.equals("trackActivityOnNode")) {
            return NodeServiceInterceptor.handleInvocation(((NodeRef) invocation.getArguments()[0]).getId(), invocation, false);
        }
        return invocation.proceed();
    }
}
