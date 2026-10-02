package org.edu_sharing.repository.server.tools;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Behaviour of {@link ApplicationInfoList} when the application infos can not be loaded:
 * on a cold start the next call tries again, on a refresh the previous state is kept.
 * <p>
 * ApplicationInfo and PropertiesHelper load their files via the thread context class loader, so the failures are
 * provoked by a class loader which hides the repository registry or redirects it to another test file.
 */
class ApplicationInfoListFailureTest {

    private static final String REGISTRY = PropertiesHelper.Config.getPropertyFilePath("ccapp-registry.properties.xml");
    private static final String FAILURE_DIR = "applicationinfolist-failure/";
    private static final int EXPECTED_APP_COUNT = 5;

    private ClassLoader originalClassLoader;

    @BeforeEach
    void setUp() throws Exception {
        originalClassLoader = Thread.currentThread().getContextClassLoader();
        setSnapshot(null);
    }

    @AfterEach
    void tearDown() throws Exception {
        Thread.currentThread().setContextClassLoader(originalClassLoader);
        setSnapshot(null);
    }

    @Test
    void coldStart_registryMissing_returnsEmptyStateAndRetries() throws Exception {
        useRegistry(null);

        assertNull(ApplicationInfoList.getHomeRepository());
        assertNull(ApplicationInfoList.getRenderingService2());
        assertTrue(ApplicationInfoList.getApplicationInfos().isEmpty());
        assertTrue(ApplicationInfoList.getAppInfoIds().isEmpty());
        assertNotNull(ApplicationInfoList.getAppInfosLtiTool());
        assertTrue(ApplicationInfoList.getAppInfosLtiTool().isEmpty());
        assertNull(ApplicationInfoList.getRepositoryInfoById("home"));
        assertNull(getSnapshot(), "a failed init must not publish a snapshot, so the next call tries again");

        Thread.currentThread().setContextClassLoader(originalClassLoader);

        assertEquals(EXPECTED_APP_COUNT, ApplicationInfoList.getApplicationInfos().size());
        assertNotNull(ApplicationInfoList.getHomeRepository());
    }

    @Test
    void refresh_registryMissing_keepsPreviousSnapshot() throws Exception {
        assertRefreshKeepsPreviousSnapshot(null);
    }

    @Test
    void refresh_emptyApplicationFiles_keepsPreviousSnapshot() throws Exception {
        assertRefreshKeepsPreviousSnapshot(FAILURE_DIR + "ccapp-registry-empty.properties.xml");
    }

    @Test
    void refresh_noLoadableApplicationFile_keepsPreviousSnapshot() throws Exception {
        assertRefreshKeepsPreviousSnapshot(FAILURE_DIR + "ccapp-registry-missing-files.properties.xml");
    }

    @Test
    void refresh_singleBrokenApplicationFiles_loadsRemainingOnes() throws Exception {
        ApplicationInfo previousHome = ApplicationInfoList.getHomeRepository();
        assertNotNull(previousHome);

        // app-missing.xml does not exist, app-noappid.xml has no appid: both are skipped
        useRegistry(FAILURE_DIR + "ccapp-registry-partial.properties.xml");
        ApplicationInfoList.refresh();

        assertEquals(1, ApplicationInfoList.getApplicationInfos().size());
        ApplicationInfo home = ApplicationInfoList.getHomeRepository();
        assertNotNull(home);
        assertEquals("home", home.getAppId());
        assertNotSame(previousHome, home, "a partially successful refresh must replace the previous snapshot");
        assertNull(ApplicationInfoList.getRenderingService2());
        assertTrue(ApplicationInfoList.getAppInfosLtiTool().isEmpty());
    }

    private void assertRefreshKeepsPreviousSnapshot(String registryReplacement) throws Exception {
        ApplicationInfo previousHome = ApplicationInfoList.getHomeRepository();
        ApplicationInfo previousRenderingService2 = ApplicationInfoList.getRenderingService2();
        assertNotNull(previousHome);
        assertNotNull(previousRenderingService2);
        Object previousSnapshot = getSnapshot();

        useRegistry(registryReplacement);
        ApplicationInfoList.refresh();

        assertSame(previousSnapshot, getSnapshot());
        assertEquals(EXPECTED_APP_COUNT, ApplicationInfoList.getApplicationInfos().size());
        assertSame(previousHome, ApplicationInfoList.getHomeRepository());
        assertSame(previousRenderingService2, ApplicationInfoList.getRenderingService2());
        assertEquals(1, ApplicationInfoList.getAppInfosLtiTool().size());
    }

    /**
     * @param replacement resource to deliver instead of the repository registry, null to hide the registry
     */
    private void useRegistry(String replacement) {
        Thread.currentThread().setContextClassLoader(new ClassLoader(originalClassLoader) {
            @Override
            public URL getResource(String name) {
                if (REGISTRY.equals(name)) {
                    return replacement == null ? null : super.getResource(replacement);
                }
                return super.getResource(name);
            }
        });
    }

    private static Object getSnapshot() throws Exception {
        return snapshotField().get(null);
    }

    private static void setSnapshot(Object value) throws Exception {
        snapshotField().set(null, value);
    }

    private static Field snapshotField() throws Exception {
        Field field = ApplicationInfoList.class.getDeclaredField("snapshot");
        field.setAccessible(true);
        return field;
    }
}
