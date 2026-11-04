package org.edu_sharing.repository.server.sitemap;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import org.edu_sharing.repository.server.sitemap.xml.Urlset;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SitemapServletTest {

    private static List<? extends Config> pages(String hocon) {
        return ConfigFactory.parseString(hocon).getConfigList("pages");
    }

    @Test
    void staticPagesGetTheirUrlPriorityAndChangeFrequency() {
        Urlset set = SitemapServlet.buildStaticUrlset(pages("""
                pages: [
                    { path: "", priority: 1.0, changefreq: "daily" }
                    { path: "components/search", priority: 0.9, changefreq: "weekly" }
                ]"""), "https://host/edu-sharing");
        List<Urlset.Url> urls = List.copyOf(set.url);
        assertEquals(2, urls.size());
        assertEquals("https://host/edu-sharing/", urls.get(0).loc);
        assertEquals(1.0, urls.get(0).priority);
        assertEquals("daily", urls.get(0).changefreq);
        assertEquals("https://host/edu-sharing/components/search", urls.get(1).loc);
        assertEquals(0.9, urls.get(1).priority);
    }

    @Test
    void trailingAndLeadingSlashesDoNotDoubleUp() {
        Urlset set = SitemapServlet.buildStaticUrlset(pages("pages: [{ path: \"/components/start\" }]"), "https://host/edu-sharing/");
        assertEquals("https://host/edu-sharing/components/start", set.url.iterator().next().loc);
    }

    @Test
    void invalidPriorityAndChangeFrequencyAreLeftOut() {
        Urlset set = SitemapServlet.buildStaticUrlset(pages("pages: [{ path: \"a\", priority: 5, changefreq: \"sometimes\" }]"), "https://host");
        Urlset.Url url = set.url.iterator().next();
        assertEquals("https://host/a", url.loc);
        assertNull(url.priority);
        assertNull(url.changefreq);
    }

    @Test
    void staticPagesAreWrittenInTheOrderOfTheSitemapSchema() throws Exception {
        Urlset set = SitemapServlet.buildStaticUrlset(pages("pages: [{ path: \"\", priority: 1.0, changefreq: \"daily\" }]"), "https://host/edu-sharing");
        StringWriter out = new StringWriter();
        Marshaller marshaller = JAXBContext.newInstance(Urlset.class).createMarshaller();
        marshaller.marshal(set, out);
        String xml = out.toString();
        assertTrue(xml.contains("<loc>https://host/edu-sharing/</loc>"), xml);
        assertTrue(xml.contains("<priority>1.0</priority>"), xml);
        assertTrue(xml.indexOf("<loc>") < xml.indexOf("<changefreq>"), xml);
        assertTrue(xml.indexOf("<changefreq>") < xml.indexOf("<priority>"), xml);
    }

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
