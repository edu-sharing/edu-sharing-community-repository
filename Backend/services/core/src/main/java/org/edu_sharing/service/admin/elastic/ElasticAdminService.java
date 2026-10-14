package org.edu_sharing.service.admin.elastic;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.opencsv.CSVWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.MLText;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.TempFileProvider;
import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.jobs.annotations.Queued;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.notification.NotificationService;
import org.edu_sharing.service.permission.annotation.Permission;
import org.edu_sharing.service.search.SearchServiceElastic;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Admin tooling for the elasticsearch index: index overview, export (csv / json / full dump) and document inspection.
 * <p>
 * All public methods require a global admin via {@link Permission#requiresGlobalAdmin()}. This is enforced by the
 * {@code PermissionChecking} aspect and therefore only applies to calls through the spring proxy.
 * {@link #scheduleExport} is checked when scheduling and again when the job queue invokes it (as the scheduling user).
 */
@Slf4j
@Lazy
@Service
@RequiredArgsConstructor
public class ElasticAdminService {
    static final String PARENT_PREFIX = "parent::";
    static final String SCROLL_TIMEOUT = "5m";
    static final int PAGE_SIZE = 500;
    /**
     * documents per NDJSON file inside a dump, keeps every file below the default http.max_content_length of _bulk
     */
    static final int DUMP_DOCS_PER_FILE = 10000;
    static final String CSV_MULTIVALUE_SEPARATOR = "; ";

    private final SearchServiceElastic searchServiceElastic;
    private final NodeService nodeService;
    private final NotificationService notificationService;

    private final ObjectMapper mapper = new ObjectMapper();

    @Permission(requiresGlobalAdmin = true)
    public List<ElasticIndexInfo> getIndices() throws Throwable {
        JsonNode indices = mapper.readTree(searchServiceElastic.performRawRequest("GET", "_cat/indices?format=json&h=index,health,status,docs.count,store.size&s=index", null));
        JsonNode aliases = mapper.readTree(searchServiceElastic.performRawRequest("GET", "_cat/aliases?format=json&h=alias,index", null));
        Map<String, List<String>> aliasMap = new HashMap<>();
        for (JsonNode alias : aliases) {
            aliasMap.computeIfAbsent(alias.path("index").asText(), k -> new ArrayList<>()).add(alias.path("alias").asText());
        }
        List<ElasticIndexInfo> result = new ArrayList<>();
        for (JsonNode index : indices) {
            String name = index.path("index").asText();
            // hide internal system indices
            if (name.startsWith(".")) {
                continue;
            }
            String docsCount = index.path("docs.count").asText(null);
            result.add(new ElasticIndexInfo(
                    name,
                    aliasMap.getOrDefault(name, Collections.emptyList()),
                    index.path("health").asText(null),
                    index.path("status").asText(null),
                    StringUtils.isNumeric(docsCount) ? Long.parseLong(docsCount) : null,
                    index.path("store.size").asText(null),
                    SearchServiceElastic.WORKSPACE_INDEX.equals(name),
                    SearchServiceElastic.AUTHORITIES_INDEX.equals(name)
            ));
        }
        return result;
    }

    /**
     * Validate the export request before any output is written (e.g. before a streaming response is committed)
     *
     * @return the resolved index name
     */
    @Permission(requiresGlobalAdmin = true)
    public String validateExport(ElasticExportRequest request) {
        if (request == null || request.getFormat() == null) {
            throw new IllegalArgumentException("format is required");
        }
        if (!ElasticExportFormat.dump.equals(request.getFormat()) && getExportProperties(request).isEmpty()) {
            throw new IllegalArgumentException("properties are required for format " + request.getFormat());
        }
        return SearchServiceElastic.validateIndexExpression(request.getIndex());
    }

    /**
     * Run the export and stream the result to the given output stream
     */
    @Permission(requiresGlobalAdmin = true)
    public void export(ElasticExportRequest request, OutputStream out) throws IOException {
        String index = validateExport(request);
        ElasticExportFormat format = request.getFormat();
        List<String> properties = getExportProperties(request);
        ElasticPropertySource source = request.getPropertySource() == null ? ElasticPropertySource.elastic : request.getPropertySource();
        ObjectNode body = buildScrollBody(request.getQuery(), format, properties, source);
        log.info("elastic export started: index={} format={} source={} query={}", index, format, source, body);
        long count;
        switch (format) {
            case csv -> count = exportCsv(index, body, properties, source, out);
            case json -> count = exportJson(index, body, properties, source, out);
            case dump -> count = exportDump(index, body, out);
            default -> throw new IllegalArgumentException("unsupported format " + format);
        }
        log.info("elastic export finished: index={} format={} documents={}", index, format, count);
    }

    /**
     * Run the export as background job and store the result as file in the inbox of the scheduling user.
     * Parameters are passed separately since queued jobs are serialized as json.
     */
    @Queued(unique = true, ttl = "24h")
    @Permission(requiresGlobalAdmin = true)
    public void scheduleExport(String index, String query, ElasticExportFormat format, List<String> properties, ElasticPropertySource propertySource) {
        File tempFile = null;
        try {
            ElasticExportRequest request = new ElasticExportRequest();
            request.setIndex(index);
            request.setQuery(query);
            request.setFormat(format);
            request.setProperties(properties);
            request.setPropertySource(propertySource);

            tempFile = TempFileProvider.createTempFile("edu_elastic_export", "." + format.getExtension());
            try (OutputStream out = new BufferedOutputStream(new FileOutputStream(tempFile))) {
                export(request, out);
            }

            String inboxId = nodeService.getUserInbox(true);
            String filename = getExportFilename(SearchServiceElastic.validateIndexExpression(index), format);
            String nodeId = nodeService.createNode(inboxId, CCConstants.CCM_TYPE_IO, nodeService.getNameProperty(filename));
            try (InputStream in = new BufferedInputStream(new FileInputStream(tempFile))) {
                new MCAlfrescoAPIClient().writeContent(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId, in, format.getMimetype(), StandardCharsets.UTF_8.name(), CCConstants.CM_PROP_CONTENT);
            }
            String type = nodeService.getType(nodeId);
            String[] aspects = nodeService.getAspects(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), nodeId);
            Map<String, Object> nodeProps = nodeService.getProperties(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), nodeId);
            notificationService.notifyMaterialAddedToInbox(nodeId, type, Arrays.asList(aspects), nodeProps, null, "system", AuthenticationUtil.getRunAsUser());
        } catch (Throwable e) {
            throw new RuntimeException(e);
        } finally {
            if (tempFile != null && !tempFile.delete()) {
                log.warn("could not delete temp file {}", tempFile);
            }
        }
    }

    private static List<String> getExportProperties(ElasticExportRequest request) {
        return request.getProperties() == null ? Collections.emptyList() :
                request.getProperties().stream().map(String::trim).filter(StringUtils::isNotBlank).toList();
    }

    public static String getExportFilename(String index, ElasticExportFormat format) {
        String date = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        String safeIndex = index.replaceAll("[^A-Za-z0-9._\\-]", "_");
        return "elastic-" + (ElasticExportFormat.dump.equals(format) ? "dump" : "export") + "_" + safeIndex + "_" + date + "." + format.getExtension();
    }

    /**
     * Compare the indexed document(s) of a node with the current state in the repository
     */
    @Permission(requiresGlobalAdmin = true)
    public ElasticDocInspection inspect(String nodeId, String index) throws Throwable {
        index = SearchServiceElastic.validateIndexExpression(index);
        ElasticDocInspection result = new ElasticDocInspection();
        result.setNodeId(nodeId);
        result.setIndex(index);

        ObjectNode body = mapper.createObjectNode();
        body.putObject("query").putObject("term").put("nodeRef.id", nodeId);
        body.put("size", 10);
        JsonNode response = mapper.readTree(searchServiceElastic.performRawRequest("POST", index + "/_search", body.toString()));
        List<ElasticDocInspection.Document> documents = new ArrayList<>();
        for (JsonNode hit : response.path("hits").path("hits")) {
            documents.add(new ElasticDocInspection.Document(
                    hit.path("_index").asText(),
                    hit.path("_id").asText(),
                    mapper.writer().with(SerializationFeature.INDENT_OUTPUT).writeValueAsString(hit.path("_source"))
            ));
        }
        result.setDocuments(documents);

        NodeRef nodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId);
        Map<String, List<String>> repositoryProps = new TreeMap<>();
        if (NodeServiceHelper.exists(nodeRef)) {
            result.setExistsInRepository(true);
            result.setRepositoryType(CCConstants.getValidLocalName(NodeServiceHelper.getType(nodeRef)));
            result.setRepositoryAspects(Arrays.stream(NodeServiceHelper.getAspects(nodeRef)).map(CCConstants::getValidLocalName).toList());
            for (Map.Entry<QName, Serializable> entry : NodeServiceHelper.getPropertiesNative(nodeRef).entrySet()) {
                String localName = CCConstants.getValidLocalName(entry.getKey().toString());
                if (localName == null) {
                    continue;
                }
                repositoryProps.put(localName, normalizeRepositoryValue(entry.getValue()));
            }
        }

        Map<String, List<String>> indexProps = new TreeMap<>();
        if (!response.path("hits").path("hits").isEmpty()) {
            JsonNode properties = response.path("hits").path("hits").get(0).path("_source").path("properties");
            properties.fields().forEachRemaining(e -> indexProps.put(e.getKey(), normalizeIndexValue(e.getValue())));
        }
        result.setProperties(diff(repositoryProps, indexProps, !documents.isEmpty(), result.isExistsInRepository()));
        return result;
    }

    static List<ElasticDocInspection.PropertyDiff> diff(Map<String, List<String>> repositoryProps, Map<String, List<String>> indexProps, boolean hasIndex, boolean hasRepository) {
        Set<String> keys = new TreeSet<>(repositoryProps.keySet());
        keys.addAll(indexProps.keySet());
        keys.removeAll(ElasticDocInspection.IGNORED_PROPERTIES);
        List<ElasticDocInspection.PropertyDiff> diffs = new ArrayList<>();
        for (String key : keys) {
            List<String> repoValue = repositoryProps.get(key);
            List<String> indexValue = indexProps.get(key);
            ElasticDocInspection.DiffStatus status;
            if (repoValue == null) {
                status = ElasticDocInspection.DiffStatus.onlyIndex;
            } else if (indexValue == null) {
                status = ElasticDocInspection.DiffStatus.onlyRepository;
            } else {
                status = new HashSet<>(repoValue).equals(new HashSet<>(indexValue)) ? ElasticDocInspection.DiffStatus.equal : ElasticDocInspection.DiffStatus.different;
            }
            // an empty value is treated like a missing value
            if ((status == ElasticDocInspection.DiffStatus.onlyIndex && indexValue.isEmpty() && hasRepository) ||
                    (status == ElasticDocInspection.DiffStatus.onlyRepository && repoValue.isEmpty() && hasIndex)) {
                status = ElasticDocInspection.DiffStatus.equal;
            }
            diffs.add(new ElasticDocInspection.PropertyDiff(key, repoValue, indexValue, status));
        }
        return diffs;
    }

    static List<String> normalizeRepositoryValue(Serializable value) {
        if (value == null) {
            return Collections.emptyList();
        }
        if (value instanceof Collection<?> collection) {
            List<String> result = new ArrayList<>();
            for (Object o : collection) {
                if (o != null) {
                    result.addAll(normalizeRepositoryValue((Serializable) o));
                }
            }
            return result;
        }
        return Collections.singletonList(String.valueOf(convertRepositoryValue(value)));
    }

    static List<String> normalizeIndexValue(JsonNode value) {
        if (value == null || value.isNull() || value.isMissingNode()) {
            return Collections.emptyList();
        }
        if (value.isArray()) {
            List<String> result = new ArrayList<>();
            value.forEach(v -> result.addAll(normalizeIndexValue(v)));
            return result;
        }
        return Collections.singletonList(value.isValueNode() ? value.asText() : value.toString());
    }

    /**
     * converts alfresco specific types into plain values comparable with the indexed values
     */
    static Object convertRepositoryValue(Serializable value) {
        if (value instanceof Date date) {
            return date.getTime();
        }
        if (value instanceof NodeRef ref) {
            return ref.getId();
        }
        if (value instanceof MLText mlText) {
            return mlText.getDefaultValue();
        }
        if (value instanceof ContentData contentData) {
            return contentData.getContentUrl();
        }
        if (value instanceof Locale locale) {
            // same format as delivered to the tracker by alfresco (e.g. "de_DE_" instead of Locale.toString() "de_DE")
            return DefaultTypeConverter.INSTANCE.convert(String.class, locale);
        }
        if (value instanceof QName qName) {
            return CCConstants.getValidLocalName(qName.toString());
        }
        return value;
    }

    /**
     * Build the body of the initial scroll request from the user query.
     * Accepts either a full search body (only "query" and "sort" are used) or the query object itself.
     */
    ObjectNode buildScrollBody(String query, ElasticExportFormat format, List<String> properties, ElasticPropertySource source) throws IOException {
        ObjectNode body = mapper.createObjectNode();
        JsonNode parsed = StringUtils.isBlank(query) ? null : mapper.readTree(query);
        if (parsed == null || parsed.isEmpty()) {
            body.putObject("query").putObject("match_all");
        } else if (!parsed.isObject()) {
            throw new IllegalArgumentException("query must be a json object");
        } else if (parsed.has("query")) {
            body.set("query", parsed.get("query"));
            if (parsed.has("sort")) {
                body.set("sort", parsed.get("sort"));
            }
        } else {
            throw new IllegalArgumentException("query must be wrapped in {\"query\": {...}}");
        }
        if (!body.has("sort")) {
            // most efficient order for scrolling
            body.putArray("sort").add("_doc");
        }
        body.put("size", PAGE_SIZE);
        if (!ElasticExportFormat.dump.equals(format)) {
            ArrayNode includes = body.putArray("_source");
            includes.add("nodeRef");
            includes.add("parentRef");
            if (ElasticPropertySource.elastic.equals(source)) {
                properties.stream()
                        .filter(p -> !p.startsWith(PARENT_PREFIX))
                        .map(ElasticAdminService::getSourcePath)
                        .distinct()
                        .forEach(includes::add);
            }
        }
        return body;
    }

    /**
     * maps a requested property to its path in the _source document
     */
    static String getSourcePath(String property) {
        return property.contains(":") ? "properties." + property : property;
    }

    interface HitConsumer {
        void accept(JsonNode hit) throws IOException;
    }

    long scroll(String index, ObjectNode body, HitConsumer consumer) throws IOException {
        String scrollId = null;
        long count = 0;
        try {
            JsonNode response = mapper.readTree(searchServiceElastic.performRawRequest("POST", index + "/_search?scroll=" + SCROLL_TIMEOUT, body.toString()));
            while (true) {
                scrollId = response.path("_scroll_id").asText(null);
                JsonNode hits = response.path("hits").path("hits");
                if (hits.isEmpty()) {
                    break;
                }
                for (JsonNode hit : hits) {
                    consumer.accept(hit);
                    count++;
                }
                if (scrollId == null) {
                    break;
                }
                ObjectNode scrollBody = mapper.createObjectNode();
                scrollBody.put("scroll", SCROLL_TIMEOUT);
                scrollBody.put("scroll_id", scrollId);
                response = mapper.readTree(searchServiceElastic.performRawRequest("POST", "_search/scroll", scrollBody.toString()));
            }
        } finally {
            if (scrollId != null) {
                try {
                    ObjectNode clearBody = mapper.createObjectNode();
                    clearBody.putArray("scroll_id").add(scrollId);
                    searchServiceElastic.performRawRequest("DELETE", "_search/scroll", clearBody.toString());
                } catch (Exception e) {
                    log.warn("could not clear scroll context: {}", e.getMessage());
                }
            }
        }
        return count;
    }

    private long exportCsv(String index, ObjectNode body, List<String> properties, ElasticPropertySource source, OutputStream out) throws IOException {
        PropertyResolver resolver = new PropertyResolver(source);
        CSVWriter writer = new CSVWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
        List<String> header = new ArrayList<>();
        header.add("id");
        header.addAll(properties);
        writer.writeNext(header.toArray(new String[0]));
        long count = scroll(index, body, hit -> {
            String[] row = new String[properties.size() + 1];
            row[0] = getNodeId(hit);
            for (int i = 0; i < properties.size(); i++) {
                row[i + 1] = toCsvValue(resolver.resolve(hit, properties.get(i)));
            }
            writer.writeNext(row);
        });
        writer.flush();
        return count;
    }

    private long exportJson(String index, ObjectNode body, List<String> properties, ElasticPropertySource source, OutputStream out) throws IOException {
        PropertyResolver resolver = new PropertyResolver(source);
        JsonGenerator generator = mapper.getFactory().createGenerator(out, JsonEncoding.UTF8);
        generator.useDefaultPrettyPrinter();
        generator.writeStartArray();
        long count = scroll(index, body, hit -> {
            ObjectNode entry = mapper.createObjectNode();
            entry.put("id", getNodeId(hit));
            for (String property : properties) {
                JsonNode value = resolver.resolve(hit, property);
                if (property.startsWith(PARENT_PREFIX)) {
                    ObjectNode parent = entry.has("parent") ? (ObjectNode) entry.get("parent") : entry.putObject("parent");
                    parent.set(property.substring(PARENT_PREFIX.length()), value);
                } else {
                    entry.set(property, value);
                }
            }
            generator.writeTree(entry);
        });
        generator.writeEndArray();
        generator.flush();
        return count;
    }

    /**
     * Writes a zip containing
     * - mapping.json / settings.json / aliases.json of the exported index(es)
     * - data-00001.ndjson ...: documents in the _bulk format (action + _source line)
     * - README.txt with restore instructions
     */
    private long exportDump(String index, ObjectNode body, OutputStream out) throws IOException {
        ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8);
        JsonNode mapping = mapper.readTree(searchServiceElastic.performRawRequest("GET", index + "/_mapping", null));
        // include the index name in the bulk action only if the dump contains multiple indices (pattern) or an alias was used
        boolean includeIndex = mapping.size() != 1 || !mapping.has(index);
        writeZipJson(zip, "mapping.json", mapping);
        writeZipJson(zip, "settings.json", mapper.readTree(searchServiceElastic.performRawRequest("GET", index + "/_settings", null)));
        writeZipJson(zip, "aliases.json", mapper.readTree(searchServiceElastic.performRawRequest("GET", index + "/_alias", null)));
        Writer writer = new OutputStreamWriter(zip, StandardCharsets.UTF_8);
        long[] fileCounter = {0};
        long count = scroll(index, body, hit -> {
            if (fileCounter[0] % DUMP_DOCS_PER_FILE == 0) {
                if (fileCounter[0] > 0) {
                    writer.flush();
                    zip.closeEntry();
                }
                zip.putNextEntry(new ZipEntry(String.format("data-%05d.ndjson", fileCounter[0] / DUMP_DOCS_PER_FILE + 1)));
            }
            ObjectNode action = mapper.createObjectNode();
            ObjectNode meta = action.putObject("index");
            if (includeIndex) {
                meta.put("_index", hit.path("_index").asText());
            }
            meta.put("_id", hit.path("_id").asText());
            writer.write(action.toString());
            writer.write('\n');
            writer.write(mapper.writeValueAsString(hit.path("_source")));
            writer.write('\n');
            fileCounter[0]++;
        });
        writer.flush();
        if (count > 0) {
            zip.closeEntry();
        }
        zip.putNextEntry(new ZipEntry("README.txt"));
        writer.write(getDumpReadme(index, count, includeIndex));
        writer.flush();
        zip.closeEntry();
        zip.finish();
        zip.flush();
        return count;
    }

    private void writeZipJson(ZipOutputStream zip, String name, JsonNode content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(mapper.writer().with(SerializationFeature.INDENT_OUTPUT).writeValueAsBytes(content));
        zip.closeEntry();
    }

    private static String getDumpReadme(String index, long count, boolean includeIndex) {
        return "edu-sharing elasticsearch dump\n" +
                "index: " + index + "\n" +
                "created: " + new Date() + "\n" +
                "documents: " + count + "\n\n" +
                "mapping.json, settings.json and aliases.json contain the index definition at the time of the dump.\n" +
                "data-*.ndjson contain the documents in the elasticsearch _bulk format.\n\n" +
                "Restore (example):\n" +
                "  1. create the target index using the \"settings\" (without uuid/creation_date/provided_name/version) and \"mappings\" of the json files\n" +
                "  2. for f in data-*.ndjson; do curl -s -H 'Content-Type: application/x-ndjson' -XPOST " +
                (includeIndex ? "'http://localhost:9200/_bulk'" : "'http://localhost:9200/<target-index>/_bulk'") +
                " --data-binary \"@$f\"; done\n";
    }

    static String getNodeId(JsonNode hit) {
        String id = hit.path("_source").path("nodeRef").path("id").asText(null);
        return id != null ? id : hit.path("_id").asText(null);
    }

    static String toCsvValue(JsonNode value) {
        if (value == null || value.isNull() || value.isMissingNode()) {
            return "";
        }
        if (value.isArray()) {
            List<String> values = new ArrayList<>();
            value.forEach(v -> values.add(toCsvValue(v)));
            return String.join(CSV_MULTIVALUE_SEPARATOR, values);
        }
        return value.isValueNode() ? value.asText() : value.toString();
    }

    static JsonNode getSourceValue(JsonNode source, String property) {
        JsonNode current;
        if (property.contains(":")) {
            current = source.path("properties").path(property);
        } else {
            current = source;
            for (String part : property.split("\\.")) {
                current = current.path(part);
            }
        }
        return current.isMissingNode() ? JsonNodeFactory.instance.nullNode() : current;
    }

    /**
     * Resolves property values of a hit, either from the _source or from the repository.
     * Parent values are always read from the repository and cached per export.
     */
    class PropertyResolver {
        private final ElasticPropertySource source;
        private final Map<String, Map<String, JsonNode>> parentCache = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Map<String, JsonNode>> eldest) {
                return size() > 1000;
            }
        };

        PropertyResolver(ElasticPropertySource source) {
            this.source = source;
        }

        JsonNode resolve(JsonNode hit, String property) {
            JsonNode sourceDoc = hit.path("_source");
            if (property.startsWith(PARENT_PREFIX)) {
                String parentId = sourceDoc.path("parentRef").path("id").asText(null);
                if (parentId == null) {
                    return JsonNodeFactory.instance.nullNode();
                }
                String realProperty = property.substring(PARENT_PREFIX.length());
                return parentCache.computeIfAbsent(parentId, k -> new HashMap<>())
                        .computeIfAbsent(realProperty, p -> getRepositoryValue(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, parentId), p));
            }
            if (ElasticPropertySource.alfresco.equals(source) && property.contains(":")) {
                JsonNode storeRef = sourceDoc.path("nodeRef").path("storeRef");
                StoreRef store = storeRef.isMissingNode() ? StoreRef.STORE_REF_WORKSPACE_SPACESSTORE :
                        new StoreRef(storeRef.path("protocol").asText(StoreRef.PROTOCOL_WORKSPACE), storeRef.path("identifier").asText("SpacesStore"));
                return getRepositoryValue(new NodeRef(store, getNodeId(hit)), property);
            }
            return getSourceValue(sourceDoc, property);
        }

        private JsonNode getRepositoryValue(NodeRef nodeRef, String property) {
            try {
                Serializable value = NodeServiceHelper.getPropertyNative(nodeRef, CCConstants.getValidGlobalName(property));
                if (value instanceof Collection<?> collection) {
                    ArrayNode array = JsonNodeFactory.instance.arrayNode();
                    collection.forEach(v -> array.add(mapper.valueToTree(v instanceof Serializable s ? convertRepositoryValue(s) : v)));
                    return array;
                }
                return value == null ? JsonNodeFactory.instance.nullNode() : mapper.valueToTree(convertRepositoryValue(value));
            } catch (Throwable t) {
                log.debug("could not read property {} of {}: {}", property, nodeRef, t.getMessage());
                return JsonNodeFactory.instance.nullNode();
            }
        }
    }
}
