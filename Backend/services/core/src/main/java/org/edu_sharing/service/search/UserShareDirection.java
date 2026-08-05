package org.edu_sharing.service.search;

public enum UserShareDirection {
    fromUser,
    toUser,
    toUserGroups,
    // @TODO: add a "rejectedByUser" to search for all shares with current status REJECTED for this user
}
