package org.edu_sharing.restservices.archive.v1;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.edu_sharing.restservices.ApiService;
import org.edu_sharing.restservices.ArchiveDao;
import org.edu_sharing.restservices.DAOMissingException;
import org.edu_sharing.restservices.DAOSecurityException;
import org.edu_sharing.restservices.DAOValidationException;
import org.edu_sharing.restservices.NodeDao;
import org.edu_sharing.restservices.RepositoryDao;
import org.edu_sharing.restservices.archive.v1.model.RestoreResults;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.edu_sharing.restservices.shared.Filter;
import org.edu_sharing.restservices.shared.Node;
import org.edu_sharing.restservices.shared.NodeRef;
import org.edu_sharing.restservices.shared.NodeSearch;
import org.edu_sharing.restservices.shared.Pagination;
import org.edu_sharing.restservices.shared.SearchResult;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Path("/archive/v1")
@Api(tags = { "ARCHIVE v1" })
@ApiService(value = "ARCHIVE", major = 1, minor = 0)
public class ArchiveApi {

	
	/**
	 * 	GET		archive/v1/search
		GET		archive/v1/search/username
		PUT     archive/v1/restore/nodeid/targetnodeid
		DELETE  archive/v1/purge/nodeid
	 */
	
	private static Logger logger = Logger.getLogger(ArchiveApi.class);
	
	@GET
	@Path("/search/{repository}/{pattern}")
	
