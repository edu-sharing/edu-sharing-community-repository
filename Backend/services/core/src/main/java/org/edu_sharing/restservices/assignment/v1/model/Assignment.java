package org.edu_sharing.restservices.assignment.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.edu_sharing.restservices.shared.Authority;
import org.edu_sharing.restservices.shared.NodeRef;
import org.edu_sharing.restservices.shared.UserSimple;

import java.util.Date;
import java.util.List;

public record Assignment(
        @JsonProperty(required = true)
        NodeRef ref,
        @JsonProperty(required = true)
        String title,
        String summary,
        @JsonProperty(required = true)
        UserSimple creator,
        @JsonProperty(required = true)
        Date created,
        Date startTime,
        Date endTime,
        @JsonProperty(required = true)
        Status status,
        @JsonProperty(required = true)
        Type type,
        @JsonProperty(required = true)
        boolean allowAdditionalDocumentSubmissions,
        Date modified,
        @JsonProperty(required = true)
        List<Permission> permissions
) {

    public enum Status {
        OPEN,
        PROGRESS,
        FINISHED,
        CANCELED
    }
    public enum Type {
        DEFAULT,
        SUBMISSION,
    }

    public enum Role {
        ASSIGNEE,
        COORDINATOR
    }

    public record Permission(
            @JsonProperty(required = true)
            Authority authority,
            @JsonProperty(required = true)
            Role role
    ){
    }
}
