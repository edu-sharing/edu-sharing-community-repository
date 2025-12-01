package org.edu_sharing.restservices.exception_mapper;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.rest.framework.core.exceptions.InvalidArgumentException;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class InvalidArgumentExceptionMapper implements ExceptionMapper<InvalidArgumentException> {

    @Override
    public Response toResponse(InvalidArgumentException e) {
        log.error(e.getMessage(), e);
        return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorResponse(e)).build();
    }
}
