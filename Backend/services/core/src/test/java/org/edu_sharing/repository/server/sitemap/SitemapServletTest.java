package org.edu_sharing.repository.server.sitemap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SitemapServletTest {

    @Test
    void sessionIdIsRemovedFromTheDownloadUrl() {
        assertEquals(
                "https://host/edu-sharing/eduservlet/redirect?APP_ID=local&NODE_ID=abc&params=display%3Ddownload",
                SitemapServlet.removeVolatileUrlParts(
                        "https://host/edu-sharing/eduservlet/redirect;jsessionid=820FF39C84A1FDE0E5D0909E6DF321E5?APP_ID=local&NODE_ID=abc&params=display%3Ddownload"));
    }

    @Test
    void cacheBusterIsRemovedFromThePreviewUrl() {
        assertEquals(
                "https://host/edu-sharing/preview?nodeId=abc&storeProtocol=workspace&storeId=SpacesStore",
                SitemapServlet.removeVolatileUrlParts(
                        "https://host/edu-sharing/preview?nodeId=abc&storeProtocol=workspace&storeId=SpacesStore&dontcache=1792156664565"));
    }

    @Test
    void cacheBusterInTheMiddleOrAtTheStartIsRemoved() {
        assertEquals("https://host/p?a=1&b=2", SitemapServlet.removeVolatileUrlParts("https://host/p?a=1&dontcache=5&b=2"));
        assertEquals("https://host/p?a=1", SitemapServlet.removeVolatileUrlParts("https://host/p?dontcache=5&a=1"));
        assertEquals("https://host/p", SitemapServlet.removeVolatileUrlParts("https://host/p?dontcache=5"));
    }

    @Test
    void otherUrlsStayUntouched() {
        assertEquals("https://host/p?a=1&b=2", SitemapServlet.removeVolatileUrlParts("https://host/p?a=1&b=2"));
        assertNull(SitemapServlet.removeVolatileUrlParts(null));
    }
}
