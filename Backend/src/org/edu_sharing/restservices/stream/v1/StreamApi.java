package org.edu_sharing.restservices.stream.v1;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.edu_sharing.restservices.ApiService;
import org.edu_sharing.restservices.RepositoryDao;
import org.edu_sharing.restservices.RestConstants;
import org.edu_sharing.restservices.StreamDao;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.edu_sharing.restservices.shared.SearchResult;
import org.edu_sharing.restservices.stream.v1.model.StreamEntry;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;


@Path("/stream/v1")
@Api(tags = {"STREAM v1"})
@ApiService(value="STREAM", major=1, minor=0)
public class StreamApi {

	private static Logger logger = Logger.getLogger(StreamApi.class);
	
	 @GET
     @Path("/search/{repository}/{status}")
	        
     @ApiOperation(
    	value = "Get the stream content for the current user with the given status."
    	)
    
     @ApiResponses(
    		value = { 
    		        @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = SearchResult.class),        
    		        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
    		        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
    		        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
    		        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
    		        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })

	    public Response search(
	    	@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
	    	@ApiParam(value = "Stream object status to search for",required=false ) @QueryParam("status") @PathParam("status") String status,
		    @ApiParam(value = RestConstants.MESSAGE_MAX_ITEMS, defaultValue=""+RestConstants.DEFAULT_MAX_ITEMS) @QueryParam("maxItems") Integer maxItems,
		    @ApiParam(value = RestConstants.MESSAGE_SKIP_COUNT, defaultValue="0") @QueryParam("skipCount") Integer skipCount,
			@Context HttpServletRequest req) {
	    	
	    	try {
	    		RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    		SearchResult<StreamEntry> response = StreamDao.search(repoDao,status, skipCount,maxItems);
		    	return Response.status(Response.Status.OK).entity(response).build();
	    	}catch(Throwable t) {
	    		return ErrorResponse.createResponse(t);
	    	}
	 }
}
