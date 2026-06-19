package org.edu_sharing.restservices.assignment.v1.model;

public record SubmissionInfoRequest(
        Submission.Status status,
        String userNotes
) {
}
