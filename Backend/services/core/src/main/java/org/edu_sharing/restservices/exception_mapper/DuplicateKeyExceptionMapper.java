package org.edu_sharing.restservices.exception_mapper;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

@Component
public class DuplicateKeyExceptionMapper implements ExceptionMapper<DuplicateKeyException> {
    @Override
    public Response toResponse(DuplicateKeyException e) {
        return Response.status(Response.Status.CONFLICT).entity(new ErrorResponse(e)).build();
    }
}
