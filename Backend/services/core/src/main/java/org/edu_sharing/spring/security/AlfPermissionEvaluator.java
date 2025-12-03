package org.edu_sharing.spring.security;

import lombok.RequiredArgsConstructor;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.edu_sharing.service.permission.PermissionService;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Component
@RequiredArgsConstructor
public class AlfPermissionEvaluator implements PermissionEvaluator {
    private final PermissionService permissionService;

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if ((authentication == null)) {
            return false;
        }

        if(targetDomainObject == null) {
            return true;
        }

        NodeRef nodeRef;
        if (targetDomainObject instanceof String nodeId) {
            nodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId);
        } else if (targetDomainObject instanceof NodeRef nodeRef1) {
            nodeRef = nodeRef1;
        } else {
            throw new IllegalArgumentException("Unsupported targetDomainObject type: " + targetDomainObject.getClass());
        }

        if (permission instanceof String permissionString) {
            return permissionService.hasPermission(nodeRef.getStoreRef().getProtocol(), nodeRef.getStoreRef().getIdentifier(), nodeRef.getId(), permissionString);
        } else if (permission instanceof String[] permissionStrings) {
            return permissionService.hasAllPermissions(nodeRef.getStoreRef().getProtocol(), nodeRef.getStoreRef().getIdentifier(), nodeRef.getId(), permissionStrings).values().stream().anyMatch(v -> !v);
        } else if (permission instanceof Iterable<?> iterablePermissions) {
            String[] permissionStrings = StreamSupport.stream(iterablePermissions.spliterator(), false).map(Object::toString).toArray(String[]::new);
            return permissionService.hasAllPermissions(nodeRef.getStoreRef().getProtocol(), nodeRef.getStoreRef().getIdentifier(), nodeRef.getId(), permissionStrings).values().stream().anyMatch(v -> !v);
        }
        return false;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
