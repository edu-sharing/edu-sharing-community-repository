package org.edu_sharing.repository.server.tools;

public class EduSharingLockException extends RuntimeException {
    public EduSharingLockException(String message) {
        super(message);
    }
    public EduSharingLockException(String message, Throwable cause) {
        super(message, cause);
    }
}
