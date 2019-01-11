package org.edu_sharing.service.nodeservice;

import net.sf.acegisecurity.AuthenticationCredentialsNotFoundException;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.authentication.Context;
import org.edu_sharing.repository.server.tools.security.SignatureVerifier;
import org.edu_sharing.service.InsufficientPermissionException;
import org.edu_sharing.service.stream.StreamServiceFactory;
import org.edu_sharing.service.stream.StreamServiceHelper;
import org.springframework.context.ApplicationContext;

public class NodeServiceInterceptor implements MethodInterceptor {
    static Logger logger = Logger.getLogger(NodeServiceInterceptor.class);
    public void init(){
        
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        String methodName=invocation.getMethod().getName();
        int argumentId=-1;
        if(methodName.equals("getProperty") ||
                methodName.equals("getContent") ||
                methodName.equals("getContentHash") ||
                methodName.equals("getContentMimetype") ||
                methodName.equals("getPreview") ||
                methodName.equals("getProperties") ||
                methodName.equals("getAspects") ||
                methodName.equals("hasAspect") ||
                methodName.equals("getOwner")) {
            argumentId=2;
        }
        if (methodName.equals("getChild")) {
            argumentId=1;
        }
        if(methodName.equals("getChildrenChildAssociationRefAssoc") ||
                methodName.equals("getChildrenChildAssociationRefType") ||
                methodName.equals("getType") ||
                methodName.equals("getVersionHistory")){
            argumentId=0;
        }
        if(argumentId==-1)
            return invocation.proceed();
        String nodeId = (String) invocation.getArguments()[argumentId];
        return handleInvocation(nodeId, invocation,true);
    }

    /**
     * When onlyOnError is set, make sure that the called method will not cause any side effects since it may be called twice
     * @param nodeId
     * @param invocation
     * @param onlyOnError
     * @return
     * @throws Throwable
     */
    public static Object handleInvocation(String nodeId, MethodInvocation invocation, boolean onlyOnError) throws Throwable {
        if(onlyOnError){
            try{
                return invocation.proceed();
            }
            catch(AccessDeniedException|InsufficientPermissionException|AuthenticationCredentialsNotFoundException t)
            {
                // catch exception, check
                logger.info("Method threw "+t.getMessage()+" for node "+nodeId+", will check signature");
                return runAsSystem(nodeId,invocation);
            }
        }
        else{
            return runAsSystem(nodeId,invocation);
        }


    }
    private static Object runAsSystem(String nodeId,MethodInvocation invocation) throws Throwable {
        ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
        ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
        NodeService nodeService = serviceRegistry.getNodeService();
        int i = 0;
        while(nodeId!=null) {
            if (hasSignature(nodeId) || hasUsage(nodeId) || accessibleViaStream(nodeId)) {
                logger.debug("Node "+nodeId+" -> will run as system");
                return AuthenticationUtil.runAsSystem(() -> {
                    try {
                        return invocation.proceed();
                    } catch (Throwable throwable) {
                        throw new RuntimeException(throwable);
                    }
                });
            }

            // we'll check if any of the nodes in the parent hierarchy may has an usage -> so it is allowed as well
            final String nodeIdFinal=nodeId;
            nodeId=AuthenticationUtil.runAsSystem(()->{
                try {
                    return nodeService.getPrimaryParent(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeIdFinal)).getParentRef().getId();
                }catch(Throwable t2){
                    return null;
                }
            });
            // only one parent at the moment
            if(i++>=1) {
                break;
            }
        }
        return invocation.proceed();
    }

    private static boolean accessibleViaStream(String nodeId) {
        try {
            return StreamServiceHelper.canCurrentAuthorityAccessNode(StreamServiceFactory.getStreamService(), nodeId);
        }catch(Throwable t){
            logger.warn(t.getMessage());
        }
        return false;
    }

    private static boolean hasSignature(String nodeId) {
        if(Context.getCurrentInstance()==null)
            return false;
        String authSingleUseNodeId = Context.getCurrentInstance().getSessionAttribute(CCConstants.AUTH_SINGLE_USE_NODEID);
        if(authSingleUseNodeId==null)
            return false;
        return authSingleUseNodeId.equals(nodeId);
    }

    private static boolean hasUsage(String nodeId) {
        /*List<ChildAssociationRef> usages = nodeService.getChildAssocs(nodeRef, Collections.singleton(QName.createQName(CCConstants.CCM_ASSOC_USAGEASPECT_USAGES)));
        for(ChildAssociationRef usage : usages){
            Map<QName, Serializable> props = nodeService.getProperties(usage.getChildRef());
        }
        return false;*/
        return false;
    }
}
