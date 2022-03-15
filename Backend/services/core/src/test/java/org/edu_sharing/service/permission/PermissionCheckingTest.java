package org.edu_sharing.service.permission;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.edu_sharing.alfresco.policy.GuestCagePolicy;
import org.edu_sharing.service.InsufficientPermissionException;
import org.edu_sharing.service.authority.AuthorityService;
import org.edu_sharing.service.permission.annotation.NodePermission;
import org.edu_sharing.service.permission.annotation.Permission;
import org.edu_sharing.service.toolpermission.ToolPermissionService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


@ExtendWith(MockitoExtension.class)
class PermissionCheckingTest {
    @Mock
    AuthorityService authorityService;

    @Mock
    PermissionService permissionService;

    @Mock
    ToolPermissionService toolPermissionService;

    @Test
    void SuccessPermissionTest() throws InsufficientPermissionException {
        // given
        String authority = "Muster";
        String nodeA = "1";
        String nodeB = "2";
        List<String> nodePermissions = Arrays.asList("someNodePermission", "someMoreNodePermission", "otherNodePermission");
        String toolPermission = "someToolPermission";

        Mockito.when(authorityService.isGuest()).thenReturn(false);
        Mockito.when(toolPermissionService.hasToolPermission(toolPermission)).thenReturn(true);
        Mockito.when(permissionService.getPermissionsForAuthority(nodeA, authority)).thenReturn(nodePermissions);
        Mockito.when(permissionService.getPermissionsForAuthority(nodeB, authority)).thenReturn(nodePermissions);


        MyClass target = new MyClass();
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        PermissionChecking aspect = new PermissionChecking();
        aspect.setPermissionService(permissionService);
        aspect.setAuthorityService(authorityService);
        aspect.setToolPermissionService(toolPermissionService);
        factory.addAspect(aspect);

        MyClass proxy = factory.getProxy();
        try (MockedStatic<AuthenticationUtil> authenticationUtilMockedStatic = Mockito.mockStatic(AuthenticationUtil.class)) {
            authenticationUtilMockedStatic.when(AuthenticationUtil::getFullyAuthenticatedUser).thenReturn(authority);
            // when then
            proxy.SomeMethod(nodeA, nodeB);
        }
    }

    @Test
    void failedIsGuestTest() {
        // given
        String authority = "Muster";
        String nodeA = "1";
        String nodeB = "2";

        Mockito.when(authorityService.isGuest()).thenReturn(true);

        MyClass target = new MyClass();
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        PermissionChecking aspect = new PermissionChecking();
        aspect.setPermissionService(permissionService);
        aspect.setAuthorityService(authorityService);
        aspect.setToolPermissionService(toolPermissionService);
        factory.addAspect(aspect);

        MyClass proxy = factory.getProxy();


        try (MockedStatic<AuthenticationUtil> authenticationUtilMockedStatic = Mockito.mockStatic(AuthenticationUtil.class)) {
            authenticationUtilMockedStatic.when(AuthenticationUtil::getFullyAuthenticatedUser).thenReturn(authority);
            // when then
            Assertions.assertThrows(GuestCagePolicy.GuestPermissionDeniedException.class , () -> proxy.SomeMethod(nodeA, nodeB));
        }
    }

    @Test
    void failedToolPermissionTest() {
        // given
        String authority = "Muster";
        String nodeA = "1";
        String nodeB = "2";
        String toolPermission = "someToolPermission";

        Mockito.when(authorityService.isGuest()).thenReturn(false);
        Mockito.when(toolPermissionService.hasToolPermission(toolPermission)).thenReturn(false);


        MyClass target = new MyClass();
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        PermissionChecking aspect = new PermissionChecking();
        aspect.setPermissionService(permissionService);
        aspect.setAuthorityService(authorityService);
        aspect.setToolPermissionService(toolPermissionService);
        factory.addAspect(aspect);

        MyClass proxy = factory.getProxy();


        try (MockedStatic<AuthenticationUtil> authenticationUtilMockedStatic = Mockito.mockStatic(AuthenticationUtil.class)) {
            authenticationUtilMockedStatic.when(AuthenticationUtil::getFullyAuthenticatedUser).thenReturn(authority);
            // when then
            Assertions.assertThrows(InsufficientPermissionException.class , () -> proxy.SomeMethod(nodeA, nodeB));
        }
    }

    @Test
    void failedNodePermission_NodeATest() throws InsufficientPermissionException {
        // given
        String authority = "Muster";
        String nodeA = "1";
        String nodeB = "2";
        List<String> nodePermissionsNodeA = Collections.singletonList("otherNodePermission");
        String toolPermission = "someToolPermission";

        Mockito.when(authorityService.isGuest()).thenReturn(false);
        Mockito.when(toolPermissionService.hasToolPermission(toolPermission)).thenReturn(true);
        Mockito.when(permissionService.getPermissionsForAuthority(nodeA, authority)).thenReturn(nodePermissionsNodeA);


        MyClass target = new MyClass();
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        PermissionChecking aspect = new PermissionChecking();
        aspect.setPermissionService(permissionService);
        aspect.setAuthorityService(authorityService);
        aspect.setToolPermissionService(toolPermissionService);
        factory.addAspect(aspect);

        MyClass proxy = factory.getProxy();

        try (MockedStatic<AuthenticationUtil> authenticationUtilMockedStatic = Mockito.mockStatic(AuthenticationUtil.class)) {
            authenticationUtilMockedStatic.when(AuthenticationUtil::getFullyAuthenticatedUser).thenReturn(authority);
            // when then
            Assertions.assertThrows(InsufficientPermissionException.class , () -> proxy.SomeMethod(nodeA, nodeB));
        }
    }

    @Test
    void failedNodePermission_NodeBTest() throws InsufficientPermissionException {
        // given
        String authority = "Muster";
        String nodeA = "1";
        String nodeB = "2";
        List<String> nodePermissionsNodeA = Arrays.asList("someNodePermission", "someMoreNodePermission", "otherNodePermission");
        List<String> nodePermissionsNodeB = new ArrayList<>();
        String toolPermission = "someToolPermission";

        Mockito.when(authorityService.isGuest()).thenReturn(false);
        Mockito.when(toolPermissionService.hasToolPermission(toolPermission)).thenReturn(true);
        Mockito.when(permissionService.getPermissionsForAuthority(nodeA, authority)).thenReturn(nodePermissionsNodeA);
        Mockito.when(permissionService.getPermissionsForAuthority(nodeB, authority)).thenReturn(nodePermissionsNodeB);


        MyClass target = new MyClass();
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        PermissionChecking aspect = new PermissionChecking();
        aspect.setPermissionService(permissionService);
        aspect.setAuthorityService(authorityService);
        aspect.setToolPermissionService(toolPermissionService);
        factory.addAspect(aspect);

        MyClass proxy = factory.getProxy();

        try (MockedStatic<AuthenticationUtil> authenticationUtilMockedStatic = Mockito.mockStatic(AuthenticationUtil.class)) {
            authenticationUtilMockedStatic.when(AuthenticationUtil::getFullyAuthenticatedUser).thenReturn(authority);
            // when then
            Assertions.assertThrows(InsufficientPermissionException.class, () -> proxy.SomeMethod(nodeA, nodeB));
        }
    }
}

class MyClass {
    @Permission(requiresUser = true, value = {"someToolPermission"})
    public void SomeMethod(
            @NodePermission({"someNodePermission","otherNodePermission"}) String nodeA,
            @NodePermission({"someMoreNodePermission"}) String nodeB)
            throws InsufficientPermissionException {
        System.out.println("MyPermissionTest");
    }
}