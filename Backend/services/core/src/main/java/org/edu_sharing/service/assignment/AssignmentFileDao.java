package org.edu_sharing.service.assignment;

import org.edu_sharing.restservices.assignment.v1.model.AssignmentFile;
import org.edu_sharing.restservices.assignment.v1.model.AssignmentFileRequest;

/**
 * Interface representing a Data Access Object (DAO) for managing AssignmentFile entities.
 * Provides methods for performing CRUD operations and retrieving metadata associated
 * with an AssignmentFile.
 */
public interface AssignmentFileDao {
    /**
     * Reloads the current state of the AssignmentFile from the underlying data storage.
     * This method ensures that any changes in the data store related to the AssignmentFile
     * are updated and reflected in the current object.
     */
    void refresh();

    /**
     * Checks if the corresponding entity or resource exists in the underlying data storage.
     *
     * @return true if the entity exists, false otherwise
     */
    boolean exists();

    /**
     * Retrieves the current AssignmentFile associated with this instance.
     *
     * @return the current AssignmentFile object, which contains details like the file reference,
     *         associated node information, document role, and completion status. Returns null if
     *         no AssignmentFile is associated.
     */
    AssignmentFile getAssignmentFile();

    /**
     * Deletes the current AssignmentFile from the underlying data storage.
     * This operation removes any associated data or references related to
     * the AssignmentFile in the data store.
     *
     * The behavior of this method depends on the implementation; if the
     * AssignmentFile does not exist, it might result in a no-op or an error.
     * It is expected that this method is called only when a valid AssignmentFile exists.
     */
    void delete();

    /**
     * Retrieves the reference node identifier associated with the current AssignmentFile.
     *
     * @return a string representing the reference node ID, or null if no reference node is set
     */
    String getReferNodeId();

    /**
     * Updates the AssignmentFile entity with the new data provided in the
     * AssignmentFileRequest object. This method is responsible for modifying
     * the stored AssignmentFile attributes such as reference ID, document role,
     * and completion status to match the provided values.
     *
     * @param assignmentFileRequest the request object containing the updated
     *                              data for the AssignmentFile. It includes
     *                              the reference ID, document role, and
     *                              completion status, all of which are mandatory fields.
     */
    void update(AssignmentFileRequest assignmentFileRequest);

    /**
     * Retrieves the unique identifier for a node.
     *
     * @return a string representing the unique node ID
     */
    String getNodeId();

    /**
     * Determines whether the AssignmentFile is marked as completed or finished.
     *
     * @return a {@code Boolean} indicating the completion status of the AssignmentFile.
     *         Returns {@code true} if the AssignmentFile is completed, {@code false} otherwise.
     *         May also return {@code null} if the status is not explicitly set.
     */
    Boolean isDone();

    /**
     * Retrieves the role of the document associated with the current AssignmentFile.
     *
     * @return the document role as an {@code AssignmentFile.Role} enumeration value.
     *         Possible values include {@code SUPPLEMENTARY} and {@code SUBMITTABLE}.
     */
    AssignmentFile.Role getDocumentRole();
}

