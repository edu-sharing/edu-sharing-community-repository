package org.edu_sharing.service.stream;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.http.HttpHost;
import org.apache.lucene.search.join.ScoreMode;
import org.edu_sharing.repository.server.tools.PropertiesHelper;
import org.edu_sharing.service.search.model.SortDefinition;
import org.edu_sharing.service.stream.model.ContentEntry;
import org.edu_sharing.service.stream.model.ContentEntry.Audience.STATUS;
import org.edu_sharing.service.stream.model.ScoreResult;
import org.edu_sharing.service.stream.model.StreamSearchRequest;
import org.edu_sharing.service.stream.model.StreamSearchResult;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.sum.ParsedSum;
import org.elasticsearch.search.aggregations.metrics.sum.SumAggregationBuilder;
import org.elasticsearch.search.aggregations.support.ValueType;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;

public class StreamServiceElasticsearchImpl implements StreamService {
	public static class ContentEntryConverter{
		public static ContentEntry fromSourceMap(Map<String, Object> source,String id) {
			ContentEntry entry=new ContentEntry();
			entry.id=id;
			entry.title=(String) source.get("title");
			entry.description=(String) source.get("description");
			entry.priority=(Number) source.get("priority");
			entry.nodeId=(List<String>)source.get("nodeId");
			entry.properties=(Map<String,Object>)source.get("properties");
			entry.created=(Number) source.get("created");
			entry.modified=(Number) source.get("modified");
			entry.author=(String) source.get("author");
			List<Map<String, Object>> audienceMap = (List<Map<String,Object>>) source.get("audience");
			entry.audience=new ArrayList<ContentEntry.Audience>(audienceMap.size());
			for(Map<String, Object> mapEntry : audienceMap) {
				ContentEntry.Audience audienceEntry=new ContentEntry.Audience();
				audienceEntry.authority=(String) mapEntry.get("authority");
				audienceEntry.status=(ContentEntry.Audience.STATUS) ContentEntry.Audience.STATUS.valueOf((String) mapEntry.get("status"));
				entry.audience.add(audienceEntry);
			}
			return entry;
		}

		public static XContentBuilder toContentBuilder(ContentEntry entry) throws IOException {
			XContentBuilder builder = jsonBuilder().startObject()
					.field("priority",entry.priority)
					.field("nodeId",entry.nodeId)
					.field("properties", entry.properties)
					.field("title", entry.title)
					.field("description", entry.description)
					.field("created",entry.created)
					.field("modified",entry.modified)
					.field("author",entry.author)
					.startArray("audience");
			if(entry.audience!=null) {
					for(ContentEntry.Audience audience : entry.audience) {
						builder.startObject()
						.field("authority",audience.authority)
						.field("status",audience.status)
						.endObject();
					}
			}
					builder.endArray()
					.endObject();
			return builder;
		}


	}
	private static String PROPERTY_XML = "/org/edu_sharing/service/stream/stream.elasticsearch.properties";
	public StreamServiceElasticsearchImpl() {
		if(client!=null)
			return;
		List<HttpHost> hosts = getConfiguredHosts();
		RestClientBuilder restClient = RestClient.builder(
				hosts.toArray(new HttpHost[0]));
		client=new RestHighLevelClient(restClient);
		try {
			CreateIndexRequest  indexRequest = new CreateIndexRequest(INDEX_NAME);
			indexRequest.mapping(TYPE_NAME, jsonBuilder().
					startObject().
					startObject(TYPE_NAME).
					startObject("properties").
					startObject("created").field("type", "date").endObject().
					startObject("modified").field("type", "date").endObject().
					startObject("title").field("type", "keyword").endObject().
					startObject("description").field("type", "text").endObject().
					startObject("priority").field("type", "integer").endObject().
					startObject("audience").field("type", "nested").
					startObject("properties").
					startObject("authority").field("type","keyword").endObject().
					startObject("status").field("type","keyword").endObject().
					endObject().
					endObject().
					endObject().
					endObject().
					endObject()
			);
			client.indices().create(indexRequest);
		}catch(Exception e) {
			// index already exists
			// throw new RuntimeException("Elastic search init failed",e);
		}
	}

	private List<HttpHost> getConfiguredHosts() {
		List<HttpHost> hosts=null;
		try {
			String[] servers=null;
			String prop=null;
			try {
				prop=PropertiesHelper.getProperty("server",PROPERTY_XML,PropertiesHelper.TEXT);
			}
			catch(Exception e) {
			}			
			if(prop!=null)
				servers=prop.split(",");				
			if(servers==null) {
				servers=new String[] {"127.0.0.1:9200"};
			}
			hosts=new ArrayList<>();
			for(String server : servers) {
				hosts.add(new HttpHost(server.split(":")[0],Integer.parseInt(server.split(":")[1])));		
			}
		}catch(Throwable t) {
			throw new IllegalArgumentException("Parameter server in "+PROPERTY_XML+" seems invalid. Scheme: server-ip1:server-port1,server-ip2:server-port2...",t);
		}
		return hosts;
	}
	private static String INDEX_NAME="entry_index22";
	private static String TYPE_NAME="entry";
	private static TimeValue SCROLL_TIME=TimeValue.timeValueMinutes(1);
	private static RestHighLevelClient client;

