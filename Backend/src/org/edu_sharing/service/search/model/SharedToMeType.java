package org.edu_sharing.service.search.model;

public enum SharedToMeType {
    // only directly shared to the specific authority
    Private,
    // shared to all authority groups of the authority (except GROUP_EVERYONE)
    All,
}
