package org.edu_sharing.service.admin.elastic;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@Data
public class ElasticDocInspection {
    /**
     * properties which are not compared since repository and index store them in different formats
     * (e.g. cm:content: content url vs. content data object)
     */
    public static final Set<String> IGNORED_PROPERTIES = Set.of("cm:content");

    private String nodeId;
    private String index;
    /**
     * true if the node exists in the alfresco workspace store
     */
    private boolean existsInRepository;
    private String repositoryType;
    private List<String> repositoryAspects;
    /**
     * all documents found for the node id (more than one indicates a broken index)
     */
    private List<Document> documents;
    /**
     * property comparison between repository and the first indexed document
     */
    private List<PropertyDiff> properties;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "ElasticDocument")
    public static class Document {
        private String index;
        private String id;
        /**
         * raw _source, pretty printed json
         */
        private String source;
    }

    public enum DiffStatus {
        equal,
        different,
        onlyRepository,
        onlyIndex
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "ElasticPropertyDiff")
    public static class PropertyDiff {
        private String property;
        private List<String> repositoryValue;
        private List<String> indexValue;
        private DiffStatus status;
    }
}
