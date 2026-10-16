package org.edu_sharing.service.search;

import co.elastic.clients.json.JsonpSerializable;
import co.elastic.clients.json.JsonpUtils;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Converts elastic requests/queries into readable (and still executable) json.
 * All wrapper queries (base64 encoded, see {@link ReadableWrapperQueryBuilder}) are replaced by their decoded content.
 */
@Slf4j
public class ElasticDslReadableHelper {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ElasticDslReadableHelper() {
    }

    public static String toReadableJson(JsonpSerializable value) {
        return decodeWrapperQueries(JsonpUtils.toJsonString(value, new JacksonJsonpMapper()));
    }

    /**
     * @param json any elastic json (request body or query)
     * @return pretty printed json with all (nested) wrapper queries decoded
     */
    public static String decodeWrapperQueries(String json) {
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(decode(MAPPER.readTree(json)));
        } catch (JsonProcessingException e) {
            log.warn("Could not parse elastic json: {}", e.getMessage());
            return json;
        }
    }

    static JsonNode decode(JsonNode node) {
        if (node instanceof ObjectNode object) {
            JsonNode wrapper = object.get("wrapper");
            if (object.size() == 1 && wrapper != null && wrapper.path("query").isTextual()) {
                try {
                    String decoded = new String(Base64.getDecoder().decode(wrapper.get("query").asText()), StandardCharsets.UTF_8);
                    return decode(MAPPER.readTree(decoded));
                } catch (IllegalArgumentException | JsonProcessingException e) {
                    log.debug("Could not decode wrapper query: {}", e.getMessage());
                    return object;
                }
            }
            List<String> fieldNames = new ArrayList<>();
            object.fieldNames().forEachRemaining(fieldNames::add);
            for (String fieldName : fieldNames) {
                object.set(fieldName, decode(object.get(fieldName)));
            }
        } else if (node instanceof ArrayNode array) {
            for (int i = 0; i < array.size(); i++) {
                array.set(i, decode(array.get(i)));
            }
        }
        return node;
    }
}
