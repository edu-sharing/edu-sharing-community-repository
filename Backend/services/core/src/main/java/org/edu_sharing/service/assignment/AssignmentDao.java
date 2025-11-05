package org.edu_sharing.service.assignment;

import org.edu_sharing.restservices.assignment.v1.model.Assignment;
import org.edu_sharing.restservices.assignment.v1.model.CreateAssignmentRequest;

import java.util.Date;
import java.util.List;

/**
 * Interface representing a Data Access Object (DAO) for managing assignments.
 * Provides methods for creating, updating, retrieving, checking existence,
 * refreshing, and deleting assignments, along with retrieving assignment details
 * such as creator, dates, files, and status.
 */
public interface AssignmentDao {
    /**
     * Creates a new assignment or updates an existing one based on the provided request.
     *
     * @param request the request object containing details about the assignment to create or update
     */
    void createOrUpdate(CreateAssignmentRequest request);

    /**
     * Reloads the current state of the assignment from the underlying data storage.
     * This method ensures that any changes to the assignment data in the data store
     * are reflected in the current object.
     */
    void refresh();

    /**
     * Checks if the assignment exists in the underlying data storage.
     *
     * @return true if the assignment exists, false otherwise
     */
    boolean exists();

    /**
     * Deletes the current assignment from the underlying data storage.
     * This method will remove any associated data or references to the assignment
     * in the storage system.
     *
     * It is expected that this method will only be called if a valid assignment exists.
     * If the assignment does not exist, the behavior is implementation-specific and
     * may result in a no-op or an error.
     */
    void delete();

    /**
     * Retrieves the current assignment associated with this instance.
     *
     * @return the current Assignment object, or null if no assignment exists
     */
    Assignment getAssignment();

    /**
     * Retrieves the unique identifier associated with the current node.
     *
     * @return a string representing the unique ID of the node
     */
    String getNodeId();


    /**
     * Retrieves the identifier of the creator associated with the assignment.
     *
     * @return a string representing the creator's identifier, or null if no creator is set
     */
    String getCreator();

    /**
     * Retrieves the last modified date of the assignment.
     *
     * @return a {@code Date} object representing the last modified date of the assignment,
     *         or {@code null} if no modification date is available
     */
    Date getModifiedDate();

    /**
     * Determines whether additional document submissions are allowed for the assignment.
     *
     * @return a {@code Boolean} indicating if additional document submissions are permitted.
     *         Returns {@code true} if allowed, {@code false} otherwise. May also return {@code null}
     *         if the value is not explicitly set.
     */
    Boolean getAllowAdditionalDocumentSubmissions();

    /**
     * Retrieves the type of the assignment.
     *
     * @return the {@link Assignment.Type} of the assignment, which can indicate specific assignment categories such as
     *         {@code DEFAULT} or {@code SUBMISSION}.
     */
    Assignment.Type getType();

    /**
     * Retrieves the current status of the assignment.
     *
     * @return the {@link Assignment.Status} representing the current state of the assignment,
     *         which can be {@code OPEN}, {@code PROGRESS}, {@code FINISHED}, or {@code CANCELED}.
     */
    Assignment.Status getStatus();

    /**
     * Retrieves the end date of the assignment.
     *
     * @return a {@code Date} object representing the end date of the assignment,
     *         or {@code null} if no end date is set.
     */
    Date getEndDate();

    /**
     * Retrieves the start date of the assignment.
     *
     * @return a {@code Date} object representing the start date of the assignment,
     *         or {@code null} if no start date is set.
     */
    Date getStartDate();

    /**
     * Retrieves the creation date of the assignment.
     *
     * @return a {@code Date} object representing the creation date of the assignment,
     *         or {@code null} if no creation date is available.
     */
    Date getCreateDate();

    /**
     * Retrieves the summary information of the assignment.
     *
     * @return a string representing the summary of the assignment, or null if no summary is set
     */
    String getSummary();

    /**
     * Retrieves the title of the assignment.
     *
     * @return a string representing the title of the assignment, or null if no title is set
     */
    String getTitle();

    /**
     * Retrieves a list of assignment files associated with the current assignment.
     *
     * @return a list of {@code AssignmentFileDao} objects representing the files linked to the assignment,
     *         or an empty list if no files are associated.
     */
    List<AssignmentFileDao> getAssignmentFiles();
}
