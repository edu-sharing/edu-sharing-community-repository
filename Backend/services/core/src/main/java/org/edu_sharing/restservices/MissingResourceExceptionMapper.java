package org.edu_sharing.restservices;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.springframework.stereotype.Component;


@Component
public class MissingResourceExceptionMapper implements ExceptionMapper<MissingResourceException> {
    @Override
    public Response toResponse(MissingResourceException e) {
        return Response.status(Response.Status.NOT_FOUND).entity(new ErrorResponse(e)).build();
    }
}

