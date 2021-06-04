package org.edu_sharing.restservices.tracking.v1;

import io.swagger.annotations.*;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tracking.TrackingEvent;
import org.edu_sharing.restservices.*;
import org.edu_sharing.restservices.comment.v1.model.Comments;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.edu_sharing.service.tracking.TrackingService;
import org.edu_sharing.service.tracking.TrackingServiceFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.Arrays;

@Path("/tracking/v1")
@Api(tags = { "TRACKING v1" })
@ApiService(value = "TRACKING", major = 1, minor = 0)
public class TrackingApi {
	private static Logger logger = Logger.getLogger(TrackingApi.class);
	@PUT
	@Path("/tracking/{repository}/{event}")
	@ApiOperation(value = "Track a user interaction", notes = "Currently limited to video / audio play interactions")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Void.class),
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })
	public Response trackEvent(
			@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
	    	@ApiParam(value = "type of event to track",required=false ) @PathParam("event") TrackingService.EventType event,
	    	@ApiParam(value = "node id for which the event is tracked. For some event, this can be null",required=false ) @QueryParam("node") String node,
			@Context HttpServletRequest req) {
    	try {
	    	if(Arrays.asList(TrackingService.EventType.VIEW_MATERIAL_PLAY_MEDIA, TrackingService.EventType.VIEW_MATERIAL).contains(event)){
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
