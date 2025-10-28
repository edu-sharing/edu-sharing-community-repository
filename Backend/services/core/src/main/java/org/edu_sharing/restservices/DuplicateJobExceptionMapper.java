package org.edu_sharing.restservices;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import org.edu_sharing.repository.server.jobs.DuplicateJobException;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.springframework.stereotype.Component;

@Component
public class DuplicateJobExceptionMapper implements ExceptionMapper<DuplicateJobException> {
    @Override
    public Response toResponse(DuplicateJobException e) {
        return Response.status(Response.Status.CONFLICT).entity(new ErrorResponse(e)).build();
    }
}
