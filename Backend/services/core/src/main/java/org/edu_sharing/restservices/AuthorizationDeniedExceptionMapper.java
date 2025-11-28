package org.edu_sharing.restservices;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.stereotype.Component;

@Component
public class AuthorizationDeniedExceptionMapper implements ExceptionMapper<AuthorizationDeniedException> {
    @Override
    public Response toResponse(AuthorizationDeniedException exception) {
        return Response.status(Response.Status.FORBIDDEN).entity(new ErrorResponse(exception)).build();
    }
}

