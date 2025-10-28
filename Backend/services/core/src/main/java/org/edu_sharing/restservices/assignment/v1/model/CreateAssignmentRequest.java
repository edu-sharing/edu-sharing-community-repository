package org.edu_sharing.restservices.assignment.v1.model;

import java.util.Date;
import java.util.List;

public record CreateAssignmentRequest(
        String id,
        String title,
        String summary,
        Date startTime,
        Date endTime,
        Assignment.Status status,
        Assignment.Type type,
        boolean alloDelayedSubmission,
        boolean allowAdditionalDocumentSubmission,
        List<Assignment.Permission> permissions
) {
}

