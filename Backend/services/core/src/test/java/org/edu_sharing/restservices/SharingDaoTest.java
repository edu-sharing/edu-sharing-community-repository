package org.edu_sharing.restservices;

import org.edu_sharing.repository.client.rpc.Share;
import org.edu_sharing.service.share.GlobalShareService;
import org.edu_sharing.service.share.GlobalShareServiceImpl;
import org.edu_sharing.spring.ApplicationContextFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the checks in front of the signed share data. The happy path needs a running repository
 * (node conversion, signing) and is verified on the dev stack.
 */
@ExtendWith(MockitoExtension.class)
class SharingDaoTest {

    private static final String NODE = "node-id";
    private static final String TOKEN = "token";

    @Mock
    private GlobalShareService globalShareService;
    @Mock
    private ApplicationContext applicationContext;
    @Mock
    private RepositoryDao repoDao;

    private MockedStatic<ApplicationContextFactory> applicationContextFactory;

    @BeforeEach
    void setUp() {
        applicationContextFactory = Mockito.mockStatic(ApplicationContextFactory.class);
        applicationContextFactory.when(ApplicationContextFactory::getApplicationContext).thenReturn(applicationContext);
        when(applicationContext.getBean(GlobalShareService.class)).thenReturn(globalShareService);
    }

    @AfterEach
    void tearDown() {
        applicationContextFactory.close();
    }

    private Share share(boolean view, boolean download, long expiry, String password) {
        Share share = new Share();
        share.setView(view);
        share.setDownload(download);
        share.setExpiryDate(expiry);
        share.setPassword(password);
        when(globalShareService.getShare(NODE, TOKEN)).thenReturn(share);
        return share;
    }

    @Test
    void getSignedNode_withUnknownShare_throws() {
        // Arrange
        when(globalShareService.getShare(NODE, TOKEN)).thenReturn(null);

        // Act / Assert
        assertThrows(DAOException.class, () -> SharingDao.getSignedNode(repoDao, NODE, TOKEN, null, null));
    }

    @Test
    void getSignedNode_withExpiredShare_throws() {
        // Arrange
        share(true, true, System.currentTimeMillis() - 60_000, null);

        // Act / Assert
        assertThrows(DAOSecurityException.class, () -> SharingDao.getSignedNode(repoDao, NODE, TOKEN, null, null));
    }

    @Test
    void getSignedNode_withWrongPassword_throws() {
        // Arrange
        share(true, true, GlobalShareService.EXPIRY_DATE_UNLIMITED, GlobalShareServiceImpl.encryptPassword("secret"));

        // Act / Assert
        assertThrows(DAOSecurityException.class, () -> SharingDao.getSignedNode(repoDao, NODE, TOKEN, "wrong", null));
    }

    @Test
    void getSignedNode_withDownloadOnlyShare_throws() {
        // Arrange
        share(false, true, GlobalShareService.EXPIRY_DATE_UNLIMITED, null);

        // Act / Assert
        assertThrows(DAOSecurityException.class, () -> SharingDao.getSignedNode(repoDao, NODE, TOKEN, null, null));
    }

    @Test
    void getSignedNode_withShareWithoutStoredActionsAndDefaultWithoutView_throws() {
        // Arrange: a share without stored choice uses the context default, which does not allow viewing here
        Share legacy = new Share();
        legacy.setExpiryDate(GlobalShareService.EXPIRY_DATE_UNLIMITED);
        legacy.setDefaultView(false);
        legacy.setDefaultDownload(true);
        when(globalShareService.getShare(NODE, TOKEN)).thenReturn(legacy);

        // Act / Assert
        assertThrows(DAOSecurityException.class, () -> SharingDao.getSignedNode(repoDao, NODE, TOKEN, null, null));
    }

    @Test
    void getMetadataSet_withDownloadOnlyShare_throws() {
        // Arrange
        share(false, true, GlobalShareService.EXPIRY_DATE_UNLIMITED, null);

        // Act / Assert
        assertThrows(DAOSecurityException.class, () -> SharingDao.getMetadataSet(repoDao, NODE, TOKEN, null, null));
    }

    @Test
    void getMetadataSet_withWrongPassword_throws() {
        // Arrange
        share(true, true, GlobalShareService.EXPIRY_DATE_UNLIMITED, GlobalShareServiceImpl.encryptPassword("secret"));

        // Act / Assert
        assertThrows(DAOSecurityException.class, () -> SharingDao.getMetadataSet(repoDao, NODE, TOKEN, "wrong", null));
    }

    @Test
    void getMetadataSet_withChildOutsideTheShare_throws() {
        // Arrange
        share(true, false, GlobalShareService.EXPIRY_DATE_UNLIMITED, null);
        when(globalShareService.isNodeAccessibleViaShare(any(), Mockito.eq("foreign"))).thenReturn(false);

        // Act / Assert
        assertThrows(DAOSecurityException.class, () -> SharingDao.getMetadataSet(repoDao, NODE, TOKEN, null, "foreign"));
    }

    @Test
    void getSignedNode_withChildOutsideTheShare_throws() {
        // Arrange
        share(true, false, GlobalShareService.EXPIRY_DATE_UNLIMITED, null);
        when(globalShareService.isNodeAccessibleViaShare(any(), Mockito.eq("foreign"))).thenReturn(false);

        // Act / Assert
        assertThrows(DAOSecurityException.class, () -> SharingDao.getSignedNode(repoDao, NODE, TOKEN, null, "foreign"));
        verify(globalShareService).isNodeAccessibleViaShare(any(), Mockito.eq("foreign"));
    }
}
