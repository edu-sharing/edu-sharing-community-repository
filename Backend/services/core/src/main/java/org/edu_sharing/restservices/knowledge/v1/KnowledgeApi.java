package org.edu_sharing.restservices.knowledge.v1;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.edu_sharing.restservices.knowledge.v1.model.JobEntry;

@Path("/knowledge/v1")
public class KnowledgeApi  {

    @POST
    @Path("/analyze/jobs")
        
    @ApiOperation(
		value = "Run analyzing job.", 
		notes = "Run analyzing job for a node.")
    
    @ApiResponses(
    	value = { 
			@ApiResponse(code = 202, message = "Accepted.", response = JobEntry.class),        
			@ApiResponse(code = 401, message = "Authorization failed.", response = Void.class),        
			@ApiResponse(code = 403, message = "The current user has insufficient rights to read the node or to perform an analyzing job.", response = Void.class),        
			@ApiResponse(code = 404, message = "Repository or node not found.", response = Void.class) 
		})

    public Response runAnalyzingJob(
    	@ApiParam(value = "ID of repository (or \"-home-\" for home repository)",required=true, defaultValue="-home-") @QueryParam("repository") String repository,
    	@ApiParam(value = "ID of node",required=true) @QueryParam("node") String node,
		@Context HttpServletRequest req) {
        
    	return null;
    }

    @OPTIONS    
    @Path("/analyze/jobs")
    @ApiOperation(hidden = true, value = "")
    public Response options1() {
    	
    	return Response.status(Response.Status.OK).header("Allow", "OPTIONS, POST").build();
    }

    @GET
    @Path("/analyze/jobs/{job}")
    
    
    @ApiOperation(
    	value = "Get analyzing job status.", 
    	notes = "Get analyzing job status.")
    
    @ApiResponses(
    	value = { 
			@ApiResponse(code = 200, message = "OK.", response = JobEntry.class),        
			@ApiResponse(code = 401, message = "Authorization failed.", response = Void.class),        
			@ApiResponse(code = 403, message = "The current user has insufficient rights to access the ticket.", response = Void.class),        
			@ApiResponse(code = 404, message = "Job not found.", response = Void.class) 
		})

    public Response getAnalyzingJobStatus(
    	@ApiParam(value = "ID of job ticket",required=true ) @PathParam("job") String job,
		@Context HttpServletRequest req) {
    	
        return null;
    }
    
    @OPTIONS    
    @Path("/analyze/jobs/{job}")
    @ApiOperation(hidden = true, value = "")
    public Response options2() {
    	
    	return Response.status(Response.Status.OK).header("Allow", "OPTIONS, GET").build();
    }
    
}

