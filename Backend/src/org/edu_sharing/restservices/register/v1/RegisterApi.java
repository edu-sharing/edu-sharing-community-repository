package org.edu_sharing.restservices.register.v1;

import io.swagger.annotations.*;
import org.edu_sharing.restservices.ApiService;
import org.edu_sharing.restservices.RegisterDao;
import org.edu_sharing.restservices.RestConstants;
import org.edu_sharing.restservices.register.v1.model.RegisterExists;
import org.edu_sharing.restservices.register.v1.model.RegisterInformation;
import org.edu_sharing.restservices.shared.ErrorResponse;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

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
					@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Void.class),
					@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class)
			})

	public Response resendMail(@Context HttpServletRequest req,
							 @ApiParam(value = "The mail a registration is pending for and should be resend to",required=true ) @PathParam("mail") String mail
	) {
		try{
			RegisterDao.resendMail(mail);
			return Response.ok().build();
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
	@GET
	@Path("/exists/{mail}")
	@ApiOperation(
			value = "Check if the given mail is already successfully registered"
	)
	@ApiResponses(
			value = {
					@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = RegisterExists.class),
					@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class)
			})

	public Response mailExists(@Context HttpServletRequest req,
									@ApiParam(value = "The mail (authority) of the user to check",required=true ) @PathParam("mail") String mail
	) {
		try{
			RegisterExists result = RegisterDao.mailExists(mail);
			return Response.ok().entity(result).build();
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
					@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Void.class),
					@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class)
			})

	public Response recoverPassword(@Context HttpServletRequest req,
							 @ApiParam(value = "The mail (authority) of the user to recover",required=true ) @PathParam("mail") String mail
	) {
		try{
			RegisterDao.recoverPassword(mail);
			return Response.ok().build();
		}
		catch(Throwable t){
			return ErrorResponse.createResponse(t);
		}
	}
	@POST
	@Path("/reset/{key}/{password}")
	@ApiOperation(
			value = "Send a mail to recover/reset password"
	)
	@ApiResponses(
			value = {
					@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Void.class),
					@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class)
			})

	public Response resetPassword(@Context HttpServletRequest req,
									@ApiParam(value = "The key for the password reset request",required=true ) @PathParam("key") String key,
									@ApiParam(value = "The new password for the user",required=true ) @PathParam("password") String password
	) {
		try{
			RegisterDao.resetPassword(key,password);
			return Response.ok().build();
		}
		catch(Throwable t){
			return ErrorResponse.createResponse(t);
		}
	}
}

