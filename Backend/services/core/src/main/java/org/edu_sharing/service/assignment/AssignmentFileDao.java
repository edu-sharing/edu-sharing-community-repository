package org.edu_sharing.service.assignment;

import org.edu_sharing.repository.server.tools.security.RunAsSystem;
import org.edu_sharing.repository.server.tools.transaction.RetryingTransaction;
import org.edu_sharing.restservices.assignment.v1.model.AssignmentFile;
import org.edu_sharing.restservices.assignment.v1.model.AssignmentFileRequest;

/**
 * Interface representing a Data Access Object (DAO) for managing AssignmentFile entities.
 * Provides methods for performing CRUD operations and retrieving metadata associated
 * with an AssignmentFile.
 */
public interface AssignmentFileDao extends BasicNodeDao {
    @RunAsSystem
    @RetryingTransaction
    void create(AssignmentFileRequest request);

    /**
     * Reloads the current state of the AssignmentFile from the underlying data storage.
     * This method ensures that any changes in the data store related to the AssignmentFile
     * are updated and reflected in the current object.
     */
    void refresh();

    /**
     * Retrieves the current AssignmentFile associated with this instance.
     *
     * @return the current AssignmentFile object, which contains details like the file reference,
     *         associated node information, document role, and completion status. Returns null if
     *         no AssignmentFile is associated.
     */
    AssignmentFile getAssignmentFile();


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

