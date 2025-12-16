package org.edu_sharing.service.assignment.dao;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.StoreRef;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.authority.AuthorityServiceHelper;
import org.edu_sharing.service.model.NodeRef;
import org.edu_sharing.service.permission.PermissionService;

public final class AssignmentUtil {

    static boolean isAssignmentCoordinator(PermissionService permissionService, String nodeId) {
        if (AuthorityServiceHelper.isAdmin(AuthenticationUtil.getFullyAuthenticatedUser())) {
            return true;
        }

        return permissionService.hasPermission(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), nodeId, CCConstants.PERMISSION_ASSIGNMENT_COORDINATOR);
    }

    static boolean isAssignmentCoordinator(NodeRef nodeRef) {
        if (AuthorityServiceHelper.isAdmin(AuthenticationUtil.getFullyAuthenticatedUser())) {
            return true;
        }

        return nodeRef.getPermissions().getOrDefault(CCConstants.PERMISSION_ASSIGNMENT_COORDINATOR, false);
    }

    static boolean isAssignee(PermissionService permissionService, String nodeId) {
        if (AuthorityServiceHelper.isAdmin(AuthenticationUtil.getFullyAuthenticatedUser())) {
            return true;
        }

        return permissionService.hasPermission(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), nodeId, CCConstants.PERMISSION_ASSIGNEE);
    }

    static boolean isAssignee(NodeRef nodeRef) {
        if (AuthorityServiceHelper.isAdmin(AuthenticationUtil.getFullyAuthenticatedUser())) {
            return true;
        }

        return nodeRef.getPermissions().getOrDefault(CCConstants.PERMISSION_ASSIGNEE, false);
    }

    static boolean hasAccessTo(PermissionService permissionService, String nodeId) {
      return isAssignmentCoordinator(permissionService, nodeId) || isAssignee(permissionService, nodeId);
    }

    static boolean hasAccessTo(NodeRef nodeRef) {
        return isAssignmentCoordinator(nodeRef) || isAssignee(nodeRef);
    }
}