package org.edu_sharing.repository.server.sitemap.xml;

import org.edu_sharing.repository.server.sitemap.SitemapServlet;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.Collection;

@XmlRootElement(namespace = SitemapServlet.NS_SITEMAP)
public class Sitemapindex {
    @XmlElement(namespace = SitemapServlet.NS_SITEMAP) public Collection<Sitemap> sitemap=new ArrayList<>();

    public static class Sitemap{
        @XmlElement(namespace = SitemapServlet.NS_SITEMAP) public String loc;
    }
}
