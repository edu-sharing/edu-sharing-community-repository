package org.edu_sharing.restservices.exception_mapper;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import org.alfresco.error.AlfrescoRuntimeException;
import org.edu_sharing.restservices.DAOException;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.springframework.stereotype.Component;

@Component
public class AlfrescoRuntimeExceptionMapper implements ExceptionMapper<AlfrescoRuntimeException> {
    @Override
    public Response toResponse(AlfrescoRuntimeException exception) {
        return ErrorResponse.createResponse(DAOException.mapping(exception));
    }
}
