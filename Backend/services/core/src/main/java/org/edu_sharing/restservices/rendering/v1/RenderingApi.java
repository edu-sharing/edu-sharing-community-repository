package org.edu_sharing.restservices.rendering.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.authentication.ContextManagementFilter;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tracking.TrackingTool;
import org.edu_sharing.restservices.*;
import org.edu_sharing.restservices.admin.v1.ApplicationSimple;
import org.edu_sharing.restservices.rendering.v1.model.RenderingDetailsEntry;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.edu_sharing.service.authority.AuthorityServiceHelper;
import org.edu_sharing.service.rendering.RenderingDetails;
import org.edu_sharing.service.rendering.RenderingTool;
import org.edu_sharing.service.repoproxy.RepoProxy;
import org.edu_sharing.service.repoproxy.RepoProxyFactory;
import org.edu_sharing.service.tracking.ActivityOnNodeEventType;
import org.edu_sharing.service.tracking.NodeTrackingDetails;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;


@Path("/rendering/v1")
@Tag(name="RENDERING v1")
@ApiService(value="RENDERING", major=1, minor=0)
@Consumes({ "application/json" })
@Produces({"application/json"})
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


	/**
	 * @Deprecated
	 * use getDetailsSnippetWithParameters instead
	 */
	public Response getDetailsSnippet(
			@Parameter(description = "ID of repository (or \"-home-\" for home repository)", required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
	    	@Parameter(description = "ID of node",required=true ) @PathParam("node") String node,
	    	@Parameter(description = "version of node",required=false) @QueryParam("version") String nodeVersion,
	    	@Parameter(description = "Rendering displayMode", required=false) @QueryParam("displayMode") String displayMode,
			@Context HttpServletRequest req){
		return getDetailsSnippetWithParameters(repository, node, nodeVersion, displayMode, null, req);
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


			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			if (repoDao == null) {
				return Response.status(Response.Status.NOT_FOUND).build();
			}

			if(remote != null) {
				return RepoProxyFactory.getRepoProxy().getDetailsSnippetWithParameters(remote.getRepository(), remote.getNodeId(), nodeVersion, displayMode, parameters, req);
			} else {
				RenderingDetails detailsSnippet = new RenderingDao(repoDao).getDetails(node, nodeVersion, displayMode, parameters);
				if(detailsSnippet.getException() != null) {
					if(detailsSnippet.getException().getNested() != null)  {
						DAOException mapped = DAOException.mapping(detailsSnippet.getException().getNested());
						if(mapped instanceof DAOMissingException) {
							throw mapped;
						}
					}
				}
				if (repoDao.isHomeRepo()) {
					NodeTrackingDetails details = (NodeTrackingDetails) org.edu_sharing.alfresco.repository.server.authentication.
							Context.getCurrentInstance().getRequest().getSession().getAttribute(CCConstants.SESSION_RENDERING_DETAILS);
					if (details == null || !details.getNodeId().equals(node)) {
						details = new NodeTrackingDetails(node, nodeVersion);
					} else {
						details.setNodeVersion(nodeVersion);
						org.edu_sharing.alfresco.repository.server.authentication.
								Context.getCurrentInstance().getRequest().getSession().removeAttribute(CCConstants.SESSION_RENDERING_DETAILS);
					}
					if (Arrays.asList(RenderingTool.DISPLAY_DYNAMIC, RenderingTool.DISPLAY_CONTENT).contains(displayMode) || displayMode == null) {
						TrackingTool.trackActivityOnNode(node, details, ActivityOnNodeEventType.VIEW_MATERIAL);
					} else if (RenderingTool.DISPLAY_INLINE.equals(displayMode)) {
						TrackingTool.trackActivityOnNode(node, details, ActivityOnNodeEventType.VIEW_MATERIAL_EMBEDDED);
					}
				}

				RenderingDetailsEntry response = new RenderingDetailsEntry();
				response.setDetailsSnippet(detailsSnippet.getDetails());
				if(detailsSnippet.getRenderingServiceData() != null) {
					String mimeType = detailsSnippet.getRenderingServiceData().getNode().getMimetype();
					response.setMimeType(mimeType);
					response.setNode(detailsSnippet.getRenderingServiceData().getNode());
				}

				return Response.status(Response.Status.OK).entity(response).build();
			}
		}catch (Throwable t) {
			logger.error(t.getMessage(), t);
			return ErrorResponse.createResponse(t);
		}

	}

	@GET
	@Path("/applications")


	@Operation(summary = "Provides application infos for a connected renderer", description = "Note: Requires admin rights or an application signature header for the rendering service")

	@ApiResponses(
			value = { @ApiResponse(responseCode="200", description=RestConstants.HTTP_200,  content = @Content(
					array = @ArraySchema(schema = @Schema(implementation = ApplicationSimple.class))
			)),
					@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
					@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })
	public Response getApplications(
			@Context HttpServletRequest req){
		try {
			if(
					(

							ContextManagementFilter.accessTool.get() == null ||
							!ApplicationInfo.TYPE_RENDERSERVICE_2.equals(ContextManagementFilter.accessTool.get().getApplicationInfo().getType())
					) &&
							!AuthorityServiceHelper.isAdmin()) {
				throw new DAOSecurityException(new SecurityException());
			}
			return Response.ok(ApplicationInfoList.getRepositoryInfosOrdered().stream().map(a -> {
				ApplicationSimple entry = new ApplicationSimple();
				entry.fill(a);
				return entry;
			}).collect(Collectors.toList())).build();
		}catch(Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}

}
