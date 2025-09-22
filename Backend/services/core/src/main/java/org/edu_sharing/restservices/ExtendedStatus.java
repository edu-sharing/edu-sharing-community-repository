package org.edu_sharing.restservices;

import jakarta.ws.rs.core.Response;

public enum ExtendedStatus implements Response.StatusType {
    LOCKED(423, "Locked");



    private final int code;
    private final String reason;
    private final Response.Status.Family family;

    ExtendedStatus(final int statusCode, final String reasonPhrase) {
        this.code = statusCode;
        this.reason = reasonPhrase;
        this.family = Response.Status.Family.familyOf(statusCode);
    }

    @Override
    public int getStatusCode() {
        return code;
    }

    @Override
    public String getReasonPhrase() {
        return reason;
    }

    @Override
    public Response.Status.Family getFamily() {
        return family;
    }

    @Override
    public String toString() {
        return reason;
    }
}
