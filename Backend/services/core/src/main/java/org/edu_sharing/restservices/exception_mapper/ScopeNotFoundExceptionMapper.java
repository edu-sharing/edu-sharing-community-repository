package org.edu_sharing.restservices.exception_mapper;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.edu_sharing.service.scope.ScopeNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class ScopeNotFoundExceptionMapper implements ExceptionMapper<ScopeNotFoundException> {

    @Override
    public Response toResponse(ScopeNotFoundException e) {
        return Response.status(Response.Status.NOT_FOUND).entity(new ErrorResponse(e)).build();
    }
}
