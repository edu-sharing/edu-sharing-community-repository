package org.edu_sharing.restservices.assignment.v1.model;

public record EditSubmissionRequest(
        String validationNotes,
        String feedback,
        Submission.Status validationStatus
) {
}
