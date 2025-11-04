package org.edu_sharing.service.assignment;

import lombok.Data;
import org.edu_sharing.lightbend.ConfigurationProperties;

@Data
@ConfigurationProperties( prefix = "assignment")
public class AssignmentConfig {
    private String nodePattern;
}
