package org.edu_sharing.rest.notification.data;

public enum StatusDTO {
    /**
     * waits to get send
     */
    PENDING,
    /**
     * notification was sent
     */
    SENT,
    /**
     * was read
     */
    READ,
    /**
     * was disabled
     */
    IGNORED,
}
