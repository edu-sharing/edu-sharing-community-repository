package org.edu_sharing.restservices.rating.v1;

import io.swagger.annotations.*;
import org.apache.log4j.Logger;
import org.edu_sharing.restservices.*;
import org.edu_sharing.restservices.shared.ErrorResponse;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

@Path("/rating/v1")
@Api(tags = { "RATING v1" })
@ApiService(value = "RATING", major = 1, minor = 0)
public class RatingApi {
	private static Logger logger = Logger.getLogger(RatingApi.class);
	@PUT
	@Path("/ratings/{repository}/{node}")
	@ApiOperation(value = "create or update a rating", notes = "Adds the rating. If the current user already rated that element, the rating will be altered")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Void.class),
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })
	public Response addOrUpdateRating(
			@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
	    	@ApiParam(value = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
	    	@ApiParam(value = "The rating (usually in range 1-5)",required=true) @QueryParam("rating") double rating,
	    	@ApiParam(value = "Text content of rating",required=true) String comment,
			@Context HttpServletRequest req) {
    	try {
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	new RatingDao(repoDao).addOrUpdateRating(node, rating, comment);
	    	return Response.status(Response.Status.OK).build();
    	} catch (Throwable t) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(t)).build();
		}
    }
	@DELETE
	@Path("/ratings/{repository}/{node}")
	@ApiOperation(value = "delete a comment", notes = "Delete the comment with the given id")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Void.class),
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })
	public Response deleteRating(
			@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
			@ApiParam(value = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
			@Context HttpServletRequest req) {
    	try {
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	new RatingDao(repoDao).deleteRating(node);
	    	return Response.status(Response.Status.OK).build();
    	} catch (Throwable t) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(t)).build();
		}
    }
}
