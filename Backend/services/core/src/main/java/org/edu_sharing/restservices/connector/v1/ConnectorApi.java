package org.edu_sharing.restservices.connector.v1;

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
import org.edu_sharing.restservices.ConnectorDAO;
import org.edu_sharing.restservices.RestConstants;
import org.edu_sharing.restservices.connector.v1.model.ConnectorList;
import org.edu_sharing.restservices.shared.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;

@Path("/connector/v1")
@Tag(name= "CONNECTOR v1" )
@ApiService(value = "CONNECTOR", major = 1, minor = 0)
@Consumes({ "application/json" })
@Produces({"application/json"})
public class ConnectorApi {

	private static Logger logger = Logger.getLogger(ConnectorApi.class);

	@GET
	@Path("/connectors/{repository}/list")
	
	@Operation(summary = "List all available connectors")
	
	@ApiResponses(value = {
			@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = ConnectorList.class))),
	        @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
	        @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
	    })

	public Response listConnectors(
			@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-")) @PathParam("repository") String repository,
			@Context HttpServletRequest req) {

			try {
				return Response.status(Response.Status.OK).entity(ConnectorDAO.getConnectorList()).build();
		
			} catch (Throwable t) {
	    		return ErrorResponse.createResponse(t);
	    	}
	}

	@OPTIONS    
	@Path("/connectors/{repository}/list")
    @Hidden
	public Response options04() {
		return Response.status(Response.Status.OK).header("Allow", "OPTIONS, GET").build();
	}
}
