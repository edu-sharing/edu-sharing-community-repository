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
import org.codehaus.groovy.reflection.stdclasses.CachedClosureClass;
import org.edu_sharing.repository.client.tools.CCConstants;
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

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import org.edu_sharing.service.repoproxy.RepoProxy;
import org.edu_sharing.service.repoproxy.RepoProxyFactory;
import org.edu_sharing.service.tracking.NodeTrackingDetails;
import org.edu_sharing.service.tracking.TrackingService;
import org.edu_sharing.service.tracking.TrackingServiceFactory;


@Path("/rendering/v1")
@Tag(name="RENDERING v1")
@ApiService(value="RENDERING", major=1, minor=0)
public class RenderingApi {

	private static Logger logger = Logger.getLogger(RenderingApi.class);
	
	@GET
    @Path("/details/{repository}/{node}")
    
	
	 @Operation(summary = "Get metadata of node.", description = "Get metadata of node.")
		    
    @ApiResponses(
    	value = { 
	        @ApiResponse(responseCode="200", description="OK.", content = @Content(schema = @Schema(implementation = RenderingDetailsEntry.class))),
	        @ApiResponse(responseCode="400", description="Preconditions are not present.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
	        @ApiResponse(responseCode="401", description="Authorization failed.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
	        @ApiResponse(responseCode="403", description="Session user has insufficient rights to perform this operation.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
	        @ApiResponse(responseCode="404", description="Ressources are not found.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
	        @ApiResponse(responseCode="500", description="Fatal error occured.", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	    })
	
	public Response getDetailsSnippet(
			@Parameter(description = "ID of repository (or \"-home-\" for home repository)", required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
	    	@Parameter(description = "ID of node",required=true ) @PathParam("node") String node,
	    	@Parameter(description = "version of node",required=false) @QueryParam("version") String nodeVersion,
	    	@Parameter(description = "Rendering displayMode", required=false) @QueryParam("displayMode") String displayMode,
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
    
	
	 @Operation(summary = "Get metadata of node.", description = "Get metadata of node.")
		    
    @ApiResponses(
    	value = { 
	        @ApiResponse(responseCode="200", description="OK.", content = @Content(schema = @Schema(implementation = RenderingDetailsEntry.class))),
	        @ApiResponse(responseCode="400", description="Preconditions are not present.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
	        @ApiResponse(responseCode="401", description="Authorization failed.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
	        @ApiResponse(responseCode="403", description="Session user has insufficient rights to perform this operation.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
	        @ApiResponse(responseCode="404", description="Ressources are not found.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
	        @ApiResponse(responseCode="500", description="Fatal error occured.", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	    })
	
	public Response getDetailsSnippetWithParameters(
			@Parameter(description = "ID of repository (or \"-home-\" for home repository)", required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
	    	@Parameter(description = "ID of node",required=true ) @PathParam("node") String node,
	    	@Parameter(description = "version of node",required=false) @QueryParam("version") String nodeVersion,
			@Parameter(description = "Rendering displayMode", required=false) @QueryParam("displayMode") String displayMode,
			// options include: showDownloadButton, showDownloadAdvice, metadataGroup
			@Parameter(description = "additional parameters to send to the rendering service",required=false) Map<String,String> parameters,
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

			if(repoDao.isHomeRepo()) {
				NodeTrackingDetails details = (NodeTrackingDetails) org.edu_sharing.alfresco.repository.server.authentication.
						Context.getCurrentInstance().getRequest().getSession().getAttribute(CCConstants.SESSION_RENDERING_DETAILS);
				if(details == null || !details.getNodeId().equals(node)) {
					details = new NodeTrackingDetails(node, nodeVersion);
				} else {
					details.setNodeVersion(nodeVersion);
					org.edu_sharing.alfresco.repository.server.authentication.
							Context.getCurrentInstance().getRequest().getSession().removeAttribute(CCConstants.SESSION_RENDERING_DETAILS);
				}
				TrackingTool.trackActivityOnNode(node, details, TrackingService.EventType.VIEW_MATERIAL);
			}

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
