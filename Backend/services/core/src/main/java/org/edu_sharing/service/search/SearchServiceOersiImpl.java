package org.edu_sharing.service.search;

import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.metadataset.v2.MetadataSet;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.forms.VCardTool;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.model.NodeRef;
import org.edu_sharing.service.search.model.SearchToken;

import java.io.Closeable;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchServiceOersiImpl extends SearchServiceAdapter {

  private static final Logger logger = Logger.getLogger(SearchServiceOersiImpl.class);

  private static final String OERSI_PROPERTY_ABOUT = "about";
  private static final String OERSI_PROPERTY_ABOUT_ID = "id";
  private static final String OERSI_PROPERTY_CREATOR = "creator";
  private static final String OERSI_PROPERTY_CREATOR_NAME = "name";
  private static final String OERSI_PROPERTY_DATE_CREATED = "dateCreated";
  private static final String OERSI_PROPERTY_DATE_PUBLISHED = "datePublished";
  private static final String OERSI_PROPERTY_DESCRIPTION = "description";
  private static final String OERSI_PROPERTY_ID = "id";
  private static final String OERSI_PROPERTY_IMAGE = "image";
  private static final String OERSI_PROPERTY_INLANGUAGE = "inLanguage";
  private static final String OERSI_PROPERTY_KEYWORDS = "keywords";
  private static final String OERSI_PROPERTY_LICENSE = "license";
  private static final String OERSI_PROPERTY_LICENSE_ID = "id";
  private static final String OERSI_PROPERTY_LRT = "learningResourceType";
  private static final String OERSI_PROPERTY_LRT_ID = "id";
  private static final String OERSI_PROPERTY_MAIN_ENTITY_OF_PAGE = "mainEntityOfPage";
  private static final String OERSI_PROPERTY_MAIN_ENTITY_OF_PAGE_PROVIDER = "provider";
  private static final String OERSI_PROPERTY_MAIN_ENTITY_OF_PAGE_PROVIDER_NAME = "name";
  private static final String OERSI_PROPERTY_NAME = "name";
  private static final String OERSI_PROPERTY_SOURCE_ORGANIZATION = "sourceOrganization";
  private static final String OERSI_PROPERTY_SOURCE_ORGANIZATION_NAME = "name";

  private final String oersiHost;
  private final int oersiPort;
  private final String oersiScheme;
  private final String oersiPathPrefix;
  private final String oersiIndex;

  String repositoryId = null;

  public SearchServiceOersiImpl(String appId) {
    ApplicationInfo appInfo = ApplicationInfoList.getRepositoryInfoById(appId);
    this.repositoryId = appInfo.getAppId();
    this.oersiHost = appInfo.getString(ApplicationInfo.KEY_HOST, "oersi.de");
    this.oersiPort = Integer.parseInt(appInfo.getString(ApplicationInfo.KEY_PORT, "443"));
    this.oersiScheme = appInfo.getString(ApplicationInfo.KEY_PROTOCOL, "https");
    this.oersiPathPrefix = appInfo.getString("pathprefix", "/resources/api-internal/search");
    this.oersiIndex = appInfo.getString("index", "oer_data");
  }

  public Map<String, Object> retrieveNode(String nodeId) throws Exception {
    try (OersiQueryExecutor queryExecutor = queryExecutor()) {
      return queryExecutor.executeRetrieveById(nodeId);
    }
  }

  @Override
  public List<? extends Suggestion> getSuggestions(MetadataSet mds, String queryId, String parameterId, String value, List<org.edu_sharing.restservices.shared.MdsQueryCriteria> criteria) {
    // TODO
    return new ArrayList<>();
  }

  @Override
  public SearchResultNodeRef search(MetadataSet mds, String query, Map<String, String[]> criteria,
                                    SearchToken searchToken) throws Throwable {
    OersiSearchResult result;
    try (OersiQueryExecutor queryExecutor = queryExecutor()) {
      result = queryExecutor.executeSearch(mds, query, criteria, searchToken.getFrom(), searchToken.getMaxResult());
    }

    SearchResultNodeRef searchResultNodeRef = new SearchResultNodeRef();
    List<NodeRef> data = new ArrayList<>();
    for (Map<String, Object> properties : result.records) {
      NodeRef ref = new org.edu_sharing.service.model.NodeRefImpl(repositoryId,
        StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getProtocol(),
        StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), new HashMap<>(properties));
      data.add(ref);
    }
    searchResultNodeRef.setStartIDX(searchToken.getFrom());
    searchResultNodeRef.setData(data);
    searchResultNodeRef.setNodeCount((int) result.total);
    return searchResultNodeRef;
  }

  private OersiQueryExecutor queryExecutor() {
    OersiQueryExecutor queryExecutor = new OersiElasticsearchQueryExecutor();
    queryExecutor.endpoint(oersiHost, oersiPort, oersiScheme, oersiPathPrefix, oersiIndex);
    return queryExecutor;
  }

  public Map<String, Object> convertOersiRecordToProperties(String oersiId, Map<String, Object> oersiProperties) {
    HashMap<String, Object> properties = new HashMap<>();
    properties.put(CCConstants.SYS_PROP_NODE_UID, oersiId);
    String title = (String) oersiProperties.get(OERSI_PROPERTY_NAME);
    properties.put(CCConstants.LOM_PROP_GENERAL_TITLE, title);
    String name = title.replaceAll(ApplicationInfoList.getHomeRepository().getValidatorRegexCMName(), "_").trim();
    properties.put(CCConstants.CM_NAME, name);
    mapOersiLicense(properties, oersiProperties);
    mapOersiString(properties, oersiProperties, OERSI_PROPERTY_ID, CCConstants.CCM_PROP_IO_WWWURL);
    mapOersiString(properties, oersiProperties, OERSI_PROPERTY_DESCRIPTION, CCConstants.LOM_PROP_GENERAL_DESCRIPTION);
    mapOersiObjectArray(properties, oersiProperties, OERSI_PROPERTY_CREATOR, CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_AUTHOR, o -> VCardTool.nameToVCard((String) ((Map<String, Object>) o).get(OERSI_PROPERTY_CREATOR_NAME)));
    mapOersiObjectArray(properties, oersiProperties, OERSI_PROPERTY_SOURCE_ORGANIZATION, CCConstants.getValidGlobalName("ccm:sourceOrganization"), o -> (String) ((Map<String, Object>) o).get(OERSI_PROPERTY_SOURCE_ORGANIZATION_NAME));
    mapOersiObjectArray(properties, oersiProperties, OERSI_PROPERTY_ABOUT, CCConstants.CCM_PROP_IO_REPL_TAXON_ID, o -> (String) ((Map<String, Object>) o).get(OERSI_PROPERTY_ABOUT_ID));
    mapOersiObjectArray(properties, oersiProperties, OERSI_PROPERTY_LRT, CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_LEARNINGRESSOURCETYPE, o -> (String) ((Map<String, Object>) o).get(OERSI_PROPERTY_LRT_ID));
    mapOersiStringArray(properties, oersiProperties, OERSI_PROPERTY_KEYWORDS, CCConstants.LOM_PROP_GENERAL_KEYWORD);
    mapOersiStringArray(properties, oersiProperties, OERSI_PROPERTY_INLANGUAGE, CCConstants.LOM_PROP_GENERAL_LANGUAGE);
    mapOersiString(properties, oersiProperties, OERSI_PROPERTY_IMAGE, CCConstants.CCM_PROP_IO_THUMBNAILURL);
    mapOersiString(properties, oersiProperties, OERSI_PROPERTY_IMAGE, CCConstants.CM_ASSOC_THUMBNAILS);
    properties.put(CCConstants.CCM_PROP_IO_REPLICATIONSOURCE, "OERSI");
    properties.put(CCConstants.CCM_PROP_IO_REPLICATIONSOURCEID, oersiProperties.get(OERSI_PROPERTY_ID));
    properties.put(CCConstants.LOM_PROP_TECHNICAL_LOCATION, oersiScheme + "://" + oersiHost + "/resources/" + oersiId);

    SimpleDateFormat sdfOersi = new SimpleDateFormat("yyyy-MM-dd");
    properties.put(CCConstants.CCM_PROP_IO_REPLICATIONSOURCETIMESTAMP, new Date().getTime());
    mapOersiDate(properties, oersiProperties, OERSI_PROPERTY_DATE_PUBLISHED, CCConstants.CCM_PROP_IO_PUBLISHED_DATE, sdfOersi);
    mapOersiDate(properties, oersiProperties, OERSI_PROPERTY_DATE_CREATED, CCConstants.CM_PROP_C_CREATED, sdfOersi);

    List<String> provider = getArrayValues(oersiProperties, OERSI_PROPERTY_MAIN_ENTITY_OF_PAGE,
      o -> (String) ((Map<String, Object>) ((Map<String, Object>) o).get(OERSI_PROPERTY_MAIN_ENTITY_OF_PAGE_PROVIDER)).get(OERSI_PROPERTY_MAIN_ENTITY_OF_PAGE_PROVIDER_NAME)
    );
    if (provider == null) {
      provider = new ArrayList<>();
    }
    provider.add(0, "OERSI");
    properties.put(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_CONTENT_PROVIDER, String.join(CCConstants.MULTIVALUE_SEPARATOR, provider));
    return properties;
  }

  private static void mapOersiString(Map<String, Object> eduProperties, Map<String, Object> oersiProperties, String sourceFieldName, String targetFieldName) {
    Object value = oersiProperties.get(sourceFieldName);
    if (value != null && value instanceof String && StringUtils.isNotEmpty((String) value)) {
      eduProperties.put(targetFieldName, value);
    }
  }
  private static void mapOersiDate(Map<String, Object> eduProperties, Map<String, Object> oersiProperties, String sourceFieldName, String targetFieldName, SimpleDateFormat sdfOersi) {
    Object value = oersiProperties.get(sourceFieldName);
    if (value instanceof String && StringUtils.isNotEmpty((String) value)) {
      try {
        Date date = sdfOersi.parse((String) value);
        eduProperties.put(targetFieldName, date.getTime());
      } catch (ParseException e) {
        logger.debug("Cannot parse OERSI date", e);
      }
    }
  }
  /**
   * map license from JSON to edu-sharing-properties
   */
  private static void mapOersiLicense(Map<String, Object> eduProperties, Map<String, Object> oersiProperties) {
    if (oersiProperties.containsKey(OERSI_PROPERTY_LICENSE)) {
      String licenseKey = null;
      Map<String, Object> license = (Map<String, Object>) oersiProperties.get(OERSI_PROPERTY_LICENSE);
      String licenseUrl = (String) license.get(OERSI_PROPERTY_LICENSE_ID);
      if (licenseUrl.matches("https?:\\/\\/creativecommons.org\\/(?:licenses|licences)\\/by\\/.*")) {
        licenseKey = CCConstants.COMMON_LICENSE_CC_BY;
      } else if (licenseUrl.matches("https?:\\/\\/creativecommons.org\\/(?:licenses|licences|publicdomain)\\/zero\\/1.0.*")) {
        licenseKey = CCConstants.COMMON_LICENSE_CC_ZERO;
      } else if (licenseUrl.matches("https?:\\/\\/creativecommons.org\\/(?:licenses|licences)\\/by-sa\\/.*")) {
        licenseKey = CCConstants.COMMON_LICENSE_CC_BY_SA;
      } else if (licenseUrl.matches("https?:\\/\\/creativecommons.org\\/(?:licenses|licences)\\/by-nc\\/.*")) {
        licenseKey = CCConstants.COMMON_LICENSE_CC_BY_NC;
      } else if (licenseUrl.matches("https?:\\/\\/creativecommons.org\\/(?:licenses|licences)\\/by-nd\\/.*")) {
        licenseKey = CCConstants.COMMON_LICENSE_CC_BY_ND;
      } else if (licenseUrl.matches("https?:\\/\\/creativecommons.org\\/(?:licenses|licences)\\/by-nc-nd\\/.*")) {
        licenseKey = CCConstants.COMMON_LICENSE_CC_BY_NC_ND;
      } else if (licenseUrl.matches("https?:\\/\\/creativecommons.org\\/(?:licenses|licences)\\/by-nc-sa\\/.*")) {
        licenseKey = CCConstants.COMMON_LICENSE_CC_BY_NC_SA;
      } else if (licenseUrl.matches("https?:\\/\\/creativecommons.org\\/(?:licenses|licences|publicdomain)\\/mark\\/1.0.*")) {
        licenseKey = CCConstants.COMMON_LICENSE_PDM;
      }
      Matcher versionMatcher = Pattern.compile("https?:\\/\\/creativecommons.org\\/(?:licenses|licences|publicdomain)\\/(?:[a-zA-Z-]+)\\/([0-9.]+)(\\/.*)?").matcher(licenseUrl);
      if (versionMatcher.matches()) {
        String licenseVersion = versionMatcher.group(1);
        eduProperties.put(CCConstants.CCM_PROP_IO_COMMONLICENSE_CC_VERSION, licenseVersion);
      }
      Matcher countryMatcher = Pattern.compile("https?:\\/\\/creativecommons.org\\/(?:licenses|licences|publicdomain)\\/(?:[a-zA-Z-]+)\\/(?:[0-9.]+)\\/([a-z][a-z])(\\/.*)?").matcher(licenseUrl);
      if (countryMatcher.matches()) {
        String countryCode = countryMatcher.group(1).toUpperCase();
        eduProperties.put(CCConstants.CCM_PROP_IO_COMMONLICENSE_CC_LOCALE, countryCode);
      }
      if (licenseKey != null) {
        eduProperties.put(CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY, licenseKey);
        eduProperties.put(CCConstants.VIRT_PROP_LICENSE_URL, licenseUrl);
      }
    }
  }

  private interface JsonObjectAccessor {
    String getValue(Object object);
  }
  private static void mapOersiStringArray(Map<String, Object> eduProperties, Map<String, Object> oersiProperties, String sourceFieldName, String targetFieldName) {
    mapOersiObjectArray(eduProperties, oersiProperties, sourceFieldName, targetFieldName, String.class::cast);
  }
  private static void mapOersiObjectArray(Map<String, Object> eduProperties, Map<String, Object> oersiProperties, String sourceFieldName, String targetFieldName, JsonObjectAccessor accessor) {
    List<String> valueList = getArrayValues(oersiProperties, sourceFieldName, accessor);
    if (valueList != null && !valueList.isEmpty()) {
      eduProperties.put(targetFieldName, String.join(CCConstants.MULTIVALUE_SEPARATOR, valueList));
    }
  }
  private static List<String> getArrayValues(Map<String, Object> oersiProperties, String sourceFieldName, JsonObjectAccessor accessor) {
    List<String> valueList = null;
    Object value = oersiProperties.get(sourceFieldName);
    if (value instanceof Collection) {
      valueList = new ArrayList<>();
      for (Object entry : (Collection) value) {
        valueList.add(accessor.getValue(entry));
      }
    }
    return valueList;
  }

  private class OersiSearchResult {
    private long total;
    private List<Map<String, Object>> records;
  }

  private class OersiAccessException extends Exception {
    public OersiAccessException(String message, Exception e) {
      super(message, e);
    }
  }

  // note: QueryStringExecuter has problems because the query is restricted by the length of the URL -> better use ElasticsearchQuery
  private interface OersiQueryExecutor extends Closeable {
    /**
     * OERSI API endpoint
     */
    void endpoint(String host, int port, String scheme, String pathPrefix, String index);

    OersiSearchResult executeSearch(MetadataSet mds, String query, Map<String, String[]> criteria, int from, int size) throws OersiAccessException;

    Map<String, Object> executeRetrieveById(String oersiId) throws OersiAccessException;
  }


  // TODO elastic search migration to 8.11.1
  private class OersiElasticsearchQueryExecutor implements OersiQueryExecutor {

    @Override
    public void endpoint(String host, int port, String scheme, String pathPrefix, String index) {
      throw new NotImplementedException();
    }

    @Override
    public OersiSearchResult executeSearch(MetadataSet mds, String query, Map<String, String[]> criteria, int from, int size) throws OersiAccessException {
      throw new NotImplementedException();
    }

    @Override
    public Map<String, Object> executeRetrieveById(String oersiId) throws OersiAccessException {
      throw new NotImplementedException();
    }

    @Override
    public void close() throws IOException {
      throw new NotImplementedException();
    }
  }
  /*
  private class OersiElasticsearchQueryExecutor implements OersiQueryExecutor {

    //private RestHighLevelClient client = null;
    private String index;

    @Override
    public void endpoint(String host, int port, String scheme, String pathPrefix, String index) {
      this.client = new RestHighLevelClient(RestClient.builder(new HttpHost(host, port, scheme)).setPathPrefix(pathPrefix));
      this.index = index;
    }
    @Override
    public void close() throws IOException {
      if (client != null) {
        client.close();
      }
    }

    @Override
    public OersiSearchResult executeSearch(MetadataSet mds, String query, Map<String, String[]> criteria, int from, int size) throws OersiAccessException {
      QueryBuilder queryBuilder = getQuery(mds, query, criteria);
      logger.debug("es query: " + queryBuilder);

      SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
      searchSourceBuilder.query(queryBuilder);
      searchSourceBuilder.from(from);
      searchSourceBuilder.size(size);
      searchSourceBuilder.trackTotalHits(true);

      SearchRequest searchRequest = new SearchRequest(index);
      searchRequest.source(searchSourceBuilder);
      try {
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        SearchHits hits = searchResponse.getHits();
        OersiSearchResult searchResult = new OersiSearchResult();
        searchResult.total = hits.getTotalHits().value;
        searchResult.records = new ArrayList<>();
        for (SearchHit hit : hits) {
          String oersiId = hit.getId();
          Map<String, Object> oersiProperties = hit.getSourceAsMap();
          Map<String, Object> properties = convertOersiRecordToProperties(oersiId, oersiProperties);
          searchResult.records.add(properties);
        }
        return searchResult;
      } catch (IOException e) {
        throw new OersiAccessException("Cannot access OERSI", e);
      }
    }

    private QueryBuilder getQuery(MetadataSet mds, String query, Map<String, String[]> criteria) {
      MetadataQuery queryData;
      try {
        queryData = mds.findQuery(query, MetadataReader.QUERY_SYNTAX_DSL);
      } catch (IllegalArgumentException e) {
        logger.info("Query " + query + " is not defined within dsl language, switching to default query...");
        return getDefaultQuery(criteria);
      }
      try {
        return MetadataElasticSearchHelper.getElasticSearchQuery(null,mds.getQueries(MetadataReader.QUERY_SYNTAX_DSL), queryData, criteria);
      } catch (Throwable e) {
        logger.info("Cannot get elasticsearch query, switching to default query... ", e);
        return getDefaultQuery(criteria);
      }
    }
    private QueryBuilder getDefaultQuery(Map<String, String[]> criteria) {
      BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
      for (Map.Entry<String, String[]> entry : criteria.entrySet()) {
        String joinedValues = String.join(" ", entry.getValue());
        if (joinedValues.trim().length() == 0) {
          continue;
        }
        if (MetadataSet.DEFAULT_CLIENT_QUERY_CRITERIA.equals(entry.getKey())) {
          queryBuilder.must(QueryBuilders.multiMatchQuery(joinedValues)
            .field(OERSI_PROPERTY_NAME)
            .field(OERSI_PROPERTY_DESCRIPTION)
            .field(OERSI_PROPERTY_KEYWORDS)
            .field(OERSI_PROPERTY_CREATOR + "." + OERSI_PROPERTY_CREATOR_NAME)
          );
        } else if (CCConstants.getValidLocalName(CCConstants.LOM_PROP_GENERAL_KEYWORD).equals(entry.getKey())) {
          queryBuilder.must(getFieldQuery(OERSI_PROPERTY_KEYWORDS, entry.getValue()));
        } else if (CCConstants.getValidLocalName(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_LEARNINGRESSOURCETYPE).equals(entry.getKey())) {
          queryBuilder.must(getFieldQuery(OERSI_PROPERTY_LRT + "." + OERSI_PROPERTY_LRT_ID, entry.getValue()));
        } else if (CCConstants.getValidLocalName(CCConstants.CCM_PROP_IO_REPL_TAXON_ID).equals(entry.getKey())) {
          queryBuilder.must(getFieldQuery(OERSI_PROPERTY_ABOUT + "." + OERSI_PROPERTY_ABOUT_ID, entry.getValue()));
        } else if (CCConstants.getValidLocalName(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_AUTHOR).equals(entry.getKey())) {
          queryBuilder.must(
            QueryBuilders.matchQuery(OERSI_PROPERTY_CREATOR + "." + OERSI_PROPERTY_CREATOR_NAME, joinedValues).operator(Operator.AND)
          );
        } else if ("license".equals(entry.getKey())) {
          Set<String> licensePrefixes = new HashSet<>();
          for (String licenseValue : entry.getValue()) {
            switch (licenseValue) {
              case "OPEN":
                licensePrefixes.add("https://creativecommons.org/publicdomain/mark");
                licensePrefixes.add("https://creativecommons.org/publicdomain/zero/");
                break;
              case "OER":
                licensePrefixes.add("https://creativecommons.org/publicdomain/mark");
                licensePrefixes.add("https://creativecommons.org/publicdomain/zero/");
                licensePrefixes.add("https://creativecommons.org/licenses/by/");
                licensePrefixes.add("https://creativecommons.org/licenses/by-sa/");
                break;
              case "CC_BY_RESTRICTED":
                licensePrefixes.add("https://creativecommons.org/licenses/by-nd/");
                licensePrefixes.add("https://creativecommons.org/licenses/by-nc-sa/");
                licensePrefixes.add("https://creativecommons.org/licenses/by-nc/");
                licensePrefixes.add("https://creativecommons.org/licenses/by-nc-nd/");
                break;
              default:
                break;
            }
          }
          BoolQueryBuilder fieldQueryBuilder = QueryBuilders.boolQuery();
          for (String prefix : licensePrefixes) {
            fieldQueryBuilder.should(QueryBuilders.prefixQuery(OERSI_PROPERTY_LICENSE + "." + OERSI_PROPERTY_LICENSE_ID, prefix));
          }
          queryBuilder.must(fieldQueryBuilder);
        }
      }
      return queryBuilder;
    }
    private QueryBuilder getFieldQuery(String fieldName, String[] values) {
      BoolQueryBuilder fieldQueryBuilder = QueryBuilders.boolQuery();
      for (String value : values) {
        fieldQueryBuilder.should(QueryBuilders.matchQuery(fieldName, value));
      }
      return fieldQueryBuilder;
    }

    @Override
    public Map<String, Object> executeRetrieveById(String oersiId) throws OersiAccessException {
      GetRequest getRequest = new GetRequest(index, oersiId);
      try {
        GetResponse getResponse = client.get(getRequest, RequestOptions.DEFAULT);
        Map<String, Object> oersiProperties = getResponse.getSourceAsMap();
        return convertOersiRecordToProperties(oersiId, oersiProperties);
      } catch (IOException e) {
        throw new OersiAccessException("Cannot access OERSI", e);
      }
    }
  }
  */
}
