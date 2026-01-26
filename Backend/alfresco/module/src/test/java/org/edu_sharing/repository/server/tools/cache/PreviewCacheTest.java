package org.edu_sharing.repository.server.tools.cache;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mockStatic;

class PreviewCacheTest {
    @Test
    void getFileForNodeTest() {
        try (MockedStatic<PreviewCache> mockedStatic =
                     mockStatic(PreviewCache.class)) {

            mockedStatic.when(PreviewCache::getCacheStore)
                    .thenReturn(new File("/tmp"));

            // Call real method under test
            mockedStatic.when(() ->
                    PreviewCache.getFileForNode(
                            anyString(),
                            anyInt(),
                            anyInt(),
                            anyInt(),
                            anyInt(),
                            anyBoolean()
                    )).thenCallRealMethod();
            mockedStatic.when(() ->
                        PreviewCache.getFolder(
                            anyInt(),
                            anyInt(),
                            anyInt(),
                            anyInt(),
                            anyBoolean()
                    )).thenCallRealMethod();

            assertEquals("/tmp/200x100/abcd/abcd1234.jpg", PreviewCache.getFileForNode(
                    "abcd1234",
                    200,
                    100,
                    0,
                    0,
                    true
            ).getAbsolutePath());
            assertEquals("/tmp/m_400x400/abcd/abcd1234.jpg", PreviewCache.getFileForNode(
                    "abcd1234",
                    0,
                    0,
                    400,
                    400,
                    true
            ).getAbsolutePath());
        }
    }
}