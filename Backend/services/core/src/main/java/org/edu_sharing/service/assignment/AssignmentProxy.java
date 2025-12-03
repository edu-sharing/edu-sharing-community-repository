package org.edu_sharing.service.assignment;

import lombok.RequiredArgsConstructor;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.alfresco.service.cmr.repository.StoreRef;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.authority.AuthorityServiceHelper;
import org.edu_sharing.service.permission.PermissionService;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;


/**
 * AssignmentProxy is an aspect responsible for handling access control checks
 * and permission validations on methods of the AssignmentDao classes. It ensures
 * that certain operations are performed under the context of a system-level user
 * and validates permissions for the current user before proceeding with specific actions.
 */
@Aspect
@Component
@RequiredArgsConstructor
public class AssignmentProxy {

    private final PermissionService permissionService;

//   @Order(Ordered.LOWEST_PRECEDENCE)
//    @Around(value = "execution(* org.edu_sharing.service.assignment.AssignmentDaoFactory.*.*(..))")
//    public Object around(ProceedingJoinPoint pjp) {
//        return AuthenticationUtil.runAsSystem(() -> {
//            try {
//                return pjp.proceed();
//            } catch (RuntimeException e) {
//                throw e;
//            } catch (Throwable e) {
//                throw new RuntimeException(e);
//            }
//        });
//    }

    @Order(Ordered.HIGHEST_PRECEDENCE + 1)
    @Before(value = "execution(* org.edu_sharing.service.assignment.AssignmentDaoFactory.*.*(..)) && target(basicNodeDao)")
    public void basicPermissionCheckAdvice(BasicNodeDao basicNodeDao) {
        if(basicNodeDao.getNodeId() == null) {
            return;
        }

        if (AuthorityServiceHelper.isAdmin()) {
            return;
        }

        if (!isCoordinatorOn(basicNodeDao.getNodeId()) && !isAssigneeOn(basicNodeDao.getNodeId()) ) {
            throw new AccessDeniedException("User is not allowed to perform this action");
        }
    }

    private boolean isAssigneeOn(String nodeId) {
        return permissionService.hasPermission(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), nodeId, CCConstants.PERMISSION_ASSIGNEE);
    }

    private boolean isCoordinatorOn(String nodeId) {
        return permissionService.hasPermission(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), nodeId, CCConstants.PERMISSION_ASSIGNMENT_COORDINATOR);
    }
}
