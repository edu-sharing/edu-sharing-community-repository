package org.edu_sharing.restservices.statistic.v1;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.edu_sharing.restservices.ApiService;
import org.edu_sharing.restservices.DAOMissingException;
import org.edu_sharing.restservices.DAOSecurityException;
import org.edu_sharing.restservices.DAOValidationException;
import org.edu_sharing.restservices.RestConstants;
import org.edu_sharing.restservices.StatisticDao;
import org.edu_sharing.restservices.about.v1.model.About;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.edu_sharing.restservices.statistic.v1.model.Filter;
import org.edu_sharing.restservices.statistic.v1.model.Statistics;
import org.edu_sharing.service.statistic.StatisticsGlobal;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Path("/statistic/v1")
@Api(tags = { "STATISTIC v1" })
@ApiService(value = "STATISTIC", major = 1, minor = 0)
public class StatisticApi {

	@POST
	@Path("/facettes/{context}")
	@ApiOperation(value = "Get statistics of repository.", notes = "Statistics.")

	@ApiResponses(value = { @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Statistics.class), @ApiResponse(code = 400, message = "Preconditions are not present.", response = ErrorResponse.class),
			@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
			@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
			@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) })
	public Response get(
			@ApiParam(value = "context, the node where to start", required = true, defaultValue = "-root-") @PathParam("context") String context,
			@ApiParam(value = "filter", required = true) Filter filter, @ApiParam(value = "properties") @QueryParam("properties") List<String> properties, @Context HttpServletRequest req) {

		try {
			Statistics statistics = new StatisticDao().get(context, properties, filter);
			return Response.status(Response.Status.OK).entity(statistics).build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);				
		}
	}
	
	 	@GET
		@Path("/public")
	    @ApiOperation(
	    	value = "Get stats.", 
	    	notes = "Get global statistics for this repository.")
	    
	    @ApiResponses(
	    	value = { 
				@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = StatisticsGlobal.class), 
		        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
		        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    	})

	    public Response getGlobalStatistics() {

	    	try {
		    	StatisticsGlobal statistics=StatisticDao.getGlobal();
		    	
		    	return Response.status(Response.Status.OK).entity(statistics).build();
		    	
			} catch (Throwable t) {
				return ErrorResponse.createResponse(t);				
			}
	
}

}