	@Override
	public String addEntry(ContentEntry entry) throws Exception {
		IndexRequest indexRequest = new IndexRequest();
		indexRequest.index(INDEX_NAME);
		indexRequest.type(TYPE_NAME);
		
		indexRequest.source(ContentEntryConverter.toContentBuilder(entry));
		IndexResponse result = client.index(indexRequest);
		return result.getId();
	}
	@Override
	public void updateEntry(ContentEntry entry) throws Exception {
		UpdateRequest updateRequest = new UpdateRequest();
		updateRequest.index(INDEX_NAME);
		updateRequest.type(TYPE_NAME);

		updateRequest.doc(ContentEntryConverter.toContentBuilder(entry));
		client.update(updateRequest);
	}
	public StreamSearchResult searchScroll(String scrollId) throws Exception {
		SearchScrollRequest searchScrollRequest = new SearchScrollRequest(scrollId);
		searchScrollRequest.scroll(SCROLL_TIME);
		SearchResponse searchResult = client.searchScroll(searchScrollRequest);
		return responseToStreamResult(searchResult);
	}
	@Override
	public boolean canAccessNode(List<String> authorities,String nodeId) throws Exception {
		BoolQueryBuilder query = getAuthorityQuery(authorities, null);
		query = query.must(QueryBuilders.matchQuery("nodeId.keyword", nodeId));
		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
		searchSourceBuilder.query(query);
		searchSourceBuilder.size(0);
		SearchRequest request = new SearchRequest().source(searchSourceBuilder);
        request.indices(INDEX_NAME);
		SearchResponse searchResult = client.search(request);
		return searchResult.getHits().totalHits>0; 
	}
	private BoolQueryBuilder getAuthorityQuery(List<String> authorities,ContentEntry.Audience.STATUS status) {
		BoolQueryBuilder query = QueryBuilders.boolQuery();
	
		query.minimumShouldMatch(1);
		BoolQueryBuilder exclude = QueryBuilders.boolQuery();
		exclude.minimumShouldMatch(1);
		for(String a : authorities) {
			BoolQueryBuilder bool = QueryBuilders.boolQuery().					
					must(QueryBuilders.matchQuery("audience.authority", a));
			if(status!=null)
				bool=bool.must(QueryBuilders.matchQuery("audience.status", status));
			query=query.should(bool);
		}
		for(STATUS type : ContentEntry.Audience.STATUS.values()) {
			if(type.equals(status))
					continue;
				exclude=exclude.should(
						QueryBuilders.boolQuery().
						must(QueryBuilders.matchQuery("audience.authority", authorities.get(0))).
						must(QueryBuilders.matchQuery("audience.status", type))
						);
		}
			
		BoolQueryBuilder main=QueryBuilders.boolQuery().
					must(QueryBuilders.nestedQuery("audience",query, ScoreMode.None));
		if(status!=null) {
			main=main.mustNot(QueryBuilders.nestedQuery("audience",exclude, ScoreMode.None));
		}
				
		return main;
	}
	@Override
	public ContentEntry.Audience.STATUS getStatus(String entryId,List<String> authorities) throws Exception {
		List<Map<String,Object>> audience = (List<Map<String, Object>>) getEntryRequest(entryId).getSource().get("audience");
		for(String a : authorities) {
			for(Map<String,Object> entry : audience) {
				if(entry.get("authority").equals(a)) {
					return ContentEntry.Audience.STATUS.valueOf((String) entry.get("status")); 
				}
			}
		}
		return null;
	}
	@Override
	public ContentEntry getEntry(String entryId) throws Exception {
		return ContentEntryConverter.fromSourceMap(getEntryRequest(entryId).getSource(),entryId);
	}
	private GetResponse getEntryRequest(String entryId) throws IOException {
		GetRequest request=new GetRequest(INDEX_NAME, TYPE_NAME, entryId);
		return client.get(request);
	}
	@Override
	public ScoreResult getScoreByAuthority(String authority,ContentEntry.Audience.STATUS status) throws Exception {
		QueryBuilder nested=getAuthorityQuery(Arrays.asList(new String[] {authority}),status);
		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
		searchSourceBuilder.query(nested);
		SumAggregationBuilder aggregation = AggregationBuilders.sum("score").field("score");
		searchSourceBuilder.aggregation(aggregation);     
		searchSourceBuilder.size(0);
		SearchRequest request = new SearchRequest().source(searchSourceBuilder);
        request.indices(INDEX_NAME);
		SearchResponse searchResult = client.search(request);
		ScoreResult result=new ScoreResult();
		result.score=(long) ((ParsedSum)searchResult.getAggregations().get("score")).getValue();
		return result;
	}
	@Override
	public void delete(String id) throws Exception {
		DeleteRequest request = new DeleteRequest();
		request.index(INDEX_NAME);
		request.type(TYPE_NAME);
		request.id(id);
		client.delete(request);
	}

