package org.edu_sharing.service.admin.elastic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.notification.NotificationService;
import org.edu_sharing.service.search.SearchServiceElastic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ElasticAdminServiceTest {
    private static final String HITS = """
            {"_scroll_id":"scroll1","hits":{"total":{"value":2},"hits":[
              {"_index":"workspace_11.0","_id":"1","_source":{"nodeRef":{"id":"node-1"},"parentRef":{"id":"parent-1"},"properties":{"cm:name":"a.txt","cclom:general_keyword":["k1","k2"]}}},
              {"_index":"workspace_11.0","_id":"2","_source":{"nodeRef":{"id":"node-2"},"properties":{"cm:name":"b.txt"}}}
            ]}}""";
    private static final String EMPTY = """
            {"_scroll_id":"scroll1","hits":{"total":{"value":2},"hits":[]}}""";

    @Mock
    private SearchServiceElastic searchServiceElastic;
    @Mock
    private NodeService nodeService;
    @Mock
    private NotificationService notificationService;

    private ElasticAdminService underTest;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setup() throws Exception {
        underTest = new ElasticAdminService(searchServiceElastic, nodeService, notificationService);
        when(searchServiceElastic.performRawRequest(eq("POST"), startsWith("workspace_11.0/_search?scroll="), anyString())).thenReturn(HITS);
        when(searchServiceElastic.performRawRequest(eq("POST"), eq("_search/scroll"), anyString())).thenReturn(EMPTY);
        when(searchServiceElastic.performRawRequest(eq("DELETE"), eq("_search/scroll"), anyString())).thenReturn("{}");
    }


    @Test
    void validateIndexExpression() {
        assertEquals(SearchServiceElastic.WORKSPACE_INDEX, SearchServiceElastic.validateIndexExpression(null));
        assertEquals(SearchServiceElastic.WORKSPACE_INDEX, SearchServiceElastic.validateIndexExpression(" "));
        assertEquals(SearchServiceElastic.WORKSPACE_INDEX, SearchServiceElastic.validateIndexExpression("-workspace-"));
        assertEquals(SearchServiceElastic.AUTHORITIES_INDEX, SearchServiceElastic.validateIndexExpression("-authorities-"));
        assertEquals("workspace_*,authorities_11.0", SearchServiceElastic.validateIndexExpression("workspace_*,authorities_11.0"));
        assertThrows(IllegalArgumentException.class, () -> SearchServiceElastic.validateIndexExpression("workspace/_doc/1"));
        assertThrows(IllegalArgumentException.class, () -> SearchServiceElastic.validateIndexExpression("_all?pretty"));
        assertThrows(IllegalArgumentException.class, () -> SearchServiceElastic.validateIndexExpression("../x"));
    }

    @Test
    void buildScrollBody() throws Exception {
        ObjectNode body = underTest.buildScrollBody("", ElasticExportFormat.dump, List.of(), ElasticPropertySource.elastic);
        assertTrue(body.path("query").has("match_all"));
        assertEquals("_doc", body.path("sort").get(0).asText());
        assertFalse(body.has("_source"));

        body = underTest.buildScrollBody("{\"query\":{\"term\":{\"type\":\"ccm:io\"}},\"size\":5,\"from\":10,\"sort\":[{\"cm:name\":\"asc\"}]}",
                ElasticExportFormat.csv, List.of("cm:name", "parent::cm:name", "nodeRef.id"), ElasticPropertySource.elastic);
        assertEquals("ccm:io", body.path("query").path("term").path("type").asText());
        assertEquals(ElasticAdminService.PAGE_SIZE, body.path("size").asInt());
        assertFalse(body.has("from"));
        assertEquals("asc", body.path("sort").get(0).path("cm:name").asText());
        List<String> includes = new ArrayList<>();
        body.path("_source").forEach(n -> includes.add(n.asText()));
        assertEquals(List.of("nodeRef", "parentRef", "properties.cm:name", "nodeRef.id"), includes);

        body = underTest.buildScrollBody("{\"query\":{\"term\":{\"type\":\"ccm:io\"}}}", ElasticExportFormat.json, List.of("cm:name"), ElasticPropertySource.alfresco);
        assertEquals("ccm:io", body.path("query").path("term").path("type").asText());
        assertEquals(2, body.path("_source").size());

        // same structure as the raw elastic mode: the query must be wrapped in {"query": ...}
        assertThrows(IllegalArgumentException.class, () -> underTest.buildScrollBody("{\"term\":{\"type\":\"ccm:io\"}}", ElasticExportFormat.json, List.of("cm:name"), ElasticPropertySource.elastic));
        assertThrows(IllegalArgumentException.class, () -> underTest.buildScrollBody("[1]", ElasticExportFormat.json, List.of("cm:name"), ElasticPropertySource.elastic));
    }

    @Test
    void exportCsv() throws Throwable {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        underTest.export(request(ElasticExportFormat.csv, List.of("cm:name", "cclom:general_keyword")), out);
        String[] lines = out.toString(StandardCharsets.UTF_8).split("\n");
        assertEquals("\"id\",\"cm:name\",\"cclom:general_keyword\"", lines[0]);
        assertEquals("\"node-1\",\"a.txt\",\"k1; k2\"", lines[1]);
        assertEquals("\"node-2\",\"b.txt\",\"\"", lines[2]);
        // scroll context must be released
        verify(searchServiceElastic).performRawRequest(eq("DELETE"), eq("_search/scroll"), contains("scroll1"));
    }

    @Test
    void exportJson() throws Throwable {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        underTest.export(request(ElasticExportFormat.json, List.of("cm:name", "cclom:general_keyword")), out);
        JsonNode result = mapper.readTree(out.toByteArray());
        assertEquals(2, result.size());
        assertEquals("node-1", result.get(0).path("id").asText());
        assertEquals("a.txt", result.get(0).path("cm:name").asText());
        assertEquals(2, result.get(0).path("cclom:general_keyword").size());
        assertTrue(result.get(1).path("cclom:general_keyword").isMissingNode() || result.get(1).path("cclom:general_keyword").isNull());
    }

    @Test
    void exportRequiresProperties() {
        assertThrows(IllegalArgumentException.class, () -> underTest.export(request(ElasticExportFormat.csv, List.of(" ")), new ByteArrayOutputStream()));
    }

    @Test
    void exportDump() throws Throwable {
        when(searchServiceElastic.performRawRequest("GET", "workspace_11.0/_mapping", null)).thenReturn("{\"workspace_11.0\":{\"mappings\":{}}}");
        when(searchServiceElastic.performRawRequest("GET", "workspace_11.0/_settings", null)).thenReturn("{\"workspace_11.0\":{\"settings\":{}}}");
        when(searchServiceElastic.performRawRequest("GET", "workspace_11.0/_alias", null)).thenReturn("{\"workspace_11.0\":{\"aliases\":{}}}");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        underTest.export(request(ElasticExportFormat.dump, null), out);

        Map<String, String> entries = new LinkedHashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(out.toByteArray()))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                entries.put(entry.getName(), new String(zip.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        assertEquals(List.of("mapping.json", "settings.json", "aliases.json", "data-00001.ndjson", "README.txt"), new ArrayList<>(entries.keySet()));
        String[] lines = entries.get("data-00001.ndjson").split("\n");
        assertEquals(4, lines.length);
        // single concrete index -> no _index in the action, so the dump can be restored into any index
        assertEquals("{\"index\":{\"_id\":\"1\"}}", lines[0]);
        assertEquals("node-1", mapper.readTree(lines[1]).path("nodeRef").path("id").asText());
        assertEquals("{\"index\":{\"_id\":\"2\"}}", lines[2]);
        assertTrue(entries.get("README.txt").contains("documents: 2"));
    }

    @Test
    void diff() {
        Map<String, List<String>> repo = new HashMap<>();
        repo.put("cm:name", List.of("a"));
        repo.put("cclom:general_keyword", List.of("k1", "k2"));
        repo.put("ccm:only_repo", List.of("x"));
        repo.put("ccm:empty_repo", List.of());
        repo.put("cm:content", List.of("store://content.bin"));
        Map<String, List<String>> index = new HashMap<>();
        index.put("cm:name", List.of("b"));
        index.put("cclom:general_keyword", List.of("k2", "k1"));
        index.put("ccm:only_index", List.of("y"));
        index.put("cm:content", List.of("{\"size\":\"1\"}"));

        Map<String, ElasticDocInspection.DiffStatus> result = new HashMap<>();
        ElasticAdminService.diff(repo, index, true, true).forEach(d -> result.put(d.getProperty(), d.getStatus()));
        assertEquals(ElasticDocInspection.DiffStatus.different, result.get("cm:name"));
        assertEquals(ElasticDocInspection.DiffStatus.equal, result.get("cclom:general_keyword"));
        assertEquals(ElasticDocInspection.DiffStatus.onlyRepository, result.get("ccm:only_repo"));
        assertEquals(ElasticDocInspection.DiffStatus.onlyIndex, result.get("ccm:only_index"));
        assertEquals(ElasticDocInspection.DiffStatus.equal, result.get("ccm:empty_repo"));
        assertFalse(result.containsKey("cm:content"));
    }

    @Test
    void normalizeRepositoryValueLocale() {
        // alfresco serializes locales with a trailing separator, like the values delivered to the tracker
        assertEquals(List.of("de_DE_"), ElasticAdminService.normalizeRepositoryValue(Locale.GERMANY));
        assertEquals(List.of("de_"), ElasticAdminService.normalizeRepositoryValue(Locale.GERMAN));
    }

    @Test
    void getExportFilename() {
        String filename = ElasticAdminService.getExportFilename("workspace_*", ElasticExportFormat.dump);
        assertTrue(filename.startsWith("elastic-dump_workspace___"));
        assertTrue(filename.endsWith(".zip"));
    }

    private static ElasticExportRequest request(ElasticExportFormat format, List<String> properties) {
        ElasticExportRequest request = new ElasticExportRequest();
        request.setFormat(format);
        request.setProperties(properties);
        return request;
    }
}
