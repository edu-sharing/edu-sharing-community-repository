package org.edu_sharing.restservices.tracking.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.log4j.Logger;
import org.edu_sharing.restservices.ApiService;
import org.edu_sharing.restservices.RestConstants;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.edu_sharing.service.tracking.TrackingService;
import org.edu_sharing.service.tracking.TrackingServiceFactory;

import jakarta.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.Arrays;

@Path("/tracking/v1")
@Tag(name= "TRACKING v1" )
@ApiService(value = "TRACKING", major = 1, minor = 0)
@Consumes({ "application/json" })
@Produces({"application/json"})
public class TrackingApi {
	private static Logger logger = Logger.getLogger(TrackingApi.class);
	@PUT
	@Path("/tracking/{repository}/{event}")
	@Operation(summary = "Track a user interaction", description = "Currently limited to video / audio play interactions")
	@ApiResponses(value = {
			@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Void.class))),
	        @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
	        @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
	    })
	public Response trackEvent(
			@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
	    	@Parameter(description = "type of event to track",required=false ) @PathParam("event") TrackingService.EventType event,
	    	@Parameter(description = "node id for which the event is tracked. For some event, this can be null",required=false ) @QueryParam("node") String node,
			@Context HttpServletRequest req) {
    	try {
	    	if(Arrays.asList(
					TrackingService.EventType.VIEW_MATERIAL_PLAY_MEDIA, TrackingService.EventType.VIEW_MATERIAL,
					TrackingService.EventType.DOWNLOAD_MATERIAL, TrackingService.EventType.OPEN_EXTERNAL_LINK
			).contains(event)){
				TrackingServiceFactory.getTrackingService().trackActivityOnNode(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, node), null, event);
				return Response.status(Response.Status.OK).build();
			} else {
	    		throw new IllegalArgumentException("the given event is currently not supported via api");
			}
    	} catch (Throwable t) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(t)).build();
		}
    }
}
