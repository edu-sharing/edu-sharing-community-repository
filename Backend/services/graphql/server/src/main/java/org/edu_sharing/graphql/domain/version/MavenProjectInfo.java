package org.edu_sharing.graphql.domain.version;

import lombok.Data;

@Data
public class MavenProjectInfo {
    String groupId;
    String artifactId;
    String version;
}
