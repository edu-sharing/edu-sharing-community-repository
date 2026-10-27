package org.edu_sharing.repository.server.sitemap;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.typesafe.config.Config;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;

import lombok.extern.slf4j.Slf4j;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.metadataset.v2.MetadataReader;
import org.edu_sharing.metadataset.v2.MetadataSet;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.repository.server.sitemap.xml.Sitemapindex;
import org.edu_sharing.repository.server.sitemap.xml.Urlset;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.URLTool;
import org.edu_sharing.repository.tools.URLHelper;
import org.edu_sharing.service.mime.MimeTypesV2;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.SearchServiceElastic;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.service.search.model.SearchToken;
import org.edu_sharing.service.search.model.SitemapEntry;
import org.edu_sharing.service.search.model.SitemapPage;
import org.edu_sharing.service.search.model.SortDefinition;
import org.edu_sharing.spring.servlet.SpringHttpServlet;
import org.springframework.beans.factory.annotation.Autowired;


@Slf4j
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public class SitemapServlet extends SpringHttpServlet {
    public final static String NS_SITEMAP="http://www.sitemaps.org/schemas/sitemap/0.9";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneId.systemDefault());
    private static final int NODES_PER_MAP = 500;
    private static final int DEFAULT_PAGE_SIZE = 5000;
    private static final String CONFIG_PAGE_SIZE = "angular.sitemap.pageSize";
    private static final String CONFIG_STATIC_PAGES = "angular.sitemap.staticPages";
    private static final String TYPE_STATIC = "static";
    private static final Set<String> CHANGE_FREQUENCIES = Set.of("always", "hourly", "daily", "weekly", "monthly", "yearly", "never");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            String from = req.getParameter("from");
            String after = req.getParameter("after");
            String type = req.getParameter("type");
            if (TYPE_STATIC.equals(type)) {
                toXml(getStaticPages(), resp);
                return;
            }
            SearchService search = SearchServiceFactory.getInstance().getLocalService();

            if (search instanceof SearchServiceElastic) {
                SearchServiceElastic elastic = (SearchServiceElastic) search;
                if (type == null) {
                    toXml(getAllElastic(req, elastic), resp);
                } else if (from != null) {
                    // legacy offset urls (before the cursor paging): the pages are not addressable anymore
                    toXml(new Urlset(), resp);
                } else {
                    toXml(getNodesElastic(elastic, type, after == null ? null : Long.valueOf(after)), resp);
                }
            } else if (from == null) {
                Sitemapindex index = getAll(req);
                toXml(index, resp);
            } else {
                Urlset set = getNodes(req, type, Integer.parseInt(from));
                toXml(set, resp);
            }
        }
        catch(NumberFormatException t){
            resp.sendError(400, "invalid parameter: " + t.getMessage());
        }
        catch(Throwable t){
            log.error(t.getMessage(),t);
            resp.sendError(500,t.toString());
        }
    }

    @Autowired
    private LightbendConfigLoader lightbendConfigLoader;

    private int getPageSize() {
        try {
            return Math.max(1, lightbendConfigLoader.getConfig().getInt(CONFIG_PAGE_SIZE));
        } catch (Throwable t) {
            return DEFAULT_PAGE_SIZE;
        }
    }

    private boolean hasStaticPages() {
        Config config = lightbendConfigLoader.getConfig();
        return config.hasPath(CONFIG_STATIC_PAGES) && !config.getConfigList(CONFIG_STATIC_PAGES).isEmpty();
    }

    /**
     * the sitemap of the entry pages that are not nodes (e.g. the start page and the search), from the config
     */
    private Urlset getStaticPages() {
        Config config = lightbendConfigLoader.getConfig();
        List<? extends Config> pages = config.hasPath(CONFIG_STATIC_PAGES) ? config.getConfigList(CONFIG_STATIC_PAGES) : List.of();
        return buildStaticUrlset(pages, URLHelper.getBaseUrl(true));
    }

    static Urlset buildStaticUrlset(List<? extends Config> pages, String baseUrl) {
        Urlset set = new Urlset();
        String base = baseUrl.replaceAll("/$", "");
        for (Config page : pages) {
            Urlset.Url url = new Urlset.Url();
            String path = page.hasPath("path") ? page.getString("path").replaceAll("^/+", "") : "";
            url.loc = base + "/" + path;
            if (page.hasPath("priority")) {
                double priority = page.getDouble("priority");
                if (priority >= 0 && priority <= 1) {
                    url.priority = priority;
                } else {
                    log.warn("Ignoring priority {} of the static sitemap page '{}', it must be between 0.0 and 1.0", priority, path);
                }
            }
            if (page.hasPath("changefreq")) {
                String changefreq = page.getString("changefreq");
                if (CHANGE_FREQUENCIES.contains(changefreq)) {
                    url.changefreq = changefreq;
                } else {
                    log.warn("Ignoring changefreq '{}' of the static sitemap page '{}', allowed are {}", changefreq, path, CHANGE_FREQUENCIES);
                }
            }
            set.url.add(url);
        }
        return set;
    }

    private void addStaticSitemap(Sitemapindex index, HttpServletRequest request) {
        if (hasStaticPages()) {
            Sitemapindex.Sitemap map = new Sitemapindex.Sitemap();
            map.loc = request.getRequestURL() + "?type=" + TYPE_STATIC;
            index.sitemap.add(map);
        }
    }

    private static String formatDate(long epochMillis) {
        return DATE_FORMAT.format(Instant.ofEpochMilli(epochMillis));
    }

    /**
     * The urls of the download and the preview carry the session id of the request and a cache buster that changes
     * with every call. Neither belongs in a public file, and an url that changes on every fetch is no stable url.
     */
    static String removeVolatileUrlParts(String url) {
        if (url == null) {
            return null;
        }
        String result = url
                .replaceAll(";jsessionid=[^?&#]*", "")
                .replaceAll("&dontcache=[^&#]*", "")
                .replaceAll("\\?dontcache=[^&#]*&?", "?");
        return result.endsWith("?") ? result.substring(0, result.length() - 1) : result;
    }

    private static SearchService.ContentType getContentType(String type) {
        return "collection".equals(type) ? SearchService.ContentType.COLLECTIONS : SearchService.ContentType.FILES;
    }

    private Sitemapindex getAllElastic(HttpServletRequest request, SearchServiceElastic elastic) throws Throwable {
        Sitemapindex index = new Sitemapindex();
        addStaticSitemap(index, request);
        for (String type : new String[]{"io", "collection"}) {
            for (SitemapPage page : elastic.getSitemapPages(getContentType(type), getPageSize())) {
                Sitemapindex.Sitemap map = new Sitemapindex.Sitemap();
                map.loc = request.getRequestURL() + "?type=" + type + (page.getAfter() != null ? "&after=" + page.getAfter() : "");
                if (page.getLastModified() != null) {
                    map.lastmod = formatDate(page.getLastModified());
                }
                index.sitemap.add(map);
            }
        }
        return index;
    }

    private Urlset getNodesElastic(SearchServiceElastic elastic, String type, Long after) throws Throwable {
        Urlset set = new Urlset();
        for (SitemapEntry entry : elastic.getSitemapEntries(getContentType(type), after, getPageSize())) {
            Urlset.Url url = new Urlset.Url();
            if (entry.getModified() != null) {
                url.lastmod = formatDate(entry.getModified());
            }
            url.loc = entry.isCollection()
                    ? URLTool.getNgCollectionUrl(entry.getNodeId())
                    : URLHelper.getNgRenderNodeUrl(entry.getNodeId(), null);
            // only nodes with their own binary have a download url, links (e.g. to a video on another site) have none
            // and would be an incomplete video entry
            if ("file-video".equals(entry.getMediatype()) && entry.isContent()) {
                try {
                    String contentLoc = new MCAlfrescoAPIClient().getDownloadUrl(entry.getNodeId());
                    if (contentLoc != null) {
                        Urlset.Url.Video video = new Urlset.Url.Video();
                        video.title = entry.getName();
                        video.content_loc = removeVolatileUrlParts(contentLoc);
                        video.thumbnail_loc = removeVolatileUrlParts(NodeServiceHelper.getPreview(
                                new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, entry.getNodeId())).getUrl());
                        url.video.add(video);
                    }
                } catch (Throwable t) {
                    log.warn("Can not read video data of {}: {}", entry.getNodeId(), t.getMessage());
                }
            }
            set.url.add(url);
        }
        return set;
    }

    private Sitemapindex getAll(HttpServletRequest request) throws Throwable {
        Sitemapindex index = new Sitemapindex();
        addStaticSitemap(index, request);
        SearchService search = SearchServiceFactory.getInstance().getLocalService();

        SearchToken token=new SearchToken();
        token.setContentType(SearchService.ContentType.FILES);
        token.setMaxResult(0);
        token.setExcludes(List.of("preview"));
        SearchResultNodeRef resultFiles = search.search(getMds(request), MetadataSet.DEFAULT_CLIENT_QUERY, getSearchAllCriterias(), token);
        token.setContentType(SearchService.ContentType.COLLECTIONS);
        SearchResultNodeRef resultCollections = search.search(getMds(request), MetadataSet.DEFAULT_CLIENT_QUERY, getSearchAllCriterias(), token);
        for(int i=0;i<resultFiles.getNodeCount();i+=NODES_PER_MAP){
            Sitemapindex.Sitemap map = new Sitemapindex.Sitemap();
            map.loc=request.getRequestURL()+"?type=io&from="+i;
            index.sitemap.add(map);
        }
        for(int i=0;i<resultCollections.getNodeCount();i+=NODES_PER_MAP){
            Sitemapindex.Sitemap map = new Sitemapindex.Sitemap();
            map.loc=request.getRequestURL()+"?type=collection&from="+i;
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

    private Urlset getNodes(HttpServletRequest request, String type, int from) throws Throwable {
        Urlset set = new Urlset();
        SearchService search = SearchServiceFactory.getInstance().getLocalService();
        NodeService nodeService = NodeServiceFactory.getInstance().getLocalService();

        SearchToken token=new SearchToken();
        if("collection".equals(type))
            token.setContentType(SearchService.ContentType.COLLECTIONS);
        else
            token.setContentType(SearchService.ContentType.FILES);
        token.setMaxResult(NODES_PER_MAP);
        token.setFrom(from);
        token.setExcludes(List.of("preview"));
        SortDefinition sort = new SortDefinition();
        sort.addSortDefinitionEntry(new SortDefinition.SortDefinitionEntry(CCConstants.getValidLocalName(CCConstants.CM_PROP_C_CREATED),true));
        token.setSortDefinition(sort);
        SearchResultNodeRef result = search.search(getMds(request), MetadataSet.DEFAULT_CLIENT_QUERY, getSearchAllCriterias(), token);
        for(org.edu_sharing.service.model.NodeRef ref : result.getData()){
            log.info("node:{} {}", ref.getNodeId(), ref.getStoreId());
            Urlset.Url url=new Urlset.Url();
            String[] aspects = nodeService.getAspects(ref.getStoreProtocol(),ref.getStoreId(),ref.getNodeId());
            Date property = (ref.getProperties() != null)
                    ? new Date((long)ref.getProperties().get(CCConstants.CM_PROP_C_MODIFIED))
                    : (Date) nodeService.getPropertyNative(ref.getStoreProtocol(), ref.getStoreId(), ref.getNodeId(), CCConstants.CM_PROP_C_MODIFIED);
            url.lastmod = formatDate(property.getTime());
            if(Arrays.asList(aspects).contains(CCConstants.CCM_ASPECT_COLLECTION)){
                url.loc=URLTool.getNgCollectionUrl(ref.getNodeId());
            }
            else {
                url.loc = URLHelper.getNgRenderNodeUrl(ref.getNodeId(), null);
            }
            String mimetype=nodeService.getContentMimetype(ref.getStoreProtocol(),ref.getStoreId(),ref.getNodeId());
            if(MimeTypesV2.getTypeFromMimetype(mimetype).equals("file-video")){
                Urlset.Url.Video video = new Urlset.Url.Video();
                video.thumbnail_loc = NodeServiceHelper.getPreview(ref).getUrl();
                try {
                    video.content_loc = new MCAlfrescoAPIClient().getDownloadUrl(ref.getNodeId());
                }catch(Throwable t){
                    log.warn("Can not read download url: {}", t.getMessage());
                }                video.title = nodeService.getProperty(ref.getStoreProtocol(),ref.getStoreId(),ref.getNodeId(),CCConstants.CM_NAME);
                url.video.add(video);
            }
//            else {
//                Urlset.Url.Image image = new Urlset.Url.Image();
//                image.loc = NodeServiceHelper.getPreview(ref).getUrl();
                //url.image.add(image);
//            }
            //getPreviewServletUrl
            set.url.add(url);
        }
        return set;
    }

    private Map<String, String[]> getSearchAllCriterias() {
        Map<String, String[]> criterias=new HashMap<>();
        criterias.put(MetadataSet.DEFAULT_CLIENT_QUERY_CRITERIA,new String[]{""});
        return criterias;
    }

    private MetadataSet getMds(HttpServletRequest request)throws Throwable {
        String locale;
        try {
            locale = request.getLocale().toString();
        }catch(Throwable t){
            locale="de_DE";
        }
        return MetadataReader.getMetadataset(ApplicationInfoList.getHomeRepository(), CCConstants.metadatasetdefault_id, locale);
    }

}
