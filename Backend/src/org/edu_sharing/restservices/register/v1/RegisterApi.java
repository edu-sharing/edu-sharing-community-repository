package org.edu_sharing.restservices.register.v1;

import io.swagger.annotations.*;
import org.alfresco.service.cmr.repository.NodeRef;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.repository.server.tools.Edu_SharingProperties;
import org.edu_sharing.repository.server.tools.security.ShibbolethSessions;
import org.edu_sharing.repository.server.tools.security.ShibbolethSessions.SessionInfo;
import org.edu_sharing.restservices.ApiService;
import org.edu_sharing.restservices.RegisterDao;
import org.edu_sharing.restservices.RestConstants;
import org.edu_sharing.restservices.login.v1.model.Login;
import org.edu_sharing.restservices.login.v1.model.LoginCredentials;
import org.edu_sharing.restservices.login.v1.model.ScopeAccess;
import org.edu_sharing.restservices.register.v1.model.RegisterInformation;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.edu_sharing.service.authentication.ScopeAuthenticationService;
import org.edu_sharing.service.authentication.ScopeAuthenticationServiceFactory;
import org.edu_sharing.service.authentication.ScopeUserHomeServiceFactory;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.HashMap;

@Path("/register/v1")
@Api(tags = {"REGISTER v1"})
@ApiService(value="REGISTER", major=1, minor=0)
public class RegisterApi {

	@POST
	@Path("/register")
    @ApiOperation(
    	value = "Register a new user"
	)
    @ApiResponses(
    	value = { 
    		@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Void.class),
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class)
		})

    public Response register(@Context HttpServletRequest req,RegisterInformation info) {
		try{
			RegisterDao.register(info);
			return Response.ok().build();
		}
    	catch(Throwable t){
			return ErrorResponse.createResponse(t);
		}
    }
	@POST
	@Path("/resend/{mail}")
	@ApiOperation(
			value = "Resend a registration mail for a given mail address",
			notes = "The method will return false if there is no pending registration for the given mail"
	)
	@ApiResponses(
			value = {
					@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Boolean.class),
					@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class)
			})

	public Response resendMail(@Context HttpServletRequest req,
							 @ApiParam(value = "The mail a registration is pending for and should be resend to",required=true ) @PathParam("mail") String mail
	) {
		try{
			return Response.ok().entity(RegisterDao.resendMail(mail)).build();
		}
		catch(Throwable t){
			return ErrorResponse.createResponse(t);
		}
	}
	@POST
	@Path("/activate/{key}")
	@ApiOperation(
			value = "Activate a new user (by using a supplied key)"
	)
	@ApiResponses(
			value = {
					@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Void.class),
					@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class)
			})

	public Response activate(@Context HttpServletRequest req,
							 @ApiParam(value = "The key for the user to activate",required=true ) @PathParam("key") String key
							 ) {
		try{
			RegisterDao.activate(key);
			return Response.ok().build();
		}
		catch(Throwable t){
			return ErrorResponse.createResponse(t);
		}
	}
	@POST
	@Path("/recover/{mail}")
	@ApiOperation(
			value = "Send a mail to recover/reset password"
	)
	@ApiResponses(
			value = {
					@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Boolean.class),
					@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class)
			})

	public Response recoverPassword(@Context HttpServletRequest req,
							 @ApiParam(value = "The mail (authority) of the user to recover",required=true ) @PathParam("mail") String mail
	) {
		try{
			return Response.ok().entity(RegisterDao.recoverPassword(mail)).build();
		}
		catch(Throwable t){
			return ErrorResponse.createResponse(t);
		}
	}
}

