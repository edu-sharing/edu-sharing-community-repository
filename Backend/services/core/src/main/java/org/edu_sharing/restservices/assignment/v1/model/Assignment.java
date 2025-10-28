package org.edu_sharing.restservices.assignment.v1.model;

import org.edu_sharing.restservices.shared.Authority;
import org.edu_sharing.restservices.shared.NodeRef;
import org.edu_sharing.restservices.shared.UserSimple;

import java.util.Date;
import java.util.List;

public record Assignment(
        NodeRef ref,
        String title,
        String summary,
        UserSimple creator,
        Date created,
        Date startTime,
        Date endTime,
        Status status,
        Type type,
        boolean allowDelayedSubmission,
        boolean allowAdditionalDocumentSubmissions,
        Date modified,
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
        OBSERVER,
        COORDINATOR
    }
    public record Permission(
            String authorityName,
            Authority authority,
            Role role
    ){
    }


}