	@Override
	public void deleteEntriesByAuthority(String username) {
		//@TODO this is not supported in the current api version
		// we need to update the api
		// https://www.elastic.co/guide/en/elasticsearch/client/java-rest/6.5/java-rest-high-document-delete-by-query.html
		//DeleteByQueryRequest request = new DeleteByQueryRequest();
		//client.delete(request);
		throw new NotImplementedException("deleteEntriesByAuthority");
	}

	@Override
	public void updateStatus(String id,String authority,ContentEntry.Audience.STATUS status) throws Exception {
		UpdateRequest updateRequest = new UpdateRequest();
		updateRequest.index(INDEX_NAME);
		updateRequest.type(TYPE_NAME);
		updateRequest.id(id);
		String script="boolean updated=false; if(ctx._source.audience == null) {ctx._source.audience=new ArrayList();}  for (item in ctx._source.audience) {if (item!=null && item.authority == params.audience.authority) {item.status = params.audience.status;updated=true;}} if(!updated){ctx._source.audience.add(params.audience);}";
		//script="boolean updated=false; if(ctx._source.audience == null) {ctx._source.audience=new ArrayList();}  if(!updated){ctx._source.audience.add(params.audience.authority+'='+params.audience.status);}";
		Map<String, Object> scriptParams = new HashMap<String,Object>();
		Map<String, Object> audience = new HashMap<String,Object>();
		audience.put("authority", authority);
		audience.put("status", status);		
		scriptParams.put("audience", audience);
		Script scriptObj = new Script(ScriptType.INLINE,Script.DEFAULT_SCRIPT_LANG,script,scriptParams);
		updateRequest.script(scriptObj);

		client.update(updateRequest);
	}
	@Override
	public StreamSearchResult search(StreamSearchRequest request) throws Exception {
		if(request.authority==null) {
			throw new IllegalArgumentException("StreamSearchRequest is missing authority");
		}
		/*if(request.status==null) {
			throw new IllegalArgumentException("StreamSearchRequest is missing status");
		}*/
		
		
		/*NestedQueryBuilder nested = QueryBuilders.nestedQuery(
				"audience",boolQuery,
				ScoreMode.None
				);*/
		BoolQueryBuilder query = QueryBuilders.boolQuery();
		query.must(getAuthorityQuery(request.authority, request.status));
		if(request.properties!=null) {
			for(String key : request.properties.keySet()) {
				query.must(QueryBuilders.matchQuery("properties."+key, request.properties.get(key)));
			}
		}
		if(request.search!=null)
			query.must(QueryBuilders.multiMatchQuery(request.search,"description","title"));
		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
		searchSourceBuilder.query(query);
		if(request.sortDefinition!=null && request.sortDefinition.hasContent()){
			for(SortDefinition.SortDefinitionEntry sort : request.sortDefinition.getSortDefinitionEntries()){
				searchSourceBuilder.sort(sort.getProperty(),sort.isAscending() ? SortOrder.ASC : SortOrder.DESC);
			}
		}
		else {
			searchSourceBuilder.sort("priority", SortOrder.DESC);
			searchSourceBuilder.sort("created", SortOrder.DESC);
		}
		searchSourceBuilder.size(request.size);
		searchSourceBuilder.from(request.offset);
        SearchRequest searchRequest = new SearchRequest().source(searchSourceBuilder);
        //searchRequest.scroll(SCROLL_TIME);
        searchRequest.indices(INDEX_NAME);
		SearchResponse searchResult = client.search(searchRequest);
		return responseToStreamResult(searchResult);
	}
	private static StreamSearchResult responseToStreamResult(SearchResponse searchResult) {
		List<ContentEntry> list=new ArrayList<ContentEntry>();
		for(SearchHit data : searchResult.getHits()) {
			list.add(ContentEntryConverter.fromSourceMap(data.getSourceAsMap(),data.getId()));
		}
		StreamSearchResult result=new StreamSearchResult();
		result.scrollId=searchResult.getScrollId();
		result.results=list;
		result.total=searchResult.getHits().totalHits;
		return result;
	}
	@Override
	public Map<String, Number> getTopValues(String property) throws Exception {
		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
		searchSourceBuilder.size(0);
		TermsAggregationBuilder aggregation = AggregationBuilders.terms("agg").field("properties."+property+".keyword").valueType(ValueType.STRING);
		searchSourceBuilder.aggregation(aggregation);
        SearchRequest request = new SearchRequest().source(searchSourceBuilder);
        request.indices(INDEX_NAME);
        List<? extends Bucket> buckets = ((ParsedTerms) client.search(request).getAggregations().get("agg")).getBuckets();
        Map<String, Number> result = new HashMap<>(buckets.size());
        for(Bucket bucket : buckets) {
        	result.put(bucket.getKeyAsString(), bucket.getDocCount());
        }
        return result;
	}
}
