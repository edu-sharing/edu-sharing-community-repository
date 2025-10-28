package org.edu_sharing.repository.server.jobs;


public class DuplicateJobException extends RuntimeException {
    public DuplicateJobException(String message, Throwable cause) {
        super(message, cause);
    }
}