	@ApiOperation(value = "Searches for archive nodes.", notes = "Searches for archive nodes.")
	
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "OK.", response = SearchResult.class),
	        @ApiResponse(code = 400, message = "Preconditions are not present.", response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = "Authorization failed.", response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = "Session user has insufficient rights to perform this operation.", response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = "Ressources are not found.", response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = "Fatal error occured.", response = ErrorResponse.class) 
	    })
	
	public Response search(@ApiParam(value = "ID of repository (or \"-home-\" for home repository)", required = true, defaultValue = "-home-") @PathParam("repository") String repository,
			@ApiParam(value = "search pattern", required = true) @PathParam("pattern") String pattern,
			@ApiParam(value = "maximum items per page", defaultValue="10") @QueryParam("maxItems") Integer maxItems,
		    @ApiParam(value = "skip a number of items", defaultValue="0") @QueryParam("skipCount") Integer skipCount,   
		    @ApiParam(value = "sort properties") @QueryParam("sortProperties") List<String> sortProperties,
		    @ApiParam(value = "sort ascending") @QueryParam("sortAscending") List<Boolean> sortAscending,
		    @ApiParam(value = "property filter for result nodes (or \"-all-\" for all properties)") @QueryParam("propertyFilter") List<String> propertyFilter,
			@Context HttpServletRequest req){
			
		return search(repository,pattern,null,maxItems,skipCount,sortProperties,sortAscending,new Filter(propertyFilter));
		
	}
	
	@OPTIONS    
	@Path("/search/{repository}/{pattern}")
    @ApiOperation(hidden = true, value = "")

	public Response options() {
		
		return Response.status(Response.Status.OK).header("Allow", "OPTIONS, GET").build();
	}
	
	@GET
	@Path("/search/{repository}/{pattern}/{person}")
	
	@ApiOperation(value = "Searches for archive nodes.", notes = "Searches for archive nodes.")
	
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "OK.", response = SearchResult.class),
	        @ApiResponse(code = 400, message = "Preconditions are not present.", response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = "Authorization failed.", response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = "Session user has insufficient rights to perform this operation.", response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = "Ressources are not found.", response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = "Fatal error occured.", response = ErrorResponse.class) 
	    })
	
	public Response search(@ApiParam(value = "ID of repository (or \"-home-\" for home repository)", required = true, defaultValue = "-home-") @PathParam("repository") String repository,
			@ApiParam(value = "search pattern", required = true) @PathParam("pattern") String pattern,
			@ApiParam(value = "person", required = true, defaultValue = "-me-") @PathParam("person") String person,
			@ApiParam(value = "maximum items per page", defaultValue="10") @QueryParam("maxItems") Integer maxItems,
		    @ApiParam(value = "skip a number of items", defaultValue="0") @QueryParam("skipCount") Integer skipCount,   
		    @ApiParam(value = "sort properties") @QueryParam("sortProperties") List<String> sortProperties,
		    @ApiParam(value = "sort ascending") @QueryParam("sortAscending") List<Boolean> sortAscending,
		    @ApiParam(value = "property filter for result nodes (or \"-all-\" for all properties)") @QueryParam("propertyFilter") List<String> propertyFilter,
			@Context HttpServletRequest req){
		
		return search(repository, pattern, person, maxItems, skipCount,sortProperties,sortAscending, new Filter(propertyFilter));
	}
	
	@OPTIONS    
	@Path("/search/{repository}/{pattern}/{person}")
    @ApiOperation(hidden = true, value = "")

	public Response options1() {
		
		return Response.status(Response.Status.OK).header("Allow", "OPTIONS, GET").build();
	}
	
	
	private Response search(String repository,
			String pattern, 
			String user,
			Integer maxItems,
		    Integer skipCount,
		    List<String> sortProperties,
		    List<Boolean> sortAscending, 
		    Filter filter){
		try {
			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			NodeSearch search = (user == null) ? 
					ArchiveDao.search(repoDao, pattern, skipCount, maxItems, sortProperties, sortAscending) : 
						ArchiveDao.search(repoDao, pattern,user, skipCount, maxItems, sortProperties, sortAscending);
			
			List<Node> data = new ArrayList<Node>();
	    	for (NodeRef ref : search.getResult()) {
	    		
	    		if(ref.isArchived()){
	    			data.add(NodeDao.getNode(repoDao, NodeDao.archiveStoreProtocol,NodeDao.archiveStoreId, ref.getId(), filter).asNode());
	    		}else{
	    			data.add(NodeDao.getNode(repoDao, ref.getId(),filter).asNode());
	    		}
	    	}
			
			Pagination pagination = new Pagination();
	    	pagination.setFrom(search.getSkip());
	    	pagination.setCount(data.size());
	    	pagination.setTotal(search.getCount());
	    	
	    	
	    	SearchResult<Node> response = new SearchResult<>();
	    	response.setNodes(data);
	    	response.setPagination(pagination);	    	
	    	response.setFacettes(search.getFacettes());
	    	
	    	return Response.status(Response.Status.OK).entity(response).build();
	    	
		} catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}
	}
	
	
	@DELETE
	@Path("/purge/{repository}")
	
	@ApiOperation(value = "Searches for archive nodes.", notes = "Searches for archive nodes.")
	
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "OK.", response = String.class),
	        @ApiResponse(code = 400, message = "Preconditions are not present.", response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = "Authorization failed.", response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = "Session user has insufficient rights to perform this operation.", response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = "Ressources are not found.", response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = "Fatal error occured.", response = ErrorResponse.class) 
	    })
	
	public Response purge(@ApiParam(value = "ID of repository (or \"-home-\" for home repository)", required = true, defaultValue = "-home-") @PathParam("repository") String repository,
			@ApiParam(value = "archived node", required = true) @QueryParam("archivedNodeIds")  List<String> archivedNodeIds,
			@Context HttpServletRequest req){
		
		try {
			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			ArchiveDao.purge(repoDao, archivedNodeIds);
			return Response.status(Response.Status.OK).build();
		} catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}
	}
	
	@OPTIONS    
	@Path("/purge/{repository}/{archivedNodeId}")
    @ApiOperation(hidden = true, value = "")

	public Response options2() {
		
		return Response.status(Response.Status.OK).header("Allow", "OPTIONS, DELETE").build();
	}
	
	@POST
	@Path("/restore/{repository}")
	
	@ApiOperation(value = "restore archived nodes.", notes = "restores archived nodes. restoreStatus can have the following values: FALLBACK_PARENT_NOT_EXISTS, FALLBACK_PARENT_NO_PERMISSION, DUPLICATENAME, FINE")
	
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "OK.", response = RestoreResults.class),
	        @ApiResponse(code = 400, message = "Preconditions are not present.", response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = "Authorization failed.", response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = "Session user has insufficient rights to perform this operation.", response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = "Ressources are not found.", response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = "Fatal error occured.", response = ErrorResponse.class) 
	    })
	
	public Response restore(@ApiParam(value = "ID of repository (or \"-home-\" for home repository)", required = true, defaultValue = "-home-") @PathParam("repository") String repository,
			@ApiParam(value = "archived nodes", required = true) @QueryParam("archivedNodeIds") List<String> archivedNodeIds,
			@ApiParam(value = "to target", required = false) @QueryParam("target") String target,
			@Context HttpServletRequest req){
		
		try {
			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			
			RestoreResults response = new RestoreResults();
			response.setResults(ArchiveDao.restore(repoDao, archivedNodeIds, target));
			return Response.status(Response.Status.OK).entity(response).build();
		} catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
    	}
	}
	
	@OPTIONS    
	@Path("/restore/{repository}/{archivedNodeId}/{target}")
    @ApiOperation(hidden = true, value = "")

	public Response options3() {
		
		return Response.status(Response.Status.OK).header("Allow", "OPTIONS, POST").build();
	}
	
}
