package org.edu_sharing.graphql.domain.version;

import lombok.Data;

@Data
public class EduSharingInfo {
    GitVersion git;
    MavenInfo maven;
    EduSharingVersionInfo version;
}
