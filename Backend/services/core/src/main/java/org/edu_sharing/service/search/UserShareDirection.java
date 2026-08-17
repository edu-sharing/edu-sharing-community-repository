package org.edu_sharing.service.search;

public enum UserShareDirection {
    fromUser,
    toUser,
    toUserGroups,
    /**
     * all shares to the user (or one of the user's groups) that the user has rejected (hidden).
     * These are filtered out of all other directions.
     */
    rejectedByUser,
}
