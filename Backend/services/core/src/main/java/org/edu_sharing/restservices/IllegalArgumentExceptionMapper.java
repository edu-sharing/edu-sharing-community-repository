package org.edu_sharing.restservices;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.springframework.stereotype.Component;

@Component
public class IllegalArgumentExceptionMapper implements ExceptionMapper<IllegalArgumentException> {

    @Override
    public Response toResponse(IllegalArgumentException e) {

        ErrorResponse errorResponse = new ErrorResponse(e);
        errorResponse.setMessage(e.getMessage());
        return Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).build();
    }
}
