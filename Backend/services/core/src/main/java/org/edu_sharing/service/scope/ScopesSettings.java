package org.edu_sharing.service.scope;

import lombok.Data;
import org.edu_sharing.lightbend.ConfigurationProperties;

/**
 * lightbend configuration repository.scopes (feature flag B4), refreshed automatically.
 */
@Data
@ConfigurationProperties(prefix = "repository.scopes")
public class ScopesSettings {
    /** context override of the scope group: repository.context.&lt;contextId&gt;.repository.scopes.group */
    public static final String CONTEXT_GROUP_PATH = "repository.scopes.group";

    public enum SortStrategy {
        RUNTIME,
        MERGE
    }

    private boolean enabled;
    private Sort sort;
    private Bulk bulk;
    private ChangeFeed changeFeed;
    private Relevance relevance;

    public SortStrategy getSortStrategyValue() {
        return SortStrategy.valueOf(sort.getStrategy().toUpperCase());
    }

    @Data
    public static class Sort {
        /** "runtime" (painless runtime field) or "merge" (backend merge sort) */
        private String strategy;
    }

    @Data
    public static class Bulk {
        private int partitionSize;
    }

    @Data
    public static class Relevance {
        /** additional score of the entries recommended inside the active scope, 0 disables it */
        private double recommendedBoost;
    }

    @Data
    public static class ChangeFeed {
        private int pageSize;
    }
}
