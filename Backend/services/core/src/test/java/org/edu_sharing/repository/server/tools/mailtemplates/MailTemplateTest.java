package org.edu_sharing.repository.server.tools.mailtemplates;

import org.edu_sharing.repository.tools.URLHelper;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MailTemplateTest {
    @Test
    void testAddContentLinks_directoryWithCollection() {
        String nodeId = "node123";
        Map<String, String> target = new HashMap<>();
        String keyName = "linkKey";

        try (MockedStatic<URLHelper> urlHelperMock = Mockito.mockStatic(URLHelper.class)) {
            urlHelperMock.when(URLHelper::getNgComponentsUrl).thenReturn("http://ng/");
            urlHelperMock.when(() -> URLHelper.getNgComponentsUrl(false)).thenReturn("http://ng/");

            MailTemplate.addContentLinks(nodeId, target, keyName, "collection");
            assertEquals("http://ng/collections?id=node123", target.get(keyName));

            MailTemplate.addContentLinks(nodeId, target, keyName, "folder");
            assertEquals("http://ng/workspace?id=node123", target.get(keyName));

            MailTemplate.addContentLinks(nodeId, target, keyName, "file-image");
            assertEquals("http://ng/render/node123?closeOnBack=true", target.get(keyName));

            MailTemplate.addContentLinks(nodeId, target, keyName, "saved_search");
            assertEquals("http://ng/search?savedSearch=node123", target.get(keyName));
        }
    }

}