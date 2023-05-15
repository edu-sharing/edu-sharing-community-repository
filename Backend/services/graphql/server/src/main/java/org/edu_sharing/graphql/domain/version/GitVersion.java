package org.edu_sharing.graphql.domain.version;

import lombok.Data;

@Data
public class GitVersion {
    String branch;
    GitTagInfo closest;
    GitCommitInfo commit;
    Boolean dirty;
}
