package org.edu_sharing.service.nodeservice;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.namespace.QName;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.authentication.Context;
import org.edu_sharing.repository.server.tools.security.SignatureVerifier;
import org.edu_sharing.service.usage.Usage2Service;

import javax.servlet.http.HttpSession;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class NodeServiceInterceptor implements MethodInterceptor {

    public void init(){

    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        String methodName=invocation.getMethod().getName();
        if(methodName.equals("getProperty") || methodName.equals("getProperties") || methodName.equals("getAspects") || methodName.equals("getOwner")){
            String nodeId= (String) invocation.getArguments()[2];
            if(true || hasUsage(nodeId) || hasSignature(nodeId)) {
                System.out.println("Node "+nodeId+" -> will run "+methodName+" as system");
                return AuthenticationUtil.runAsSystem(()-> {
                    try {
                        return invocation.proceed();
                    } catch (Throwable throwable) {
                        throw new RuntimeException(throwable);
                    }
                });
            }
        }
        return invocation.proceed();
    }
    private boolean hasSignature(String nodeId) {
        String authSingleUseNodeId = Context.getCurrentInstance().getSessionAttribute(CCConstants.AUTH_SINGLE_USE_NODEID);
        String authSingleUseTs = Context.getCurrentInstance().getSessionAttribute(CCConstants.AUTH_SINGLE_USE_TIMESTAMP);
        System.out.println("Usage node "+authSingleUseNodeId+" "+authSingleUseTs);
        if(authSingleUseNodeId==null || authSingleUseTs==null)
            return false;
        long timestamp=Long.parseLong(authSingleUseTs);
        return (authSingleUseNodeId.equals(nodeId)
                && timestamp > (System.currentTimeMillis() - SignatureVerifier.DEFAULT_OFFSET_MS));
    }

    private boolean hasUsage(String nodeId) {
        /*List<ChildAssociationRef> usages = nodeService.getChildAssocs(nodeRef, Collections.singleton(QName.createQName(CCConstants.CCM_ASSOC_USAGEASPECT_USAGES)));
        for(ChildAssociationRef usage : usages){
            Map<QName, Serializable> props = nodeService.getProperties(usage.getChildRef());
        }
        return false;*/
        return false;
    }
}
