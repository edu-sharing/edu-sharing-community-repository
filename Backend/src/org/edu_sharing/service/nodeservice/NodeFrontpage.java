package org.edu_sharing.service.nodeservice;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.permissions.PermissionReference;
import org.alfresco.repo.security.permissions.impl.PermissionReferenceImpl;
import org.alfresco.repo.security.permissions.impl.model.PermissionModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.AccessPermission;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.namespace.QName;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.jobs.helper.NodeRunner;
import org.edu_sharing.repository.server.jobs.quartz.AbstractJob;
import org.edu_sharing.repository.server.jobs.quartz.UpdateFrontpageCacheJob;
import org.edu_sharing.service.admin.AdminServiceFactory;
import org.edu_sharing.service.admin.RepositoryConfigFactory;
import org.edu_sharing.service.admin.model.RepositoryConfig;
import org.edu_sharing.service.permission.PermissionService;
import org.edu_sharing.service.permission.PermissionServiceFactory;
import org.edu_sharing.service.rating.AccumulatedRatings;
import org.edu_sharing.service.rating.RatingService;
import org.edu_sharing.service.rating.RatingServiceFactory;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.service.stream.StreamServiceHelper;
import org.edu_sharing.service.tracking.TrackingService;
import org.edu_sharing.service.tracking.TrackingServiceFactory;
import org.edu_sharing.service.tracking.TrackingServiceImpl;
import org.edu_sharing.service.tracking.model.StatisticEntry;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

