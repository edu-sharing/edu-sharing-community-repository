package org.edu_sharing.restservices.exception_mapper;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.edu_sharing.service.scope.ScopesDisabledException;
import org.springframework.stereotype.Component;

@Component
public class ScopesDisabledExceptionMapper implements ExceptionMapper<ScopesDisabledException> {

    @Override
    public Response toResponse(ScopesDisabledException e) {
        return Response.status(Response.Status.NOT_FOUND).entity(new ErrorResponse(e)).build();
    }
}
