package org.edu_sharing.restservices.assignment.v1.model;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

import java.util.Date;
import java.util.List;

/**
 * Represents a request to create an assignment. This record contains all the necessary information
 * to define an assignment, including metadata, status, type, permissions, and associated files.
 *
 * The `CreateAssignmentRequest` encapsulates essential parameters for creating a new assignment
 * within the system, ensuring validation and completeness of the data.
 *
 * @param id The unique identifier for the assignment. This may be automatically generated
 *           or user-provided depending on the system requirements.
 * @param title The title of the assignment. This field is mandatory and must not be empty.
 * @param summary A brief summary or description of the assignment. This field cannot be null.
 * @param endTime The optional deadline for the assignment, represented as a Date object.
 * @param status The current status of the assignment. Mandatory and must be one of the predefined
 *               statuses from the `Assignment.Status` enum.
 * @param type The type of the assignment, which defines its nature (e.g., DEFAULT or SUBMISSION).
 *             This field is mandatory and uses the `Assignment.Type` enum.
 * @param allowAdditionalDocumentSubmissions A flag indicating whether participants are permitted
 *                                          to submit additional documents for this assignment.
 *                                          This field is mandatory.
 * @param permissions A list of permissions associated with the assignment. Each `PermissionRequest`
 *                    specifies the authority and the role assigned to it. This field is mandatory
 *                    and cannot be null.
 * @param assignmentFiles A list of files associated with the assignment. Each `AssignmentFileRequest`
 *                        contains details about the file, its role, and completion status.
 *                        This field is mandatory and cannot be null.
 */
@Validated
public record CreateAssignmentRequest(
        String id,
        @NotEmpty
        String title,
        @NotNull
        String summary,
        Date endTime,
        @NotNull
        Assignment.Status status,
        @NotNull
        Assignment.Type type,
        @NotNull
        boolean allowAdditionalDocumentSubmissions,
        @NotNull
        List<PermissionRequest> permissions,
        @NotNull
        List<AssignmentFileRequest> assignmentFiles
) {
}

