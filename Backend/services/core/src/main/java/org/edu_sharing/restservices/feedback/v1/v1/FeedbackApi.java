package org.edu_sharing.restservices.feedback.v1.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.log4j.Logger;
import org.edu_sharing.restservices.ApiService;
import org.edu_sharing.restservices.DAOException;
import org.edu_sharing.restservices.RepositoryDao;
import org.edu_sharing.restservices.RestConstants;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.edu_sharing.service.feedback.FeedbackServiceFactory;
import org.edu_sharing.service.feedback.model.FeedbackData;
import org.edu_sharing.service.feedback.model.FeedbackResult;

import jakarta.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Path("/feedback/v1")
@Tag(name= "FEEDBACK v1" )
@ApiService(value = "FEEDBACK", major = 1, minor = 0)
@Consumes({ "application/json" })
@Produces({"application/json"})
public class FeedbackApi {
	private static Logger logger = Logger.getLogger(FeedbackApi.class);
	@PUT
	@Path("/feedback/{repository}/{node}/add")
	@Operation(summary = "Give feedback on a node", description = "Adds feedback to the given node. Depending on the internal config, the current user will be obscured to prevent back-tracing to the original id")
	@ApiResponses(value = {
			@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = FeedbackResult.class))),
	        @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
	        @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))), 
	        @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
	    })
	public Response addFeedback(
			@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
			@Parameter(description = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String nodeId,
			@Parameter(description = "feedback data, key/value pairs",required=true ) Map<String, List<String>> feedbackData,
			@Context HttpServletRequest req) {
    	try {
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			FeedbackResult result = FeedbackServiceFactory.getFeedbackService(repoDao.getId()).addFeedback(
					nodeId,
					feedbackData
			);
	    	return Response.status(Response.Status.OK).entity(result).build();
    	} catch (Throwable t) {
			return ErrorResponse.createResponse(DAOException.mapping(t));
		}
    }
	@GET
	@Path("/feedback/{repository}/{node}/list")
	@Operation(summary = "Get given feedback on a node", description = "Get all given feedback for a node. Requires Coordinator permissions on node")
	@ApiResponses(value = {
			@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(array = @ArraySchema(schema = @Schema(implementation = FeedbackData.class)))),
			@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	})
	public Response getFeedbacks(
			@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
			@Parameter(description = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String nodeId,
			@Context HttpServletRequest req) {
		try {
			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			List<FeedbackData> entity = FeedbackServiceFactory.getFeedbackService(repoDao.getId()).getFeedback(
					nodeId
			);
			return Response.status(Response.Status.OK).entity(entity).build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(DAOException.mapping(t));
		}
	}

}
