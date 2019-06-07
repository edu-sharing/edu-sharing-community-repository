package org.edu_sharing.restservices.stream.v1;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
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
import org.edu_sharing.service.search.model.SortDefinition;


@Path("/stream/v1")
@Api(tags = {"STREAM v1"})
@ApiService(value="STREAM", major=1, minor=0)
public class StreamApi {

	private static Logger logger = Logger.getLogger(StreamApi.class);
	
	 @POST
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
	    	@ApiParam(value = "generic text to search for (in title or description)",required=false ) @QueryParam("query") String query,
		    @ApiParam(value = RestConstants.MESSAGE_MAX_ITEMS, defaultValue=""+RestConstants.DEFAULT_MAX_ITEMS) @QueryParam("maxItems") Integer maxItems,
		    @ApiParam(value = RestConstants.MESSAGE_SKIP_COUNT, defaultValue="0") @QueryParam("skipCount") Integer skipCount,
		    @ApiParam(value = "map with property + value to search", defaultValue="0") Map<String,String> properties,
		    @ApiParam(value = RestConstants.MESSAGE_SORT_PROPERTIES+", currently supported: created, priority, default: priority desc, created desc") @QueryParam("sortProperties") List<String> sortProperties,
		    @ApiParam(value = RestConstants.MESSAGE_SORT_ASCENDING) @QueryParam("sortAscending") List<Boolean> sortAscending,
			@Context HttpServletRequest req) {
	    	
	    	try {
	    		RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    		StreamList response = StreamDao.search(repoDao,status,properties,query, skipCount,maxItems,new SortDefinition(sortProperties,sortAscending));
		    	return Response.status(Response.Status.OK).entity(response).build();
	    	}catch(Throwable t) {
	    		return ErrorResponse.createResponse(t);
	    	}
	 }
	 
	 @GET
     @Path("/properties/{repository}/{property}")
	        
     @ApiOperation(
    	value = "Get top values for a property"
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

	    public Response getPropertyValues(
	    	@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
	    	@ApiParam(value = "The property to aggregate",required=true) @PathParam("property") String property,
			@Context HttpServletRequest req) {
	    	
	    	try {
	    		RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    		Map<String, Number> response = StreamDao.getTopValues(repoDao,property);
		    	return Response.status(Response.Status.OK).entity(response).build();
	    	}catch(Throwable t) {
	    		return ErrorResponse.createResponse(t);
	    	}
	 }
	 @GET
     @Path("/access/{repository}/{node}")
	        
     @ApiOperation(
    		 value = "test"
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

	    public Response canAccess(
	    	@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
	    	@ApiParam(value = "The property to aggregate",required=true) @PathParam("node") String node,
			@Context HttpServletRequest req) {
	    	try {
	    		boolean response = StreamDao.canAccessNode(node);
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

	    public Response updateEntry(
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
	 @DELETE
     @Path("/delete/{repository}/{entry}")
	        
     @ApiOperation(
    	value = "delete a stream object",
    	notes = "the current user must be author of the given stream object"
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
	    public Response deleteEntry(
	    	@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
	    	@ApiParam(value = "entry id to delete",required=true ) @PathParam("entry") String entryId,
			@Context HttpServletRequest req) {
	    	try {
	    		RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    		StreamDao.deleteEntry(repoDao, entryId);
		    	return Response.status(Response.Status.OK).build();
	    	}catch(Throwable t) {
	    		return ErrorResponse.createResponse(t);
	    	}
	 }
}