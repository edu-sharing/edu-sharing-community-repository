package org.edu_sharing.graphql.domain.version;

import lombok.Data;

@Data
public class EduSharingVersionInfo {
    String full;
    String major;
    String minor;
    String patch;
    String qualifier;
    String build;
}
