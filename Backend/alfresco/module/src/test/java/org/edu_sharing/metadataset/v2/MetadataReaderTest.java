package org.edu_sharing.metadataset.v2;

import org.alfresco.repo.cache.MemoryCache;
import org.alfresco.repo.cache.SimpleCache;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.context.ApplicationContext;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MetadataReaderTest {
    @Test
    void testWidgets() throws Exception {
        // Create a spy of your real MetataReader
        try (MockedStatic<AlfAppContextGate> mockedGate = mockStatic(AlfAppContextGate.class)) {
            ApplicationContext mockContext = mock(ApplicationContext.class);
            SimpleCache<String, MetadataSet> mockCache = mock(MemoryCache.class);

            mockedGate.when(AlfAppContextGate::getApplicationContext).thenReturn(mockContext);
            when(mockContext.getBean("eduSharingMdsCache")).thenReturn(mockCache);

            // Now MetataReader.mdsCache will be initialized with mockCache when class loads

            try (MockedStatic<MetadataReader> mocked = mockStatic(MetadataReader.class)) {
                InputStream testInputStream = getClass().getClassLoader().getResource("mds_widgets.xml").openStream();


                mocked.when(() -> MetadataReader.getFile(anyString(), eq(Filetype.MDS)))
                        .thenReturn(testInputStream);
                MetadataReader reader = new MetadataReader("mds_widgets", "de");

                List<MetadataWidget> widgets = reader.getWidgets(null);
                assertEquals(2, widgets.size());
                assertEquals("cm:name", widgets.get(0).getId());
                assertEquals("text", widgets.get(0).getType());
                assertEquals(MetadataWidget.Required.mandatory, widgets.get(0).getRequired());
                assertTrue(widgets.get(0).getInputPreprocessor().isEmpty());

                assertEquals(1, widgets.get(1).getInputPreprocessor().size());
                assertEquals(MetadataWidget.MetadataInputPreprocessor.trim, widgets.get(1).getInputPreprocessor().get(0));
                // Optionally, assert something about the InputStream content
            }
        }
    }
}