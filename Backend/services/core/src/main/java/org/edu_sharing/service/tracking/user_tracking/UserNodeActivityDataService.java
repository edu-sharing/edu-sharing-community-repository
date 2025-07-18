package org.edu_sharing.service.tracking.user_tracking;

import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Date;
import java.util.List;

/**
 * Service interface for retrieving data related to user activities on nodes.
 */
public interface UserNodeActivityDataService {
    @NotNull Page<UserNodeActivity> getDataForAllUsers(@NotNull Date after, int skip, int limit);

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
