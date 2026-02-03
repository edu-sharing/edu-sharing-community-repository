package org.edu_sharing.service.relations;

import lombok.Data;
import org.edu_sharing.lightbend.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "repository.relations.trace")
public class RelationTraceSettings {
    private int maxDepth;
}
