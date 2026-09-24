package org.edu_sharing.restservices;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import net.sf.acegisecurity.AuthenticationCredentialsNotFoundException;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationCredentialsNotFoundExceptionMapper implements ExceptionMapper<AuthenticationCredentialsNotFoundException> {

    @Override
    public Response toResponse(AuthenticationCredentialsNotFoundException e) {

        return Response.status(Response.Status.FORBIDDEN).entity(new ErrorResponse(e)).build();
    }
}
