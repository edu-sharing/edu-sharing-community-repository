package org.edu_sharing.restservices.iam.v1;

import com.typesafe.config.Config;
import jakarta.ws.rs.core.Response;
import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.restservices.DAOException;
import org.edu_sharing.restservices.GroupDao;
import org.edu_sharing.restservices.PersonDao;
import org.edu_sharing.restservices.RepositoryDao;
import org.edu_sharing.restservices.shared.Group;
import org.edu_sharing.restservices.shared.UserSimple;
import org.edu_sharing.service.authority.AuthorityServiceHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
// the shared setUp stubs error-response collaborators that only the failing paths use
@MockitoSettings(strictness = Strictness.LENIENT)
class IamApiGetMemberTest {

    private static final String GROUP = "GROUP_test";

    @Mock
    private RepositoryDao repoDao;
    @Mock
    private GroupDao groupDao;

    private MockedStatic<RepositoryDao> repositoryDaoStatic;
    private MockedStatic<GroupDao> groupDaoStatic;
    private MockedStatic<AuthorityServiceHelper> authorityServiceHelperStatic;
    private MockedStatic<LightbendConfigLoader> configLoaderStatic;
    private final IamApi underTest = new IamApi();

    @BeforeEach
    void setUp() throws Throwable {
        repositoryDaoStatic = Mockito.mockStatic(RepositoryDao.class);
        repositoryDaoStatic.when(() -> RepositoryDao.getRepository("-home-")).thenReturn(repoDao);
        groupDaoStatic = Mockito.mockStatic(GroupDao.class);
        groupDaoStatic.when(() -> GroupDao.getGroup(repoDao, GROUP)).thenReturn(groupDao);
        // ErrorResponse asks whether the caller is admin; avoid the Spring context
        authorityServiceHelperStatic = Mockito.mockStatic(AuthorityServiceHelper.class);
        authorityServiceHelperStatic.when(AuthorityServiceHelper::isAdmin).thenReturn(false);
        Config config = Mockito.mock(Config.class);
        when(config.getString(Mockito.anyString())).thenReturn("minimal");
        configLoaderStatic = Mockito.mockStatic(LightbendConfigLoader.class);
        configLoaderStatic.when(LightbendConfigLoader::get).thenReturn(config);
    }

    @AfterEach
    void tearDown() {
        configLoaderStatic.close();
        authorityServiceHelperStatic.close();
        groupDaoStatic.close();
        repositoryDaoStatic.close();
    }

    @Test
    void getMember_user_returnsUser() throws Exception {
        // Arrange
        UserSimple expected = new UserSimple();
        when(groupDao.isDirectMember("user/with/slash")).thenReturn(true);

        try (MockedConstruction<PersonDao> ignored = Mockito.mockConstruction(PersonDao.class,
                (mock, ctx) -> when(mock.asPersonSimple(false)).thenReturn(expected))) {
            // Act
            Response response = underTest.getMember("-home-", GROUP, "user/with/slash", null);

            // Assert
            assertEquals(200, response.getStatus());
            assertSame(expected, response.getEntity());
        }
    }

    @Test
    void getMember_group_returnsGroup() throws Exception {
        // Arrange
        Group expected = new Group();
        when(groupDao.isDirectMember("GROUP_sub")).thenReturn(true);

        try (MockedConstruction<GroupDao> ignored = Mockito.mockConstruction(GroupDao.class,
                (mock, ctx) -> when(mock.asGroup(false)).thenReturn(expected))) {
            // Act
            Response response = underTest.getMember("-home-", GROUP, "GROUP_sub", null);

            // Assert
            assertEquals(200, response.getStatus());
            assertSame(expected, response.getEntity());
        }
    }

    @Test
    void getMember_notAMember_returns404() throws Exception {
        // Arrange
        when(groupDao.isDirectMember("someone")).thenReturn(false);

        // Act
        Response response = underTest.getMember("-home-", GROUP, "someone", null);

        // Assert
        assertEquals(404, response.getStatus());
    }

    @Test
    void getMember_blankAuthorityName_returns400() throws Exception {
        // Arrange
        when(groupDao.isDirectMember(" ")).thenThrow(DAOException.mapping(new IllegalArgumentException("empty")));

        // Act
        Response response = underTest.getMember("-home-", GROUP, " ", null);

        // Assert
        assertEquals(400, response.getStatus());
    }

    @Test
    void getMember_noAdminAccess_returns403() {
        // Arrange
        doThrow(new AccessDeniedException("denied")).when(groupDao).checkAdminAccess();

        // Act
        Response response = underTest.getMember("-home-", GROUP, "someone", null);

        // Assert
        assertEquals(403, response.getStatus());
    }
}
