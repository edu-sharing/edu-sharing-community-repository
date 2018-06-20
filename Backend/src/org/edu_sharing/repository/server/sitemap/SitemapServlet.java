package org.edu_sharing.repository.server.sitemap;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.apache.log4j.Logger;
import org.edu_sharing.metadataset.v2.MetadataReaderV2;
import org.edu_sharing.metadataset.v2.MetadataSetV2;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.repository.server.sitemap.xml.Sitemapindex;
import org.edu_sharing.repository.server.sitemap.xml.Urlset;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.URLTool;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.service.search.model.SearchToken;


public class SitemapServlet extends HttpServlet{
    public final static String NS_SITEMAP="http://www.sitemaps.org/schemas/sitemap/0.9";

    Logger logger = Logger.getLogger(SitemapServlet.class);
    private static final int NODES_PER_MAP = 500;
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            String from = req.getParameter("from");

            if(from==null){
                Sitemapindex index = getAll(req);
                toXml(index,resp);
            }
            else{
                Urlset set = getNodes(req,Integer.parseInt(from));
                toXml(set,resp);
            }
        }
        catch(Throwable t){
            t.printStackTrace();
            resp.sendError(500,t.toString());
        }
    }

    private Sitemapindex getAll(HttpServletRequest request) throws Throwable {
        Sitemapindex index = new Sitemapindex();
        SearchService search = SearchServiceFactory.getLocalService();

        SearchToken token=new SearchToken();
        token.setContentType(SearchService.ContentType.FILES);
        token.setMaxResult(Integer.MAX_VALUE);
        SearchResultNodeRef result = search.searchV2(getMds(request), MetadataSetV2.DEFAULT_CLIENT_QUERY, getSearchAllCriterias(), token);
        for(int i=0;i<result.getNodeCount();i+=NODES_PER_MAP){
            Sitemapindex.Sitemap map = new Sitemapindex.Sitemap();
            map.loc=request.getRequestURL()+"?from="+i;
            index.sitemap.add(map);
        }
        return index;
    }

    private void toXml(Object obj, HttpServletResponse resp) throws Exception {
        JAXBContext jaxbContext = JAXBContext.newInstance(obj.getClass());
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

        resp.setContentType("application/xml");
        // output pretty printed
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        jaxbMarshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, NS_SITEMAP);
        jaxbMarshaller.setProperty(Marshaller.JAXB_FRAGMENT,false);
        jaxbMarshaller.marshal(obj, resp.getOutputStream());
    }

    private Urlset getNodes(HttpServletRequest request, int from) throws Throwable {
        Urlset set = new Urlset();
        SearchService search = SearchServiceFactory.getLocalService();

        SearchToken token=new SearchToken();
        token.setContentType(SearchService.ContentType.FILES);
        token.setMaxResult(NODES_PER_MAP);
        token.setFrom(from);
        SearchResultNodeRef result = search.searchV2(getMds(request), MetadataSetV2.DEFAULT_CLIENT_QUERY, getSearchAllCriterias(), token);
        for(org.edu_sharing.service.model.NodeRef ref : result.getData()){
            Urlset.Url url=new Urlset.Url();
            url.loc=URLTool.getNgRenderNodeUrl(ref.getNodeId(),null);
            Urlset.Url.Image image = new Urlset.Url.Image();
            image.loc= URLTool.getPreviewServletUrl(ref);
            url.image.add(image);
            //getPreviewServletUrl
            set.url.add(url);
        }
        return set;
    }

    private Map<String, String[]> getSearchAllCriterias() {
        Map<String, String[]> criterias=new HashMap<>();
        criterias.put(MetadataSetV2.DEFAULT_CLIENT_QUERY_CRITERIA,new String[]{"*"});
        return criterias;
    }

    private MetadataSetV2 getMds(HttpServletRequest request)throws Throwable {
        String locale;
        try {
            locale = request.getLocale().toString();
        }catch(Throwable t){
            locale="de_DE";
        }
        return MetadataReaderV2.getMetadataset(ApplicationInfoList.getHomeRepository(), CCConstants.metadatasetdefault_id, locale);
    }

}
