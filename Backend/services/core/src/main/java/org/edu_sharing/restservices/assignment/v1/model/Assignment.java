package org.edu_sharing.restservices.assignment.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.edu_sharing.restservices.shared.Authority;
import org.edu_sharing.restservices.shared.NodeRef;
import org.edu_sharing.restservices.shared.UserSimple;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.List;


/**
 * Represents an assignment with associated metadata, statuses, and permissions.
 * This record serves as the primary structure for handling assignments.
 *
 * @param ref                                The unique identifier of the assignment represented by a NodeRef.
 *                                           This field is mandatory.
 * @param title                              The title of the assignment. This field is mandatory.
 * @param summary                            The optional summary or description of the assignment.
 * @param creator                            The creator of the assignment, represented by a UserSimple object.
 *                                           This field is mandatory.
 * @param created                            The mandatory date of creation for the assignment.
 * @param endTime                            The optional end time of the assignment.
 * @param status                             The current status of the assignment. This field is mandatory
 *                                           and based on predefined statuses in the Status enum.
 * @param type                               The type of the assignment (e.g., DEFAULT, SUBMISSION).
 *                                           This is mandatory and defined in the Type enum.
 * @param allowAdditionalDocumentSubmissions A flag indicating whether additional document submissions are allowed
 *                                           for the assignment. This field is mandatory.
 * @param modified                           The optional last modified timestamp of the assignment.
 * @param permissions                        A list of permissions associated with the assignment.
 *                                           Each permission maps a specific Authority to a Role.
 *                                           This field is mandatory.
 * @param submissions                        A list of submissions associated with this assignment.
 *                                           This field is mandatory.
 */
public record Assignment(
        @NotNull
        @JsonProperty(required = true)
        NodeRef ref,
        @NotNull
        @JsonProperty(required = true)
        String title,
        String summary,
        @NotNull
        @JsonProperty(required = true)
        UserSimple creator,
        @NotNull
        @JsonProperty(required = true)
        Date created,
        Date endTime,

        /**
         * @TODO: We also need an personalStatus or similar field which reflects the status for the current fetching
         * user. I.e. if he already submitted his data if it is of type SUBMITTABLE
         */

        @NotNull
        @JsonProperty(required = true)
        Status status,
        @NotNull
        @JsonProperty(required = true)
        Type type,
        @JsonProperty(required = true)
        boolean allowAdditionalDocumentSubmissions,
        Date modified,
        @NotNull
        @JsonProperty(required = true)
        List<Permission> permissions,
        @NotNull
        @JsonProperty(required = true)
        List<Submission> submissions
) {

    @Schema(description = """
            Status of the assignment
            * DRAFT: Assignment is in draft state, only visible to creator
            * ASSIGNED: Assignment is assigned and visible to all users with assignee permission
            * FINISHED: Assignment has been completed
            * CANCELED: Assignment has been canceled
            """
    )
    public enum Status {
        DRAFT,
        INPROGRESS,
        FINISHED,
        CANCELED
    }

    @Schema(description = """
            Type of the assignment
            * DEFAULT: Standard assignment type without specific submission requirements
            * SUBMISSION: Assignment that requires participants to submit documents or materials
            """
    )
    public enum Type {
        DEFAULT,
        SUBMISSION,
    }


    @Schema(description = """
            Role within an assignment context
            * ASSIGNEE: User who is assigned to complete or participate in the assignment (only valid for assignments of type SUBMISSION)
            * COORDINATOR: User who can manage and oversee the assignment, including monitoring progress and managing participants
            """
    )
    public enum Role {
        ASSIGNEE,
        COORDINATOR
    }

    /**
     * Represents a permission assigned to an entity within the context of an assignment.
     * Permissions define specific roles and their association with an authority, which
     * may represent a user, group, or system entity. This class is used to encapsulate
     * the relationship between an authority and a role, ensuring access control and role
     * management within assignment operations.
     *
     * @param authority The authority to which the permission applies. It represents
     *                  the entity (e.g., user or group) associated with this permission.
     *                  This field is mandatory.
     * @param role      The role assigned to the authority, determining the set of
     *                  actions or responsibilities granted. This field is mandatory.
     */
    public record Permission(
            @NotNull
            @JsonProperty(required = true)
            Authority authority,
            @NotNull
            @JsonProperty(required = true)
            Role role
    ) {
    }
}
