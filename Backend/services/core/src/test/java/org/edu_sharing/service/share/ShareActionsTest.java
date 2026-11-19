package org.edu_sharing.service.share;

import org.edu_sharing.repository.client.rpc.Share;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.restservices.NodeDao;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShareActionsTest {

    @Test
    void share_withoutStoredActionsAndUnknownDefault_onlyAllowsDownload() {
        // Arrange
        Share share = new Share();

        // Act
        boolean view = share.isViewAllowed();
        boolean download = share.isDownloadAllowed();

        // Assert
        assertFalse(view);
        assertTrue(download);
    }

    @Test
    void share_withoutStoredActions_usesContextDefault() {
        // Arrange
        Share share = new Share();
        share.setDefaultView(true);
        share.setDefaultDownload(false);

        // Act
        boolean view = share.isViewAllowed();
        boolean download = share.isDownloadAllowed();

        // Assert
        assertTrue(view);
        assertFalse(download);
    }

    @Test
    void share_withStoredActions_ignoresContextDefault() {
        // Arrange
        Share share = new Share();
        share.setView(false);
        share.setDownload(true);
        share.setDefaultView(true);
        share.setDefaultDownload(false);

        // Act
        boolean view = share.isViewAllowed();
        boolean download = share.isDownloadAllowed();

        // Assert
        assertFalse(view);
        assertTrue(download);
    }

    @Test
    void share_withStoredActions_usesStoredValues() {
        // Arrange
        Share share = new Share();
        share.setView(true);
        share.setDownload(false);

        // Act
        boolean view = share.isViewAllowed();
        boolean download = share.isDownloadAllowed();

        // Assert
        assertTrue(view);
        assertFalse(download);
    }

    @Test
    void validateActions_withNeitherViewNorDownload_throws() {
        // Arrange / Act / Assert
        assertThrows(IllegalArgumentException.class, () -> GlobalShareServiceImpl.validateActions(false, false));
    }

    @Test
    void validateActions_withAtLeastOneAction_passes() {
        // Arrange / Act / Assert
        GlobalShareServiceImpl.validateActions(true, false);
        GlobalShareServiceImpl.validateActions(false, true);
        GlobalShareServiceImpl.validateActions(true, true);
    }

    @Test
    void getSharePermissions_withoutDownload_doesNotGrantDownloadContent() {
        // Arrange / Act
        List<String> permissions = NodeDao.getSharePermissions(false);

        // Assert
        assertTrue(permissions.contains(CCConstants.PERMISSION_READ_ALL));
        assertFalse(permissions.contains(CCConstants.PERMISSION_DOWNLOAD_CONTENT));
    }

    @Test
    void getSharePermissions_withDownload_grantsDownloadContentAndView() {
        // Arrange / Act
        List<String> permissions = NodeDao.getSharePermissions(true);

        // Assert
        assertEquals(List.of(
                CCConstants.PERMISSION_READ,
                CCConstants.PERMISSION_READ_PREVIEW,
                CCConstants.PERMISSION_READ_ALL,
                CCConstants.PERMISSION_DOWNLOAD_CONTENT), permissions);
    }
}
