package org.edu_sharing.repository.server.tools;

public class HttpException extends RuntimeException {
    private final int statusCode;
    private final String message;

    public HttpException(int statusCode, String message) {
        this.statusCode=statusCode;
        this.message=message;
    }

    public int getStatusCode() {
        return statusCode;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
