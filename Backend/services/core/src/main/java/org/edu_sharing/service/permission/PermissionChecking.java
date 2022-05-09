package org.edu_sharing.service.permission;


import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.rest.framework.core.exceptions.InvalidArgumentException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.edu_sharing.alfresco.policy.GuestCagePolicy;
import org.edu_sharing.service.InsufficientPermissionException;
import org.edu_sharing.service.authority.AuthorityService;
import org.edu_sharing.service.permission.annotation.NodePermission;
import org.edu_sharing.service.permission.annotation.Permission;
import org.edu_sharing.service.toolpermission.ToolPermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;

@Aspect
@Component
public class PermissionChecking {

    AuthorityService authorityService;
    PermissionService permissionService;
    ToolPermissionService toolPermissionService;

    @Autowired
    public void setAuthorityService(AuthorityService authorityService) {
        this.authorityService = authorityService;
    }

    @Autowired
    public void setPermissionService(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Autowired
    public void setToolPermissionService(ToolPermissionService toolPermissionService) {
        this.toolPermissionService = toolPermissionService;
    }

    @Before("@annotation(org.edu_sharing.service.permission.annotation.Permission)")
    public void checkPermission(JoinPoint joinPoint) throws InsufficientPermissionException, NoSuchMethodException {
        String user = AuthenticationUtil.getFullyAuthenticatedUser();


        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Class<?> targetClass = joinPoint.getTarget().getClass();
        Method method = targetClass.getMethod(signature.getName(), signature.getParameterTypes());

        Permission permissionAnnotation = AnnotationUtils.findAnnotation(method, Permission.class);

        if (permissionAnnotation.requiresUser() && authorityService.isGuest()) {
            throw new GuestCagePolicy.GuestPermissionDeniedException(String.format("guests can not use %s", method.getName()));
        }

        for (String requiredPermission : permissionAnnotation.value()) {
            if (!toolPermissionService.hasToolPermission(requiredPermission)) {
                throw new InsufficientPermissionException(String.format("Tool permission(s): %s required", String.join(", ", permissionAnnotation.value())));
            }
        }

        checkNodePermissions(joinPoint, user, method);
    }

    private void checkNodePermissions(JoinPoint joinPoint, String user, Method method) throws InsufficientPermissionException {
        Object[] args = joinPoint.getArgs();
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Object arg = args[i];

            NodePermission nodePermissionAnnotation = parameter.getAnnotation(NodePermission.class);
            if (nodePermissionAnnotation == null) {
                continue;
            }

            if (arg.getClass().isArray()) {
                int j = 0;
                for (Object item : (Object[]) arg) {
                    checkNodePermissions(item, user, nodePermissionAnnotation.value(), String.format("%s[%s] ", parameter.getName(), j));
                    j++;
                }
            } else if (arg instanceof Iterable) {
                int j = 0;
                for (Object item : (Iterable<?>) arg) {
                    checkNodePermissions(item, user, nodePermissionAnnotation.value(), String.format("%s[%s] ", parameter.getName(), j));
                    j++;
                }
            } else {
                checkNodePermissions(arg, user, nodePermissionAnnotation.value(), parameter.getName());
            }
        }
    }

    private void checkNodePermissions(Object node, String user, String[] permissions, String parameterName) throws InsufficientPermissionException {
        String nodeId;
        if (node instanceof String) {
            nodeId = (String) node;
        } else if (node instanceof NodeRef) {
            nodeId = ((NodeRef) node).getId();
        } else {
            throw new InvalidArgumentException(String.format("%s must be of type %s or %s", parameterName, String.class, NodeRef.class));
        }

        List<String> nodePermissions = permissionService.getPermissionsForAuthority(nodeId, user);
        if (!nodePermissions.containsAll(Arrays.asList(permissions))) {
            throw new InsufficientPermissionException(String.format("%s with id %s requires permission(s): %s",
                    parameterName, nodeId, String.join(", ", permissions)));
        }
    }
}
