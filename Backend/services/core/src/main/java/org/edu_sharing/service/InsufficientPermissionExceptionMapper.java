package org.edu_sharing.service;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import org.edu_sharing.restservices.DAOException;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.springframework.stereotype.Component;

@Component
public class InsufficientPermissionExceptionMapper implements ExceptionMapper<InsufficientPermissionException> {

	@Override
	public Response toResponse(InsufficientPermissionException e) {
		return ErrorResponse.createResponse(DAOException.mapping(e));
	}
}
