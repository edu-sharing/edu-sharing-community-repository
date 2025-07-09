package org.edu_sharing.service.tracking;

public enum UserActivityEventType {
    LOGIN_USER_SESSION,
    LOGIN_USER_OAUTH_PASSWORD,
    LOGIN_USER_OAUTH_REFRESH_TOKEN,
    LOGOUT_USER_TIMEOUT,
    LOGOUT_USER_REGULAR
}
