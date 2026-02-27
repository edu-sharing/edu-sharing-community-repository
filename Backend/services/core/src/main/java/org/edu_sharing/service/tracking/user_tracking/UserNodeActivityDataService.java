package org.edu_sharing.service.tracking.user_tracking;

import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.List;

/**
 * Service interface for retrieving data related to user activities on nodes.
 */
public interface UserNodeActivityDataService {
    /**
     * Retrieves a list of user activities across all users within a specified date range.
     * The result is limited to the specified number of records.
     *
     * @param after the start date to filter activities (exclusive) must not be null
     * @param until the end date to filter activities (inclusive), can be null to include all future activities
     * @param limit the maximum number of activities to retrieve
     * @return a list of {@code UserNodeActivity} objects representing the user activities
     *         within the specified date range, or an empty list if no activities are found
     */
    @NotNull List<UserNodeActivity> getDataForAllUsers(@NotNull Date after,  Date until, int limit);

    /**
     * Retrieves a list of user node activity records associated with the specified username
     * that were performed after the provided date.
     *
     * @param username the unique username of the user whose activity data is to be retrieved
     * @param after the date after which the activities should be retrieved
     * @return a list of UserNodeActivity objects representing the user's activities after the specified date,
     *         or an empty list if no activities are found
     */
    @NotNull
    List<UserNodeActivity> getDataForUser(@NotNull String username, @NotNull Date after);
}
