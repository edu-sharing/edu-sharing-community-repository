package org.edu_sharing.repository.server.oai;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.dspace.xoai.dataprovider.handlers.results.ListItemIdentifiersResult;
import org.dspace.xoai.dataprovider.model.ItemIdentifier;
import org.dspace.xoai.model.oaipmh.OAIPMH;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.metadataset.v2.MetadataReaderV2;
import org.edu_sharing.metadataset.v2.MetadataSetV2;
import org.edu_sharing.metadataset.v2.tools.MetadataHelper;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.UrlTool;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.repository.server.exporter.OAILOMExporter;
import org.edu_sharing.repository.server.exporter.OAILOMWithSubobjectsExporter;
import org.edu_sharing.repository.server.sitemap.xml.Sitemapindex;
import org.edu_sharing.repository.server.sitemap.xml.Urlset;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.Mail;
import org.edu_sharing.repository.server.tools.URLTool;
import org.edu_sharing.service.mime.MimeTypesV2;
import org.edu_sharing.service.model.NodeRef;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.service.search.model.SearchToken;
import org.edu_sharing.service.search.model.SortDefinition;
import org.edu_sharing.xoai.EduDataHandler;
import org.edu_sharing.xoai.EduItem;
import org.edu_sharing.xoai.EduItemIdentifier;
import org.edu_sharing.xoai.EduOai;
import org.springframework.context.ApplicationContext;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;


public class OaiServlet extends HttpServlet{
    private static final int MAX_ITEMS_PER_PAGE = 300;
    Logger logger = Logger.getLogger(OaiServlet.class);
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            //oai/provider?verb=GetRecord&metadataPrefix=lom&identifier=3410648a-465e-47ff-87fe-706b89cecd65
            EduOai oai = new EduOai(MAX_ITEMS_PER_PAGE,
                    URLTool.getBaseUrl(),
                    ApplicationInfoList.getHomeRepository().getAppCaption(),
                    new Mail().getProperties().getProperty("mail.admin"),
                    new Date(0),
                    new Handler());
            Map<String, List<String>> request = mapRequest(req);
            OAIPMH response = oai.handleRequest(request);
            String responseXML = EduOai.responseToXML(response);
            resp.setHeader("Content-Type","application/xml");
            resp.getOutputStream().write(responseXML.getBytes());
        }
        catch(Throwable t){
            logger.warn(t.getMessage(),t);
            resp.sendError(500,t.getMessage());
        }
    }

    private Map<String, List<String>> mapRequest(HttpServletRequest req) {
        Map<String, List<String>> request=new HashMap<>();
        for(Object r : req.getParameterMap().entrySet()){
            Map.Entry<String,String[]> data= (Map.Entry<String, String[]>) r;
            request.put(data.getKey(), Arrays.asList(data.getValue()));
        }
        return request;
    }

    private class Handler implements EduDataHandler {
        private final ServiceRegistry serviceRegistry;

        public Handler(){

            ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
            serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
        }
        @Override
        public ListItemIdentifiersResult getIdentifiers(int from, int length) {
            return getIdentifiersSolr(from, length,new HashMap<>());
        }

        private ListItemIdentifiersResult getIdentifiersSolr(int from, int length,Map<String,String[]> searchCriterias){
            try {
                SearchToken token=new SearchToken();
                token.setFrom(from);
                token.setMaxResult(length);
                SortDefinition sort=new SortDefinition();
                sort.addSortDefinitionEntry(new SortDefinition.SortDefinitionEntry(CCConstants.CM_PROP_C_CREATED,true));
                token.setSortDefinition(sort);
                token.setContentType(SearchService.ContentType.FILES);
                SearchResultNodeRef result = SearchServiceFactory.getLocalService().searchV2(
                        MetadataHelper.getMetadataset(ApplicationInfoList.getHomeRepository(), CCConstants.metadatasetdefault_id),
                        "oai",
                        searchCriterias,
                        token);
                List<ItemIdentifier> refs = result.getData().stream().map((ref) -> new EduItemIdentifier(ref.getNodeId(),getDate(ref))).collect(Collectors.toList());
                logger.info(result.getNodeCount());
                int delivered=from+result.getData().size();
                return new ListItemIdentifiersResult(delivered<result.getNodeCount(),refs,result.getNodeCount());
            } catch (Throwable t) {
                logger.warn(t.getMessage(),t);
                return null;
            }
        }

        @Override
        public ListItemIdentifiersResult getIdentifiersFrom(int from, int length, Date date) {
            HashMap<String, String[]> criterias = new HashMap<>();
            criterias.put("from",new String[]{convertDateSolr(date)});
            return getIdentifiersSolr(from, length,criterias);
        }

        private String convertDateSolr(Date date) {
            return DateTimeFormatter.ISO_INSTANT.format(date.toInstant());
    }

        @Override
        public ListItemIdentifiersResult getIdentifiersUntil(int from, int length, Date date) {
            HashMap<String, String[]> criterias = new HashMap<>();
            criterias.put("until",new String[]{convertDateSolr(date)});
            return getIdentifiersSolr(from, length,criterias);        }

        @Override
        public ListItemIdentifiersResult getIdentifiersFromUntil(int from, int length, Date fromDate, Date untilDate) {
            HashMap<String, String[]> criterias = new HashMap<>();
            criterias.put("from",new String[]{convertDateSolr(fromDate)});
            criterias.put("until",new String[]{convertDateSolr(untilDate)});
            return getIdentifiersSolr(from, length,criterias);
        }
        private Date getDate(NodeRef ref) {
            return (Date) serviceRegistry.getNodeService().getProperty(new org.alfresco.service.cmr.repository.NodeRef(ref.getStoreProtocol(),ref.getStoreId(),ref.getNodeId()),QName.createQName(CCConstants.CM_PROP_C_MODIFIED));
        }

        @Override
        public EduItem getItem(String id) {
            try {
                ByteArrayOutputStream os=new ByteArrayOutputStream();
                new OAILOMExporter(id).write(os);
                return new EduItem(id,new String(os.toByteArray()));
            } catch (ParserConfigurationException e) {
                logger.warn(e.getMessage(),e);
                return null;
            }
        }
    }
}
