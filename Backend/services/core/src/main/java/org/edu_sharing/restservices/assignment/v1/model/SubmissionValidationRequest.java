package org.edu_sharing.restservices.assignment.v1.model;

public record SubmissionValidationRequest(
        String validationNotes,
        String feedback,
        Submission.Status validationStatus
) {
}
