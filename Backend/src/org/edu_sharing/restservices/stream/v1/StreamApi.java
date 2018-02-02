package org.edu_sharing.restservices.stream.v1;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
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
import org.edu_sharing.restservices.stream.v1.model.StreamEntryInput;
import org.edu_sharing.restservices.stream.v1.model.StreamList;

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
     @Path("/search/{repository}")
	        
     @ApiOperation(
    	value = "Get the stream content for the current user with the given status."
    	)
    
     @ApiResponses(
    		value = { 
    		        @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = StreamList.class),        
    		        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
    		        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
    		        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
    		        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
    		        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })

	    public Response search(
	    	@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
	    	@ApiParam(value = "Stream object status to search for",required=false ) @QueryParam("status") String status,
	    	@ApiParam(value = "category to search for",required=false ) @QueryParam("category") String category,
	    	@ApiParam(value = "generic text to search for (in category or description)",required=false ) @QueryParam("query") String query,
		    @ApiParam(value = RestConstants.MESSAGE_MAX_ITEMS, defaultValue=""+RestConstants.DEFAULT_MAX_ITEMS) @QueryParam("maxItems") Integer maxItems,
		    @ApiParam(value = RestConstants.MESSAGE_SKIP_COUNT, defaultValue="0") @QueryParam("skipCount") Integer skipCount,
			@Context HttpServletRequest req) {
	    	
	    	try {
	    		RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    		StreamList response = StreamDao.search(repoDao,status,category,query, skipCount,maxItems);
		    	return Response.status(Response.Status.OK).entity(response).build();
	    	}catch(Throwable t) {
	    		return ErrorResponse.createResponse(t);
	    	}
	 }
	 
	 @GET
     @Path("/categories/{repository}")
	        
     @ApiOperation(
    	value = "Get the top categories"
    	)
    
     @ApiResponses(
    		value = { 
    		        @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Map.class),        
    		        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
    		        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
    		        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
    		        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
    		        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })

	    public Response getCategories(
	    	@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
			@Context HttpServletRequest req) {
	    	
	    	try {
	    		RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    		Map<String, Number> response = StreamDao.getCategories(repoDao);
		    	return Response.status(Response.Status.OK).entity(response).build();
	    	}catch(Throwable t) {
	    		return ErrorResponse.createResponse(t);
	    	}
	 }
	 
	 @PUT
     @Path("/add/{repository}")
	        
     @ApiOperation(
    	value = "add a new stream object.",
    	notes = "will return the object and add the id to the object if creation succeeded"
    	)
    
     @ApiResponses(
    		value = { 
    		        @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = StreamEntryInput.class),        
    		        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
    		        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
    		        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
    		        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
    		        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })

	    public Response addEntry(
	    	@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
	    	@ApiParam(value = "Stream object to add",required=true ) StreamEntryInput entry,
			@Context HttpServletRequest req) {
	    	try {
	    		RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    		StreamEntryInput result = StreamDao.addEntry(repoDao, entry);
		    	return Response.status(Response.Status.OK).entity(result).build();
	    	}catch(Throwable t) {
	    		return ErrorResponse.createResponse(t);
	    	}
	 }
	 @PUT
     @Path("/status/{repository}/{entry}")
	        
     @ApiOperation(
    	value = "update status for a stream object and authority"
    	)
    
     @ApiResponses(
    		value = { 
    		        @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Void.class),        
    		        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
    		        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
    		        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
    		        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
    		        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })

	    public Response addEntry(
	    	@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
	    	@ApiParam(value = "entry id to update",required=true ) @PathParam("entry") String entryId,
	    	@ApiParam(value = "authority to set/change status",required=true ) @QueryParam("authority") String authority,
	    	@ApiParam(value = "New status for this authority",required=true ) @QueryParam("status") String status,
			@Context HttpServletRequest req) {
	    	try {
	    		RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    		StreamDao.updateStatus(repoDao, entryId, authority, status);
		    	return Response.status(Response.Status.OK).build();
	    	}catch(Throwable t) {
	    		return ErrorResponse.createResponse(t);
	    	}
	 }
}
