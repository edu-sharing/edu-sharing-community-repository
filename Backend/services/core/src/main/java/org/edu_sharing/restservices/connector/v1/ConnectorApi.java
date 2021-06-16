package org.edu_sharing.restservices.connector.v1;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.edu_sharing.restservices.ApiService;
import org.edu_sharing.restservices.ConnectorDAO;
import org.edu_sharing.restservices.RestConstants;
import org.edu_sharing.restservices.connector.v1.model.Connector;
import org.edu_sharing.restservices.connector.v1.model.ConnectorList;
import org.edu_sharing.restservices.shared.ErrorResponse;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Path("/connector/v1")
@Api(tags = { "CONNECTOR v1" })
@ApiService(value = "CONNECTOR", major = 1, minor = 0)
public class ConnectorApi {

	private static Logger logger = Logger.getLogger(ConnectorApi.class);

	@GET
	@Path("/connectors/{repository}/list")
	
	@ApiOperation(value = "List all available connectors")
	
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = ConnectorList.class),
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })

	public Response listConnectors(
			@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID, required = true, defaultValue = "-home-") @PathParam("repository") String repository,
			@Context HttpServletRequest req) {

			try {
				return Response.status(Response.Status.OK).entity(ConnectorDAO.getConnectorList()).build();
		
			} catch (Throwable t) {
	    		return ErrorResponse.createResponse(t);
	    	}
	}

	@OPTIONS    
	@Path("/connectors/{repository}/list")
    @ApiOperation(hidden = true, value = "")
	public Response options04() {
		return Response.status(Response.Status.OK).header("Allow", "OPTIONS, GET").build();
	}
}
