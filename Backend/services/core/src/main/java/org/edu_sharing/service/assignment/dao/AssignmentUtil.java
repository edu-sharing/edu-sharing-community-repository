package org.edu_sharing.service.assignment.dao;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.StoreRef;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.restservices.assignment.v1.model.Assignment;
import org.edu_sharing.service.authority.AuthorityServiceHelper;
import org.edu_sharing.service.model.NodeRef;
import org.edu_sharing.service.permission.PermissionService;

import java.util.Objects;

public final class AssignmentUtil {

    static boolean isAssignmentCoordinator(PermissionService permissionService, String nodeId) {
        if (Objects.isNull(nodeId)) {
            return false;
        }

        if (AuthorityServiceHelper.isAdmin(AuthenticationUtil.getRunAsUser())) {
            return true;
        }

        return permissionService.hasPermission(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), nodeId, CCConstants.PERMISSION_ASSIGNMENT_COORDINATOR);
    }

    static boolean isAssignmentCoordinator(NodeRef nodeRef) {
        if (Objects.isNull(nodeRef)) {
            return false;
        }

        if (AuthorityServiceHelper.isAdmin(AuthenticationUtil.getRunAsUser())) {
            return true;
        }

        return nodeRef.getPermissions().getOrDefault(CCConstants.PERMISSION_ASSIGNMENT_COORDINATOR, false);
    }

    static boolean isAssignee(PermissionService permissionService, String nodeId) {
        if (Objects.isNull(nodeId)) {
            return false;
        }

        if (AuthorityServiceHelper.isAdmin(AuthenticationUtil.getRunAsUser())) {
            return true;
        }

        return permissionService.hasPermission(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), nodeId, CCConstants.PERMISSION_ASSIGNEE);
    }

    static boolean isAssignee(NodeRef nodeRef) {
        if (Objects.isNull(nodeRef)) {
            return false;
        }

        if (AuthorityServiceHelper.isAdmin(AuthenticationUtil.getRunAsUser())) {
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

    /**
     * Checks whether the current user is the creator ({@code cm:creator}) of the node, or an admin.
     * Note that this is deliberately based on {@code cm:creator} and not Alfresco's {@code cm:owner}/
     * {@link org.alfresco.service.cmr.security.OwnableService}, since assignment nodes always have
     * their owner set to the home repository's system user (see {@code NodeAssignmentDao#createOrUpdate}).
     */
    static boolean isCreator(String creator) {
        String currentUser = AuthenticationUtil.getRunAsUser();
        return Objects.equals(creator, currentUser) || AuthorityServiceHelper.isAdmin(currentUser);
    }

    static boolean canDeletePermanently(Assignment.Status status) {
        return status == Assignment.Status.FINISHED || status == Assignment.Status.CANCELED;
    }
}