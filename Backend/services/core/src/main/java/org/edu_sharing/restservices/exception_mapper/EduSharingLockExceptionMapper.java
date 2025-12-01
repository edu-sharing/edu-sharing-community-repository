package org.edu_sharing.restservices.exception_mapper;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import org.edu_sharing.repository.server.tools.EduSharingLockException;
import org.edu_sharing.restservices.ExtendedStatus;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.springframework.stereotype.Component;

@Component
public class EduSharingLockExceptionMapper implements ExceptionMapper<EduSharingLockException> {
    @Override
    public Response toResponse(EduSharingLockException e) {
        return Response.status(ExtendedStatus.LOCKED).entity(new ErrorResponse(e)).build();
    }
}

