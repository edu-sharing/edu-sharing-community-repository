package org.edu_sharing.restservices.knowledge.v1;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.edu_sharing.restservices.knowledge.v1.model.JobEntry;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

@Path("/knowledge/v1")
@Consumes({ "application/json" })
@Produces({"application/json"})
@Tag(name="KNOWLEDGE v1")
public class KnowledgeApi  {

    @POST
    @Path("/analyze/jobs")
        
    @Operation(summary = "Run analyzing job.", description = "Run analyzing job for a node.")
    
    @ApiResponses(
    	value = { 
			@ApiResponse(responseCode="202", description="Accepted.", content = @Content(schema = @Schema(implementation = JobEntry.class))),        
			@ApiResponse(responseCode="401", description="Authorization failed.", content = @Content(schema = @Schema(implementation = Void.class))),        
			@ApiResponse(responseCode="403", description="The current user has insufficient rights to read the node or to perform an analyzing job.", content = @Content(schema = @Schema(implementation = Void.class))),        
			@ApiResponse(responseCode="404", description="Repository or node not found.", content = @Content(schema = @Schema(implementation = Void.class))) 
		})

    public Response runAnalyzingJob(
    	@Parameter(description = "ID of repository (or \"-home-\" for home repository)", required = true, schema = @Schema(defaultValue="-home-")) @QueryParam("repository") String repository,
    	@Parameter(description = "ID of node",required=true) @QueryParam("node") String node,
		@Context HttpServletRequest req) {
        
    	return null;
    }

    @OPTIONS    
    @Path("/analyze/jobs")
    @Hidden
    public Response options1() {
    	
    	return Response.status(Response.Status.OK).header("Allow", "OPTIONS, POST").build();
    }

    @GET
    @Path("/analyze/jobs/{job}")
    
    
    @Operation(summary = "Get analyzing job status.", description = "Get analyzing job status.")
    
    @ApiResponses(
    	value = { 
			@ApiResponse(responseCode="200", description="OK.", content = @Content(schema = @Schema(implementation = JobEntry.class))),        
			@ApiResponse(responseCode="401", description="Authorization failed.", content = @Content(schema = @Schema(implementation = Void.class))),        
			@ApiResponse(responseCode="403", description="The current user has insufficient rights to access the ticket.", content = @Content(schema = @Schema(implementation = Void.class))),        
			@ApiResponse(responseCode="404", description="Job not found.", content = @Content(schema = @Schema(implementation = Void.class))) 
		})

    public Response getAnalyzingJobStatus(
    	@Parameter(description = "ID of job ticket",required=true ) @PathParam("job") String job,
		@Context HttpServletRequest req) {
    	
        return null;
    }
    
    @OPTIONS    
    @Path("/analyze/jobs/{job}")
    @Hidden
    public Response options2() {
    	
    	return Response.status(Response.Status.OK).header("Allow", "OPTIONS, GET").build();
    }
    
}

