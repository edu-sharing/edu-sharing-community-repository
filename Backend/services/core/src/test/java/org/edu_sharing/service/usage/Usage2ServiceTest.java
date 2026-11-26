package org.edu_sharing.service.usage;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.restservices.NodeDao;
import org.edu_sharing.restservices.RepositoryDao;
import org.edu_sharing.service.collection.CollectionService;
import org.edu_sharing.service.collection.CollectionServiceFactory;
import org.edu_sharing.service.permission.PermissionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The Read check of {@link Usage2Service#getUsageByParentNodeId}: which user it checks.
 */
class Usage2ServiceTest {
    private static final String NODE_ID = "node-1";

    private MockedStatic<AlfAppContextGate> alfAppContextGate;
    private MockedStatic<AuthenticationUtil> authenticationUtil;
    private MockedStatic<RepositoryDao> repositoryDao;
    private MockedStatic<NodeDao> nodeDaoStatic;
    private MockedStatic<CollectionServiceFactory> collectionServiceFactory;
    private MockedConstruction<AlfServicesWrapper> alfServicesWrapper;
    private NodeDao node;

    @BeforeEach
    void setUp() throws Exception {
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        when(applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY)).thenReturn(mock(ServiceRegistry.class));
        alfAppContextGate = Mockito.mockStatic(AlfAppContextGate.class);
        alfAppContextGate.when(AlfAppContextGate::getApplicationContext).thenReturn(applicationContext);
        alfServicesWrapper = Mockito.mockConstruction(AlfServicesWrapper.class,
                (wrapper, context) -> when(wrapper.getUsages(anyString())).thenReturn(Map.of()));

        authenticationUtil = Mockito.mockStatic(AuthenticationUtil.class);
        authenticationUtil.when(AuthenticationUtil::getSystemUserName).thenReturn("System");
        authenticationUtil.when(() -> AuthenticationUtil.runAsSystem(any()))
                .thenAnswer(invocation -> ((AuthenticationUtil.RunAsWork<?>) invocation.getArgument(0)).doWork());

        RepositoryDao home = mock(RepositoryDao.class);
        repositoryDao = Mockito.mockStatic(RepositoryDao.class);
        repositoryDao.when(RepositoryDao::getHomeRepository).thenReturn(home);
        node = mock(NodeDao.class);
        nodeDaoStatic = Mockito.mockStatic(NodeDao.class);
        nodeDaoStatic.when(() -> NodeDao.getNode(home, NODE_ID)).thenReturn(node);
        // As PermissionServiceImpl: null is the fully authenticated user, who has no Read here,
        // like the missing user of an app-signed request; System has every permission.
        when(node.getPermissions(isNull())).thenReturn(List.of());
        when(node.getPermissions("System")).thenReturn(List.of(CCConstants.PERMISSION_READ));

        CollectionService collectionService = mock(CollectionService.class);
        when(collectionService.getReferenceObjects(anyString())).thenReturn(List.of());
        collectionServiceFactory = Mockito.mockStatic(CollectionServiceFactory.class);
        collectionServiceFactory.when(CollectionServiceFactory::getLocalService).thenReturn(collectionService);
    }

    @AfterEach
    void tearDown() {
        alfAppContextGate.close();
        alfServicesWrapper.close();
        authenticationUtil.close();
        repositoryDao.close();
        nodeDaoStatic.close();
        collectionServiceFactory.close();
    }

    /** UsageDao#deleteUsage and the jobs run it as System, without a user of the request. */
    @Test
    void runningAsSystemChecksSystem() throws Exception {
        authenticationUtil.when(AuthenticationUtil::isRunAsUserTheSystemUser).thenReturn(true);

        assertEquals(List.of(), new Usage2Service().getUsageByParentNodeId(null, null, NODE_ID));

        verify(node).getPermissions("System");
    }

    /** A user's request without Read on the node is refused (403), as before. */
    @Test
    void requestWithoutReadIsRefused() throws Exception {
        authenticationUtil.when(AuthenticationUtil::isRunAsUserTheSystemUser).thenReturn(false);

        UsageException exception = assertThrows(UsageException.class,
                () -> new Usage2Service().getUsageByParentNodeId(null, null, NODE_ID));

        assertInstanceOf(PermissionException.class, exception.getCause());
        verify(node).getPermissions(isNull());
    }

    /** A user given explicitly is checked as given, also when running as System. */
    @Test
    void givenUserIsChecked() throws Exception {
        authenticationUtil.when(AuthenticationUtil::isRunAsUserTheSystemUser).thenReturn(true);
        when(node.getPermissions(eq("teacher"))).thenReturn(List.of());

        assertThrows(UsageException.class, () -> new Usage2Service().getUsageByParentNodeId(null, "teacher", NODE_ID));

        verify(node).getPermissions("teacher");
    }
}
