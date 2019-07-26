package org.edu_sharing.restservices.statistic.v1;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.restservices.*;
import org.edu_sharing.restservices.about.v1.model.About;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.edu_sharing.restservices.statistic.v1.model.Filter;
import org.edu_sharing.restservices.statistic.v1.model.Statistics;
import org.edu_sharing.restservices.tracking.v1.model.Tracking;
import org.edu_sharing.restservices.tracking.v1.model.TrackingNode;
import org.edu_sharing.service.statistic.StatisticsGlobal;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.edu_sharing.service.toolpermission.ToolPermissionHelper;
import org.edu_sharing.service.tracking.TrackingService;

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
			@ApiParam(value = "filter", required = true) Filter filter, @ApiParam(value = "properties")
            @QueryParam("properties") List<String> properties,
            @Context HttpServletRequest req) {

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

	    public Response getGlobalStatistics(
                @ApiParam(value = "primary property to build facettes and count+group values", required = false) @QueryParam("group") String group,
                @ApiParam(value = "additional properties to build facettes and count+sub-group values", required = false) @QueryParam("subGroup") List<String> subGroup) {

	    	try {
		    	StatisticsGlobal statistics=StatisticDao.getGlobal(group,subGroup);
		    	return Response.status(Response.Status.OK).entity(statistics).build();
		    	
			} catch (Throwable t) {
				return ErrorResponse.createResponse(t);				
			}
	
		}

	@POST
	@Path("/statistics/nodes")

	@ApiOperation(value = "get statistics for node actions",
			      notes = "requires toolpermission "+CCConstants.CCM_VALUE_TOOLPERMISSION_GLOBAL_STATISTICS
	)

	@ApiResponses(value = {
			@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = TrackingNode[].class),
			@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
			@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
			@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
			@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) })
	public Response getStatisticsNode(@Context HttpServletRequest req,
									  @ApiParam(value = "Grouping type (by date)", required = true) @QueryParam("grouping")TrackingService.GroupingType grouping,
									  @ApiParam(value = "date range from", required = true) @QueryParam("dateFrom") Long dateFrom,
									  @ApiParam(value = "date range to", required = true) @QueryParam("dateTo") Long dateTo,
									  @ApiParam(value = "additionals fields of the custom json object stored in each query that should be returned", required = false) @QueryParam("additionalFields") List<String> additionalFields,
									  @ApiParam(value = "grouping fields of the custom json object stored in each query (currently only meant to be combined with no grouping by date)", required = false) @QueryParam("groupField") List<String> groupField,
									  @ApiParam(value = "filters for the custom json object stored in each entry", required = false) Map<String,String> filters
	) {
		try {
			// load instance to validate session
			ToolPermissionHelper.throwIfToolpermissionMissing(CCConstants.CCM_VALUE_TOOLPERMISSION_GLOBAL_STATISTICS);
			List<TrackingNode> tracks=AuthenticationUtil.runAsSystem(()->
					TrackingDAO.getNodeStatistics(grouping,new Date(dateFrom),new Date(dateTo),additionalFields,groupField,filters)
			);
			return Response.ok().entity(tracks).build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}

	@POST
	@Path("/statistics/users")

	@ApiOperation(value = "get statistics for user actions (login, logout)",
			      notes = "requires toolpermission "+CCConstants.CCM_VALUE_TOOLPERMISSION_GLOBAL_STATISTICS
	)
			@ApiResponses(value = {
			@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Tracking[].class),
			@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
			@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
			@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
			@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) })
	public Response getStatisticsUser(@Context HttpServletRequest req,
									  @ApiParam(value = "Grouping type (by date)", required = true) @QueryParam("grouping")TrackingService.GroupingType grouping,
									  @ApiParam(value = "date range from", required = true) @QueryParam("dateFrom") Long dateFrom,
									  @ApiParam(value = "date range to", required = true) @QueryParam("dateTo") Long dateTo,
									  @ApiParam(value = "additionals fields of the custom json object stored in each query that should be returned", required = false) @QueryParam("additionalFields") List<String> additionalFields,
									  @ApiParam(value = "grouping fields of the custom json object stored in each query (currently only meant to be combined with no grouping by date)", required = false) @QueryParam("groupField") List<String> groupField,
									  @ApiParam(value = "filters for the custom json object stored in each entry", required = false) Map<String,String> filters
	) {
		try {
			// load instance to validate session
			ToolPermissionHelper.throwIfToolpermissionMissing(CCConstants.CCM_VALUE_TOOLPERMISSION_GLOBAL_STATISTICS);
			List<Tracking> tracks=AuthenticationUtil.runAsSystem(()->
					TrackingDAO.getUserStatistics(grouping,new Date(dateFrom),new Date(dateTo),additionalFields,groupField,filters)
			);
			return Response.ok().entity(tracks).build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}
}
