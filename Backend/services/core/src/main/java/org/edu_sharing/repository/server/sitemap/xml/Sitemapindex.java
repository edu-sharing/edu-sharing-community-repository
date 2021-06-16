package org.edu_sharing.repository.server.sitemap.xml;

import org.edu_sharing.repository.server.sitemap.SitemapServlet;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.Collection;

@XmlRootElement(namespace = SitemapServlet.NS_SITEMAP)
public class Sitemapindex {
    @XmlElement(namespace = SitemapServlet.NS_SITEMAP) public Collection<Sitemap> sitemap=new ArrayList<>();

    public static class Sitemap{
        @XmlElement(namespace = SitemapServlet.NS_SITEMAP) public String loc;
    }
}
