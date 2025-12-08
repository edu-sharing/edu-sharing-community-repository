package org.edu_sharing.restservices.assignment.v1.model;

import org.springframework.validation.annotation.Validated;

@Validated
public record SubmissionFileValidationRequest(
        Submission.Status validationStatus
) {
}
