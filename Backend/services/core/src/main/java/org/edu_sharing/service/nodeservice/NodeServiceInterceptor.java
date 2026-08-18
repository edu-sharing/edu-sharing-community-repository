package org.edu_sharing.service.nodeservice;

import net.sf.acegisecurity.AuthenticationCredentialsNotFoundException;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.usage.ContentQuotaException;
import org.alfresco.service.namespace.QName;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.alfresco.service.toolpermission.ToolPermissionException;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.appcontext.AppContextServiceLocator;
import org.edu_sharing.repository.server.authentication.ContextManagementFilter;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.service.InsufficientPermissionException;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.SearchServiceElastic;
import org.edu_sharing.service.stream.StreamServiceFactory;
import org.edu_sharing.service.stream.StreamServiceHelper;
import org.edu_sharing.service.toolpermission.ToolPermissionHelper;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

public class NodeServiceInterceptor implements MethodInterceptor {
    static ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
    static ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);

    static Logger logger = Logger.getLogger(NodeServiceInterceptor.class);

    List<NodeServiceInterceptorPermissions> customizations;


    public void init() {

    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {

        String methodName = invocation.getMethod().getName();

        int argumentId = -1;
        if (methodName.equals("getProperty") ||
                methodName.equals("getContent") ||
                methodName.equals("getContentHash") ||
                methodName.equals("getContentMimetype") ||
                methodName.equals("getPreview") ||
                methodName.equals("getProperties") ||
                methodName.equals("getVersion") ||
                methodName.equals("getAspects") ||
                methodName.equals("hasAspect") ||
                methodName.equals("getOwner")) {
            argumentId = 2;
        }
        if (methodName.equals("getChild")) {
            argumentId = 1;
        }
        if (methodName.equals("getChildrenChildAssociationRefAssoc") ||
                methodName.equals("getChildrenChildAssociationRefType") ||
                methodName.equals("getType") ||
                methodName.equals("getPublishedCopies") ||
                methodName.equals("getOriginalNode") ||
                methodName.equals("getPrimaryParent") ||
                methodName.equals("getVersionHistory") ||
                methodName.equals("copyNode")) {
            argumentId = 0;
        }

        if (methodName.equals("writeContent")) {
            return checkIgnoreQuota(invocation);
        }


        if (argumentId == -1)
            return invocation.proceed();



        String nodeId = (String) invocation.getArguments()[argumentId];
        if (Arrays.asList("getProperty", "getProperties").contains(methodName)) {
            checkReadMetadataPermissions(nodeId, invocation);
        }


        return handleInvocation(nodeId, invocation, true);
    }

    private void checkReadMetadataPermissions(String nodeId, MethodInvocation invocation) {
        try {
            NodeRef nodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId);
            if (serviceRegistry.getNodeService().hasAspect(nodeRef, QName.createQName(
                    CCConstants.CCM_ASPECT_REMOTEREPOSITORY
            ))) {
                String remoteId = (String) serviceRegistry.getNodeService().getProperty(nodeRef, QName.createQName(
                        CCConstants.CCM_PROP_REMOTEOBJECT_REPOSITORYID
                ));
                if (remoteId != null) {
                    String tpId = CCConstants.CCM_VALUE_TOOLPERMISSION_REPOSITORY_PREFIX + remoteId;
                    ToolPermissionHelper.throwIfToolpermissionMissing(tpId);
                }
            }
        } catch (ToolPermissionException e) {
            throw e;
        } catch (InvalidNodeRefException t) {
            logger.debug("InvalidNodeRefException while verifying if object is remote object: " + t.getMessage());
        } catch (Throwable t) {
            logger.info("Unexpected error while verifying if object is remote object (" + nodeId + "): " + t.getMessage());
        }
    }

    /**
     * When onlyOnError is set, make sure that the called method will not cause any side effects since it may be called twice
     *
     * @param nodeId
     * @param invocation
     * @param onlyOnError
     * @return
     * @throws Throwable
     */
    public static Object handleInvocation(String nodeId, MethodInvocation invocation, boolean onlyOnError) throws Throwable {
        if (onlyOnError) {
            try {
                return invocation.proceed();
            } catch (AccessDeniedException | InsufficientPermissionException |
                     AuthenticationCredentialsNotFoundException t) {
                // catch exception, check
                logger.debug("Method threw " + t.getMessage() + " for node " + nodeId + ", will check signature");
                return runAsSystem(nodeId, invocation);
            }
        } else {
            return runAsSystem(nodeId, invocation);
        }


    }

    /**
     * Runs the invocation regularly first and only falls back to temporarily disabling the user's quota
     * (which is allowed if the request was initiated by a connector) if the quota really was the reason for
     * the call to fail.
     */
    private static Object checkIgnoreQuota(MethodInvocation invocation) throws Throwable {
        if (ContextManagementFilter.accessTool.get() == null ||
                !ApplicationInfo.TYPE_CONNECTOR.equals(ContextManagementFilter.accessTool.get().getApplicationInfo().getType())
        ) {
            return invocation.proceed();
        }
        try {
            return invocation.proceed();
        } catch (ContentQuotaException quotaException) {
            if (!rewindStreamArguments(invocation)) {
                logger.warn("Quota of the current user blocked " + invocation.getMethod().getName()
                        + " and the call can not be repeated because its data was already consumed."
                        + " Pass a stream that supports mark/reset to make it repeatable.");
                throw quotaException;
            }
            logger.info("Quota of the current user blocked " + invocation.getMethod().getName()
                    + ", retrying with the quota temporarily disabled: " + quotaException.getMessage());
            return ignoreQuota(() -> {
                try {
                    return invocation.proceed();
                } catch (Exception e) {
                    throw e;
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    /**
     * Rewinds all stream arguments of the invocation so that it can be run a second time
     *
     * @return false if any of them can not be read again
     */
    private static boolean rewindStreamArguments(MethodInvocation invocation) {
        for (Object argument : invocation.getArguments()) {
            if (!(argument instanceof InputStream stream)) {
                continue;
            }
            if (!stream.markSupported()) {
                return false;
            }
            try {
                stream.reset();
            } catch (IOException e) {
                logger.warn("Could not reset " + stream.getClass().getName() + ": " + e.getMessage());
                return false;
            }
        }
        return true;
    }

    /**
     * Ignores any user quotas for the given callback context
     * Basically, this temporarily disables the quota for the user
     */
    public static <T> T ignoreQuota(Callable<T> callable) {
        ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
        ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
        RetryingTransactionHelper rth = serviceRegistry.getTransactionService().getRetryingTransactionHelper();

        return rth.doInTransaction(() -> {
            Serializable quota = (Serializable) AuthorityServiceFactory.getInstance().getLocalService().getAuthorityProperty(AuthenticationUtil.getFullyAuthenticatedUser(), CCConstants.CM_PROP_PERSON_SIZE_QUOTA);
            AuthenticationUtil.runAsSystem(() -> {
                AuthorityServiceFactory.getInstance().getLocalService().setAuthorityProperty(AuthenticationUtil.getFullyAuthenticatedUser(), CCConstants.CM_PROP_PERSON_SIZE_QUOTA, null);
                return null;
            });

            T result = callable.call();

            AuthenticationUtil.runAsSystem(() -> {
                AuthorityServiceFactory.getInstance().getLocalService().setAuthorityProperty(AuthenticationUtil.getFullyAuthenticatedUser(), CCConstants.CM_PROP_PERSON_SIZE_QUOTA, quota);
                return null;
            });
            return result;
        });
    }

    /**
     * returns list of permissions the current user has access on the given node via usages, collections or other indirect permissions
     */
    public static List<String> getIndirectPermissions(String nodeId, List<String> permissions) {
        int i = 0;
        while (nodeId != null) {
            List<String> result = getIndirectPermissions(nodeId, permissions, i);
            if (!result.isEmpty()) {
                return result;
            }

            // only one parent at the moment
            if (i++ >= 1) {
                break;
            }
            nodeId = fetchParentId(nodeId);
        }
        return Collections.emptyList();
    }

    private static Object runAsSystem(String nodeId, MethodInvocation invocation) throws Throwable {
        int i = 0;
        while (nodeId != null) {
            if (
                    getIndirectPermissions(nodeId, Collections.singletonList(CCConstants.PERMISSION_READ), i).size() == 1
            ) {
                logger.debug("Node " + nodeId + " -> will run as system");
                return AuthenticationUtil.runAsSystem(() -> {
                    try {
                        return invocation.proceed();
                    } catch (Throwable throwable) {
                        throw new RuntimeException(throwable);
                    }
                });
            }

            // only one parent at the moment
            if (i++ >= 1) {
                break;
            }
            // we'll check if any of the nodes in the parent hierarchy may has an usage -> so it is allowed as well
            nodeId = fetchParentId(nodeId);
        }
        return invocation.proceed();
    }

    private static String fetchParentId(String nodeIdFinal) {
        return AuthenticationUtil.runAsSystem(() -> {
            try {
                return serviceRegistry.getNodeService().getPrimaryParent(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeIdFinal)).getParentRef().getId();
            } catch (Throwable t2) {
                return null;
            }
        });
    }

    private static List<String> getIndirectPermissions(String nodeId, List<String> permissions, int recursionDepth) {
        if ((hasSignature(nodeId) || hasUsage(nodeId)) ||
                // direct permissions only valid for current node, NOT for parent!
                (accessibleViaStream(nodeId) || accessableViaCustomization(nodeId, recursionDepth))
        ) {
            return permissions.stream().filter(p -> CCConstants.getUsagePermissions().contains(p)).collect(Collectors.toList());
        }
        if (recursionDepth == 0) {
            return hasCollectionPermissions(nodeId, permissions);
        }
        return Collections.emptyList();
    }

    private static boolean accessableViaCustomization(String nodeId, int recursionDepth) {
        for (NodeServiceInterceptorPermissions customization : PropertiesInterceptorFactory.getNodeServiceInterceptorPermissions()) {
            if (customization.accessable(nodeId, recursionDepth)) return true;
        }
        return false;
    }

    private static boolean accessibleViaStream(String nodeId) {
        try {
            return StreamServiceHelper.canCurrentAuthorityAccessNode(StreamServiceFactory.getStreamService(), nodeId);
        } catch (Throwable t) {
            logger.warn(t.getMessage());
        }
        return false;
    }

    private static boolean hasSignature(String nodeId) {
        if (Context.getCurrentInstance() == null)
            return false;
        return Context.getCurrentInstance().isSingleUseNodeId(nodeId);
    }

    private static boolean hasUsage(String nodeId) {
        /*List<ChildAssociationRef> usages = nodeService.getChildAssocs(nodeRef, Collections.singleton(QName.createQName(CCConstants.CCM_ASSOC_USAGEASPECT_USAGES)));
        for(ChildAssociationRef usage : usages){
            Map<QName, Serializable> props = nodeService.getProperties(usage.getChildRef());
        }
        return false;*/
        return false;
    }

    public static List<String> hasCollectionPermissions(String nodeId, List<String> permissionsToValidate) {
        long test = System.currentTimeMillis();
        AppContextServiceLocator locator = AppContextServiceLocator.getInstance();
        SearchService searchService = locator.get(SearchService.class);
        if(!(searchService instanceof SearchServiceElastic searchServiceElastic)){
            logger.debug("Skipping collection permission check for SearchServiceElastic");
            return Collections.emptyList();
        }

        if (
                !Arrays.asList(
                        CallSourceHelper.CallSource.Render, CallSourceHelper.CallSource.Preview,
                        CallSourceHelper.CallSource.Sitemap, CallSourceHelper.CallSource.ToolConnector,
                        CallSourceHelper.CallSource.RatingApi, CallSourceHelper.CallSource.Oai, CallSourceHelper.CallSource.Download
                ).contains(CallSourceHelper.getCallSource())
        ) {
            logger.debug("Skipping collection permission check for call source " + CallSourceHelper.getCallSource());
            return Collections.emptyList();
        }
        List<String> result = searchServiceElastic.hasPermissions(nodeId, permissionsToValidate);
        logger.debug("collection permission check took:" + (System.currentTimeMillis() - test) + "ms");
        result = handleCollectionPermissionsFromInterceptors(nodeId, permissionsToValidate, result);
        return result;
    }

    public static List<String> handleCollectionPermissionsFromInterceptors(String nodeId, List<String> permissionsToValidate, List<String> resultingPermissions) {
        List<? extends NodeServiceInterceptorPermissions> permInterceptor = PropertiesInterceptorFactory.getNodeServiceInterceptorPermissions();
        if(!permInterceptor.isEmpty()) {
            // not ideal: In some scenarios, we're in an external runAsSystem block. But the intercpetor might need to do
            // user specific permission checks so we need it to run as the regular user
            return AuthenticationUtil.runAs(() -> {
                List<String> p = resultingPermissions;
                for (NodeServiceInterceptorPermissions c : permInterceptor) {
                    p = c.hasCollectionPermissions(nodeId, permissionsToValidate, p);
                }
                return p;
            }, AuthenticationUtil.getFullyAuthenticatedUser());
        } else {
            return resultingPermissions;
        }
    }
}
