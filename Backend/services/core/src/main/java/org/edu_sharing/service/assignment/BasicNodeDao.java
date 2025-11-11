package org.edu_sharing.service.assignment;

import org.edu_sharing.restservices.shared.NodeRef;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

public interface BasicNodeDao {


    /**
     * Checks if the node exists in the underlying data storage.
     *
     * @return true if the object exists, false otherwise
     */
    boolean exists();

    /**
     * Deletes the current node from the underlying data storage.
     * This method will remove any associated data or references to the node
     * in the storage system.
     *
     * It is expected that this method will only be called if a valid node exists.
     * If the node does not exist, the behavior is implementation-specific and
     * may result in a no-op or an error.
     */
    void delete();


    /**
     * Retrieves the unique identifier associated with the current node.
     *
     * @return a string representing the unique ID of the node
     */
    String getNodeId();


    /**
     * Retrieves the identifier of the creator associated with the node.
     *
     * @return a string representing the creator's identifier, or null if no creator is set
     */
    String getCreator();

    /**
     * Retrieves the last modified date of the node.
     *
     * @return a {@code Date} object representing the last modified date of the node,
     *         or {@code null} if no modification date is available
     */
    Date getModifiedDate();

    /**
     * Retrieves the creation date of the node.
     *
     * @return a {@code Date} object representing the creation date of the node,
     *         or {@code null} if no creation date is available.
     */
    Date getCreateDate();

    @NotNull NodeRef getNodeRef();
}