public class NodeFrontpage {
    private Logger logger= Logger.getLogger(NodeFrontpage.class);
    private static final String INDEX_NAME = "frontpage_cache";
    private static final String TYPE_NAME = "_doc";
    private SearchService searchService= SearchServiceFactory.getLocalService();
    private NodeService nodeService=NodeServiceFactory.getLocalService();
    private PermissionService permissionService= PermissionServiceFactory.getLocalService();
    private RestHighLevelClient client;
    private HashMap<String, Date> APPLY_DATES;
    public NodeFrontpage(){
        APPLY_DATES=new HashMap<>();
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -30);
        APPLY_DATES.put("days_30",calendar.getTime());
        calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -100);
        APPLY_DATES.put("days_100",calendar.getTime());
        // null means of all time
        APPLY_DATES.put("all",null);
        initElastic();
    }
    public void buildCache(AbstractJob job) {
        try {
            resetElastic();

            NodeRunner runner=new NodeRunner();
            runner.setRunAsSystem(true);
            runner.setTypes(Collections.singletonList(CCConstants.CCM_TYPE_IO));
            runner.setThreaded(true);
            runner.setInvalidateCache(false);
            runner.setTask((ref)->{
                try {
                    if (job.isInterrupted())
                        return;
                    // dummy task, only for test
                    /*AuthenticationUtil.runAsSystem(()->{
                        RatingServiceFactory.getLocalService().addOrUpdateRating(ref.getId(), (double) Math.round(Math.random()*4 + 1),"test");
                        TrackingServiceFactory.getTrackingService().trackActivityOnNode(ref,null, TrackingService.EventType.VIEW_MATERIAL);
                        TrackingServiceFactory.getTrackingService().trackActivityOnNode(ref,null, TrackingService.EventType.VIEW_MATERIAL_EMBEDDED);
                        TrackingServiceFactory.getTrackingService().trackActivityOnNode(ref,null, TrackingService.EventType.DOWNLOAD_MATERIAL);
                        return null;
                    });*/
                    XContentBuilder builder = jsonBuilder().startObject();

                    long time=System.currentTimeMillis();
                    addAuthorities(ref, builder);
                    logger.info("Authorities: "+(System.currentTimeMillis()-time)+" ms");
                    time=System.currentTimeMillis();
                    addNodeMetadata(ref, builder);
                    logger.info("Metadata: "+(System.currentTimeMillis()-time)+" ms");
                    time=System.currentTimeMillis();
                    addRatings(ref, builder);
                    logger.info("Ratings: "+(System.currentTimeMillis()-time)+" ms");
                    time=System.currentTimeMillis();
                    addTracking(ref, builder);
                    logger.info("Tracking: "+(System.currentTimeMillis()-time)+" ms");
                    time=System.currentTimeMillis();

                    builder.endObject();

                    IndexRequest indexRequest = new IndexRequest();
                    indexRequest.index(INDEX_NAME);
                    indexRequest.type(TYPE_NAME);
                    indexRequest.id(ref.getId());

                    indexRequest.source(builder);
                    IndexResponse result = client.index(indexRequest);
                    String id = result.getId();
                    logger.debug("added node cache for " + ref.getId() + " to elastic");
                }
                catch(Throwable t){
                    logger.warn(t.getMessage(),t);
                }
            });
            int size=runner.run();
            logger.info("Built up elastic frontpage cache for "+size+" nodes");
        } catch (Throwable e) {
            logger.warn(e.getMessage(),e);
        }
    }

    private void addAuthorities(NodeRef ref, XContentBuilder builder) throws IOException {
        ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
        ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext
                .getBean(ServiceRegistry.SERVICE_REGISTRY);
        Set<AccessPermission> permissions = serviceRegistry.getPermissionService().getAllSetPermissions(ref);
        PermissionModel permissionModel= (PermissionModel) applicationContext.getBean("permissionsModelDAO");
        builder.startArray("authorities");
        permissions.stream().
                filter((permission)->{
                    if(!permission.getAccessStatus().equals(AccessStatus.ALLOWED))
                        return false;

                    try {
                        // Get the subset of all permissions for the particular permission
                        Set<PermissionReference> subPermissions = permissionModel.getGranteePermissions(PermissionReferenceImpl.getPermissionReference(QName.createQName(CCConstants.NAMESPACE_CM, "content"), permission.getPermission()));
                        //filter if this permission includes the Read Permission
                        return subPermissions.stream().anyMatch((p) -> p.getName().equals(org.alfresco.service.cmr.security.PermissionService.READ));
                    }catch(Throwable t){
                        // unknown permission, ignore for now
                        return false;
                    }
                }).
                map(AccessPermission::getAuthority).
                collect(Collectors.toSet()).
                forEach((authority)->{
                    try {
                        builder.value(authority);
                    } catch (IOException e) {}
                });
        // add the creator / owner as well
        builder.value(serviceRegistry.getNodeService().getProperty(ref, ContentModel.PROP_CREATOR));
        builder.endArray();
    }

    private void addTracking(NodeRef ref, XContentBuilder builder) throws IOException {
        TrackingService trackingService = TrackingServiceFactory.getTrackingService();
        builder.startObject("actions");
        for(Map.Entry<String, Date> date : APPLY_DATES.entrySet()) {
            try {
                StatisticEntry entry = trackingService.getSingleNodeData(ref, date.getValue(), null);
                builder.startObject(date.getKey());
                for(Map.Entry<TrackingService.EventType, Integer> count : entry.getCounts().entrySet()){
                    builder.field(count.getKey().name(),count.getValue());
                }
                builder.endObject();
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
        builder.endObject();
    }
    private void addNodeMetadata(NodeRef ref, XContentBuilder builder){
        try {
            builder.field("type",NodeServiceHelper.getType(ref));
            builder.startObject("properties");
            for(Map.Entry<QName, Serializable> prop : NodeServiceHelper.getPropertiesNative(ref).entrySet()){
                builder.field(prop.getKey().toString(),prop.getValue());
            }
            builder.endObject();
            builder.startArray("aspects");
            for(String aspects : NodeServiceHelper.getAspects(ref)){
                builder.value(aspects);
            }
            builder.endArray();
        } catch (Throwable t) {
            logger.info(t.getMessage(),t);
        }
    }

    private void addRatings(NodeRef ref, XContentBuilder builder) throws IOException {
        RatingService ratingService = RatingServiceFactory.getLocalService();
        builder.startObject("ratings");
        for(Map.Entry<String, Date> date : APPLY_DATES.entrySet()) {
            builder.startObject(date.getKey());
            AccumulatedRatings rating = ratingService.getAccumulatedRatings(ref.getId(), date.getValue());
            builder.field("overall",rating.getOverall().getRating());
            if(rating.getAffiliation()!=null) {
                builder.startObject("affiliation");
                for (Map.Entry<String, AccumulatedRatings.RatingData> affiliation : rating.getAffiliation().entrySet()) {
                    builder.field(affiliation.getKey(), affiliation.getValue().getRating());
                }
                builder.endObject();
            }
            builder.endObject();
        }
        builder.endObject();
    }
    private void resetElastic(){
        try {
            client.indices().delete(new DeleteIndexRequest(INDEX_NAME));
        }catch(Exception e){
        }
        initElastic();
    }
    private void initElastic() {
        List<HttpHost> hosts = getConfiguredHosts();
        RestClientBuilder restClient = RestClient.builder(
                hosts.toArray(new HttpHost[0]));
        client = new RestHighLevelClient(restClient);
        try {
            if(!client.ping()){
                throw new Exception();
            }
        } catch (Exception e) {
            throw new RuntimeException("No Elasticsearch instance was found at "+hosts.get(0));
        }
        try {
            CreateIndexRequest  indexRequest = new CreateIndexRequest(INDEX_NAME);
            /*
            XContentBuilder builder = jsonBuilder().
                    startObject().
                    startObject(TYPE_NAME).
                    startObject("ratings");
            for(String label : APPLY_DATES.keySet()){
                builder.startObject(label).field("overall", "double").endObject();
            }
            builder.endObject().
                    endObject().
                    endObject();
            indexRequest.mapping(TYPE_NAME, builder);
            */

            client.indices().create(indexRequest);
        } catch (Exception e) {
            logger.info("Error while creating the frontpage index (ignore): "+e.getMessage());
        }
    }
    private List<HttpHost> getConfiguredHosts() {
        //@TODO make elastic server configurable
        List<HttpHost> hosts=null;
        try {
            String[] servers=null;
            if(servers==null) {
                servers=new String[] {"127.0.0.1:9200"};
            }
            hosts=new ArrayList<>();
            for(String server : servers) {
                hosts.add(new HttpHost(server.split(":")[0],Integer.parseInt(server.split(":")[1])));
            }
        }catch(Throwable t) {
        }
        return hosts;
    }

    public Collection<NodeRef> getNodesForCurrentUserAndConfig() throws Throwable {
        RepositoryConfig.Frontpage config = RepositoryConfigFactory.getConfig().frontpage;
        //@TODO read and apply config
        BoolQueryBuilder query = QueryBuilders.boolQuery();
        BoolQueryBuilder audience = QueryBuilders.boolQuery();
        audience.minimumShouldMatch(1);
        for(String a : StreamServiceHelper.getCurrentAuthorities()) {
            audience.should(QueryBuilders.matchQuery("authorities", a));
        }
        query.must(audience);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(query);
        if(config.mode.equals(RepositoryConfig.Frontpage.Mode.rating)){
            searchSourceBuilder.sort("ratings."+config.timespan.name()+".overall", SortOrder.DESC);
        }
        else if(config.mode.equals(RepositoryConfig.Frontpage.Mode.downloads)){
            searchSourceBuilder.sort("actions."+config.timespan.name()+"."+ TrackingService.EventType.DOWNLOAD_MATERIAL.name(), SortOrder.DESC);
        }
        else{
            searchSourceBuilder.sort("actions."+config.timespan.name()+"."+ TrackingService.EventType.VIEW_MATERIAL.name(), SortOrder.DESC);
        }
        searchSourceBuilder.size(config.totalCount);
        searchSourceBuilder.from(0);
        SearchRequest searchRequest = new SearchRequest().source(searchSourceBuilder);
        searchRequest.indices(INDEX_NAME);
        SearchResponse searchResult = client.search(searchRequest);
        List<NodeRef> result=new ArrayList<>();
        for(SearchHit hit : searchResult.getHits().getHits()){
            if(permissionService.hasPermission(StoreRef.PROTOCOL_WORKSPACE,StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(),hit.getId(),CCConstants.PERMISSION_READ)){
                result.add(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,hit.getId()));
            }
        }
        if(config.displayCount<config.totalCount) {
            Set<NodeRef> randoms = new HashSet<>();
            // grab a random count of elements (equals displayCount) of the whole array
            while (randoms.size() < config.displayCount) {
                randoms.add(result.get(new Random().nextInt(result.size())));
            }
            return randoms;
        }
        return result;
    }
}
