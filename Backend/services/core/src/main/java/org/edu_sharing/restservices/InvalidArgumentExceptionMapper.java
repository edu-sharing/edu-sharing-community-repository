package org.edu_sharing.restservices;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import org.alfresco.rest.framework.core.exceptions.InvalidArgumentException;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.springframework.stereotype.Component;

@Component
public class InvalidArgumentExceptionMapper implements ExceptionMapper<InvalidArgumentException> {

    @Override
    public Response toResponse(InvalidArgumentException e) {
        return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorResponse(e)).build();
    }
}
