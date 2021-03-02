package org.edu_sharing.restservices.rendering.v1;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.server.tools.security.SignatureVerifier;
import org.edu_sharing.repository.server.tracking.TrackingTool;
import org.edu_sharing.restservices.ApiService;
import org.edu_sharing.restservices.NodeDao;
import org.edu_sharing.restservices.RenderingDao;
import org.edu_sharing.restservices.RepositoryDao;
import org.edu_sharing.restservices.rendering.v1.model.RenderingDetailsEntry;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.edu_sharing.restservices.shared.Filter;
import org.edu_sharing.restservices.shared.Node;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import org.edu_sharing.service.repoproxy.RepoProxy;
import org.edu_sharing.service.repoproxy.RepoProxyFactory;
import org.edu_sharing.service.tracking.NodeTrackingDetails;
import org.edu_sharing.service.tracking.TrackingService;
import org.edu_sharing.service.tracking.TrackingServiceFactory;


@Path("/rendering/v1")
@Api(tags = {"RENDERING v1"})
@ApiService(value="RENDERING", major=1, minor=0)
public class RenderingApi {

	private static Logger logger = Logger.getLogger(RenderingApi.class);
	
	@GET
    @Path("/details/{repository}/{node}")
    
	
	 @ApiOperation(
		    	value = "Get metadata of node.", 
		    	notes = "Get metadata of node.")
		    
    @ApiResponses(
    	value = { 
	        @ApiResponse(code = 200, message = "OK.", response = RenderingDetailsEntry.class),
	        @ApiResponse(code = 400, message = "Preconditions are not present.", response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = "Authorization failed.", response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = "Session user has insufficient rights to perform this operation.", response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = "Ressources are not found.", response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = "Fatal error occured.", response = ErrorResponse.class) 
	    })
	
	public Response getDetailsSnippet(
			@ApiParam(value = "ID of repository (or \"-home-\" for home repository)",required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
	    	@ApiParam(value = "ID of node",required=true ) @PathParam("node") String node,
	    	@ApiParam(value = "version of node",required=false) @QueryParam("version") String nodeVersion,
	    	@ApiParam(value = "Rendering displayMode", required=false) @QueryParam("displayMode") String displayMode,
			@Context HttpServletRequest req){

		try{
			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			if (repoDao == null) {
				return Response.status(Response.Status.NOT_FOUND).build();
			}
			String detailsSnippet = new RenderingDao(repoDao).getDetails(node,nodeVersion,displayMode,null);

			Node nodeJson = NodeDao.getNode(repoDao, node, Filter.createShowAllFilter()).asNode();
			String mimeType = nodeJson.getMimetype();


			RenderingDetailsEntry response = new RenderingDetailsEntry();
			response.setDetailsSnippet(detailsSnippet);
			response.setMimeType(mimeType);
			response.setNode(nodeJson);

			return Response.status(Response.Status.OK).entity(response).build();

		}catch (Throwable t) {

			logger.error(t.getMessage(), t);
			return ErrorResponse.createResponse(t);
		}
	}
	
	
	@POST
    @Path("/details/{repository}/{node}")
    
	
	 @ApiOperation(
		    	value = "Get metadata of node.", 
		    	notes = "Get metadata of node.")
		    
    @ApiResponses(
    	value = { 
	        @ApiResponse(code = 200, message = "OK.", response = RenderingDetailsEntry.class),
	        @ApiResponse(code = 400, message = "Preconditions are not present.", response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = "Authorization failed.", response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = "Session user has insufficient rights to perform this operation.", response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = "Ressources are not found.", response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = "Fatal error occured.", response = ErrorResponse.class) 
	    })
	
	public Response getDetailsSnippetWithParameters(
			@ApiParam(value = "ID of repository (or \"-home-\" for home repository)",required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
	    	@ApiParam(value = "ID of node",required=true ) @PathParam("node") String node,
	    	@ApiParam(value = "version of node",required=false) @QueryParam("version") String nodeVersion,
			@ApiParam(value = "Rendering displayMode", required=false) @QueryParam("displayMode") String displayMode,
			@ApiParam(value = "additional parameters to send to the rendering service",required=false) Map<String,String> parameters,
			@Context HttpServletRequest req){

		try {
			RepoProxy.RemoteRepoDetails remote = RepoProxyFactory.getRepoProxy().myTurn(repository, node);
			if(remote != null) {
				return RepoProxyFactory.getRepoProxy().getDetailsSnippetWithParameters(remote.getRepository(), remote.getNodeId(), nodeVersion, displayMode, parameters, req);
			}
			
			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			if (repoDao == null) {
				return Response.status(Response.Status.NOT_FOUND).build();
			}
			String detailsSnippet = new RenderingDao(repoDao).getDetails(node,nodeVersion, displayMode,parameters);

			NodeDao nodeDao = NodeDao.getNode(repoDao, node, Filter.createShowAllFilter());
			Node nodeJson = nodeDao.asNode();
			String mimeType = nodeJson.getMimetype();

			if(repoDao.isHomeRepo())
				TrackingTool.trackActivityOnNode(node,new NodeTrackingDetails(nodeVersion),TrackingService.EventType.VIEW_MATERIAL);


			RenderingDetailsEntry response = new RenderingDetailsEntry();
			response.setDetailsSnippet(detailsSnippet);
			response.setMimeType(mimeType);
			response.setNode(nodeJson);

			return Response.status(Response.Status.OK).entity(response).build();
		}catch (Throwable t) {

			logger.error(t.getMessage(), t);
			return ErrorResponse.createResponse(t);
		}

	}
}
