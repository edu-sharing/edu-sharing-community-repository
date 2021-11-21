package org.edu_sharing.repository.server.sitemap;

import com.google.common.net.InternetDomainName;
import org.apache.log4j.Logger;
import org.edu_sharing.metadataset.v2.MetadataReaderV2;
import org.edu_sharing.metadataset.v2.MetadataSetV2;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.repository.server.sitemap.xml.Sitemapindex;
import org.edu_sharing.repository.server.sitemap.xml.Urlset;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.URLTool;
import org.edu_sharing.service.mime.MimeTypesV2;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.service.search.model.SearchToken;
import org.edu_sharing.service.search.model.SortDefinition;
import org.springframework.util.StreamUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;


public class RobotsTXTServlet extends HttpServlet{

    Logger logger = Logger.getLogger(RobotsTXTServlet.class);
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {

            String robots=StreamUtils.copyToString(getClass().getResourceAsStream("robots.txt.properties"), Charset.forName("UTF-8"));
            robots=robots.replace("{{sitemap}}",req.getRequestURL().toString().replace("robots.txt","eduservlet/sitemap"));
            resp.getOutputStream().print(robots);
        } catch (Throwable t) {
            t.printStackTrace();
            resp.sendError(500, t.toString());
        }
    }

}
