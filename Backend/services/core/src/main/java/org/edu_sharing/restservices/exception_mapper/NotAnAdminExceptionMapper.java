package org.edu_sharing.restservices.exception_mapper;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.edu_sharing.service.NotAnAdminException;
import org.springframework.stereotype.Component;

@Component
public class NotAnAdminExceptionMapper implements ExceptionMapper<NotAnAdminException> {
    @Override
    public Response toResponse(NotAnAdminException exception) {
        return Response.status(Response.Status.FORBIDDEN).entity(new ErrorResponse(exception)).build();
    }
}
