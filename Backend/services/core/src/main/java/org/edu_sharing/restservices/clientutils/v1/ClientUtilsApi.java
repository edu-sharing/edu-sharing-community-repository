package org.edu_sharing.restservices.clientutils.v1;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.log4j.Logger;
import org.edu_sharing.restservices.ApiService;
import org.edu_sharing.restservices.RestConstants;
import org.edu_sharing.restservices.clientutils.v1.model.WebsiteInformation;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.edu_sharing.service.clientutils.ClientUtilsService;

import jakarta.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

@Path("/clientUtils/v1")
@Tag(name="CLIENTUTILS v1")
@ApiService(value="CLIENTUTILS", major=1, minor=0)
@Consumes({ "application/json" })
@Produces({"application/json"})
public class ClientUtilsApi  {

	
	private static Logger logger = Logger.getLogger(ClientUtilsApi.class);
	
    @GET
    @Path("/getWebsiteInformation")        
    @Operation(summary = "Read generic information about a webpage")
    
    @ApiResponses(
    		value = { 
    		        @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = WebsiteInformation.class))),        
    		        @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
    		        @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
    		        @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
    		        @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
    		        @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
	    })

    public Response getWebsiteInformation(
    	
    	@Parameter(description = "full url with http or https" ) @QueryParam("url") String url,
		@Context HttpServletRequest req) {
    	
    	try {
    		
	    	WebsiteInformation info=new WebsiteInformation(ClientUtilsService.getWebsiteInformation(url));
	    	return Response.status(Response.Status.OK).entity(info).build();
	
    	}  catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}

    }
	@OPTIONS    
    @Path("/getWebsiteInformation")
    @Hidden

	public Response options01() {
		
		return Response.status(Response.Status.OK).header("Allow", "OPTIONS, GET").build();
	}
    
}

