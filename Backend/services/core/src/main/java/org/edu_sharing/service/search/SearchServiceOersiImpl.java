package org.edu_sharing.service.search;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.json.JsonpUtils;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.metadataset.v2.MetadataQuery;
import org.edu_sharing.metadataset.v2.MetadataReader;
import org.edu_sharing.metadataset.v2.MetadataSet;
import org.edu_sharing.metadataset.v2.tools.MetadataElasticSearchHelper;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.forms.VCardTool;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.repository.server.appcontext.ApplicationInfoContextHolder;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.restservices.shared.MdsQueryCriteria;
import org.edu_sharing.service.model.NodeRef;
import org.edu_sharing.service.model.NodeRefImpl;
import org.edu_sharing.service.search.model.SearchToken;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Federated search connector for OERSI (https://oersi.org).
 *
 * <p>OERSI exposes its metadata index as a (read-only) Elasticsearch proxy, but its
 * public frontend is a restricted nginx proxy - not a full Elasticsearch - that only
 * accepts plain {@code application/json} requests. We therefore do NOT use the
 * Elasticsearch Java client here (it negotiates the Elastic-specific
 * {@code application/vnd.elasticsearch+json} media type and gets rejected/redirected by
 * OERSI). Instead we build the query with the co.elastic query DSL, serialize it to JSON
 * and send it over the JDK {@link HttpClient} (exactly like a plain curl). The OERSI/AMB
 * record fields are then mapped onto edu-sharing properties.</p>
 *
 * <p>Endpoint defaults to the current public API
 * {@code https://oersi.org/api/search/} (index {@code oer_data}). The API requires a
 * meaningful {@code User-Agent} header. Host, port, protocol, path prefix, index and
 * User-Agent are all overridable via the repository {@link ApplicationInfo}
 * configuration.</p>
 */
@Slf4j
@Lazy
@Service
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

    private static final String DEFAULT_USER_AGENT = "edu-sharing federated-search (https://www.edu-sharing.com)";

    private final String oersiHost;
    private final int oersiPort;
    private final String oersiScheme;
    private final String oersiPathPrefix;
    private final String oersiIndex;
    private final String oersiUserAgent;

    String repositoryId = null;

    public SearchServiceOersiImpl() {
        ApplicationInfo appInfo = ApplicationInfoContextHolder.getCurrentApplicationInfo();
        this.repositoryId = appInfo.getAppId();
        this.oersiHost = appInfo.getString(ApplicationInfo.KEY_HOST, "oersi.org");
        this.oersiPort = Integer.parseInt(appInfo.getString(ApplicationInfo.KEY_PORT, "443"));
        this.oersiScheme = appInfo.getString(ApplicationInfo.KEY_PROTOCOL, "https");
        this.oersiPathPrefix = appInfo.getString("pathprefix", "/api/search");
        this.oersiIndex = appInfo.getString("index", "oer_data");
        this.oersiUserAgent = appInfo.getString("useragent", DEFAULT_USER_AGENT);
    }

    public Map<String, Object> retrieveNode(String nodeId) throws Exception {
        String url = baseUrl() + "/" + oersiIndex + "/_doc/" + URLEncoder.encode(nodeId, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", oersiUserAgent)
                .header("Accept", "application/json")
                .GET()
                .build();
        JSONObject json = new JSONObject(execute(request));
        if (!json.optBoolean("found", false) || !json.has("_source")) {
            throw new IOException("OERSI record not found: " + nodeId);
        }
        Map<String, Object> oersiProperties = json.getJSONObject("_source").toMap();
        return convertOersiRecordToProperties(nodeId, oersiProperties);
    }

    @Override
    public List<? extends Suggestion> getSuggestions(MetadataSet mds, String queryId, String parameterId, String value, List<MdsQueryCriteria> criteria) {
        // TODO
        return new ArrayList<>();
    }

    @Override
    public SearchResultNodeRef search(MetadataSet mds, String query, Map<String, String[]> criteria,
                                      SearchToken searchToken) throws Throwable {
        SearchResultNodeRef searchResultNodeRef = new SearchResultNodeRef();
        List<NodeRef> data = new ArrayList<>();

        Query queryBuilder = getQuery(mds, query, criteria);
        // The query is built with the co.elastic query DSL and serialized to plain JSON.
        String queryJson = JsonpUtils.toJsonString(queryBuilder, new JacksonJsonpMapper());
        String body = "{\"track_total_hits\":true,\"from\":" + searchToken.getFrom()
                + ",\"size\":" + searchToken.getMaxResult()
                + ",\"query\":" + queryJson + "}";
        logger.debug("oersi search body: " + body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl() + "/" + oersiIndex + "/_search"))
                .header("User-Agent", oersiUserAgent)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        JSONObject json = new JSONObject(execute(request));
        JSONObject hits = json.getJSONObject("hits");
        long total = hits.getJSONObject("total").getLong("value");
        JSONArray hitArr = hits.getJSONArray("hits");
        for (int i = 0; i < hitArr.length(); i++) {
            JSONObject hit = hitArr.getJSONObject(i);
            String oersiId = hit.getString("_id");
            Map<String, Object> oersiProperties = hit.getJSONObject("_source").toMap();
            Map<String, Object> properties = convertOersiRecordToProperties(oersiId, oersiProperties);
            NodeRef ref = new NodeRefImpl(repositoryId,
                    StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getProtocol(),
                    StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), new HashMap<>(properties));
            data.add(ref);
        }
        searchResultNodeRef.setStartIDX(searchToken.getFrom());
        searchResultNodeRef.setData(data);
        searchResultNodeRef.setNodeCount((int) total);
        return searchResultNodeRef;
    }

    /**
     * Base URL of the OERSI search API, e.g. {@code https://oersi.org/api/search}.
     * The standard port (443/https, 80/http) is intentionally omitted so it never ends
     * up in the {@code Host} header - OERSI's frontend redirects (301) any request whose
     * Host carries an explicit port.
     */
    private String baseUrl() {
        StringBuilder sb = new StringBuilder();
        sb.append(oersiScheme).append("://").append(oersiHost);
        boolean defaultPort = ("https".equalsIgnoreCase(oersiScheme) && oersiPort == 443)
                || ("http".equalsIgnoreCase(oersiScheme) && oersiPort == 80);
        if (!defaultPort && oersiPort > 0) {
            sb.append(":").append(oersiPort);
        }
        sb.append(oersiPathPrefix);
        return sb.toString();
    }

    private String execute(HttpRequest request) throws IOException {
        HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("OERSI request to " + request.uri() + " failed: HTTP "
                        + response.statusCode() + " " + response.body());
            }
            return response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("OERSI request to " + request.uri() + " was interrupted", e);
        }
    }

    /**
     * Build the elasticsearch query from the metadata set DSL query if available,
     * falling back to a hand-built query over the OERSI field names otherwise.
     */
    private Query getQuery(MetadataSet mds, String query, Map<String, String[]> criteria) {
        MetadataQuery queryData;
        try {
            queryData = mds.findQuery(query, MetadataReader.QUERY_SYNTAX_DSL);
        } catch (IllegalArgumentException e) {
            logger.info("Query " + query + " is not defined within dsl language, switching to default query...");
            return getDefaultQuery(criteria);
        }
        try {
            return MetadataElasticSearchHelper.getElasticSearchQuery(null,
                    mds.getQueries(MetadataReader.QUERY_SYNTAX_DSL), queryData, criteria).build()._toQuery();
        } catch (Throwable e) {
            logger.info("Cannot get elasticsearch query, switching to default query... ", e);
            return getDefaultQuery(criteria);
        }
    }

    private Query getDefaultQuery(Map<String, String[]> criteria) {
        BoolQuery.Builder queryBuilder = QueryBuilders.bool();
        for (Map.Entry<String, String[]> entry : criteria.entrySet()) {
            String joinedValues = String.join(" ", entry.getValue());
            if (joinedValues.trim().length() == 0) {
                continue;
            }
            if (MetadataSet.DEFAULT_CLIENT_QUERY_CRITERIA.equals(entry.getKey())) {
                final String text = joinedValues;
                queryBuilder.must(must -> must.multiMatch(mm -> mm
                        .query(text)
                        .fields(OERSI_PROPERTY_NAME,
                                OERSI_PROPERTY_DESCRIPTION,
                                OERSI_PROPERTY_KEYWORDS,
                                OERSI_PROPERTY_CREATOR + "." + OERSI_PROPERTY_CREATOR_NAME)));
            } else if (CCConstants.getValidLocalName(CCConstants.LOM_PROP_GENERAL_KEYWORD).equals(entry.getKey())) {
                queryBuilder.must(getFieldQuery(OERSI_PROPERTY_KEYWORDS, entry.getValue()));
            } else if (CCConstants.getValidLocalName(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_LEARNINGRESSOURCETYPE).equals(entry.getKey())) {
                queryBuilder.must(getFieldQuery(OERSI_PROPERTY_LRT + "." + OERSI_PROPERTY_LRT_ID, entry.getValue()));
            } else if (CCConstants.getValidLocalName(CCConstants.CCM_PROP_IO_REPL_TAXON_ID).equals(entry.getKey())) {
                queryBuilder.must(getFieldQuery(OERSI_PROPERTY_ABOUT + "." + OERSI_PROPERTY_ABOUT_ID, entry.getValue()));
            } else if (CCConstants.getValidLocalName(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_AUTHOR).equals(entry.getKey())) {
                final String text = joinedValues;
                queryBuilder.must(must -> must.match(m -> m
                        .field(OERSI_PROPERTY_CREATOR + "." + OERSI_PROPERTY_CREATOR_NAME)
                        .query(text)
                        .operator(Operator.And)));
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
                BoolQuery.Builder fieldQueryBuilder = QueryBuilders.bool();
                for (String prefix : licensePrefixes) {
                    fieldQueryBuilder.should(should -> should.prefix(p -> p
                            .field(OERSI_PROPERTY_LICENSE + "." + OERSI_PROPERTY_LICENSE_ID)
                            .value(prefix)));
                }
                queryBuilder.must(fieldQueryBuilder.build()._toQuery());
            }
        }
        return queryBuilder.build()._toQuery();
    }

    private Query getFieldQuery(String fieldName, String[] values) {
        BoolQuery.Builder fieldQueryBuilder = QueryBuilders.bool();
        for (String value : values) {
            fieldQueryBuilder.should(should -> should.match(m -> m.field(fieldName).query(value)));
        }
        return fieldQueryBuilder.build()._toQuery();
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
        if (value instanceof String && StringUtils.isNotEmpty((String) value)) {
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
            if (licenseUrl.startsWith("https://creativecommons.org/licenses/by/")) {
                licenseKey = CCConstants.COMMON_LICENSE_CC_BY;
            } else if (licenseUrl.startsWith("https://creativecommons.org/publicdomain/zero/1.0")) {
                licenseKey = CCConstants.COMMON_LICENSE_CC_ZERO;
            } else if (licenseUrl.startsWith("https://creativecommons.org/licenses/by-sa/")) {
                licenseKey = CCConstants.COMMON_LICENSE_CC_BY_SA;
            } else if (licenseUrl.startsWith("https://creativecommons.org/licenses/by-nc/")) {
                licenseKey = CCConstants.COMMON_LICENSE_CC_BY_NC;
            } else if (licenseUrl.startsWith("https://creativecommons.org/licenses/by-nd/")) {
                licenseKey = CCConstants.COMMON_LICENSE_CC_BY_ND;
            } else if (licenseUrl.startsWith("https://creativecommons.org/licenses/by-nc-nd/")) {
                licenseKey = CCConstants.COMMON_LICENSE_CC_BY_NC_ND;
            } else if (licenseUrl.startsWith("https://creativecommons.org/licenses/by-nc-sa/")) {
                licenseKey = CCConstants.COMMON_LICENSE_CC_BY_NC_SA;
            } else if (licenseUrl.startsWith("https://creativecommons.org/publicdomain/mark/1.0")) {
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
}
