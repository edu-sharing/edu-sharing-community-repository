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
import org.edu_sharing.service.InsufficientPermissionException;
import org.edu_sharing.service.NotAnAdminException;
import org.edu_sharing.service.authority.AuthorityServiceHelper;
import org.edu_sharing.service.statistic.StatisticsGlobal;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.edu_sharing.service.toolpermission.ToolPermissionHelper;
import org.edu_sharing.service.tracking.TrackingService;
import org.edu_sharing.service.tracking.ibatis.NodeData;

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
			      notes = "requires either toolpermission "+CCConstants.CCM_VALUE_TOOLPERMISSION_GLOBAL_STATISTICS_NODES+" for global stats or to be admin of the requested mediacenter"
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
									  @ApiParam(value = "the mediacenter to filter for statistics", required = false) @QueryParam("mediacenter") String mediacenter,
									  @ApiParam(value = "additionals fields of the custom json object stored in each query that should be returned", required = false) @QueryParam("additionalFields") List<String> additionalFields,
									  @ApiParam(value = "grouping fields of the custom json object stored in each query (currently only meant to be combined with no grouping by date)", required = false) @QueryParam("groupField") List<String> groupField,
									  @ApiParam(value = "filters for the custom json object stored in each entry", required = false) Map<String,String> filters
	) {
		try {
			validatePermissions(CCConstants.CCM_VALUE_TOOLPERMISSION_GLOBAL_STATISTICS_NODES, mediacenter);
			List<TrackingNode> tracks=AuthenticationUtil.runAsSystem(()->
					TrackingDAO.getNodeStatistics(grouping,new Date(dateFrom),new Date(dateTo),mediacenter,additionalFields,groupField,filters)
			);
			return Response.ok().entity(tracks).build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}

	@GET
	@Path("/statistics/nodes/altered")
	@ApiOperation(value = "get the range of nodes which had tracked actions since a given timestamp",
			notes = "requires admin"
	)

	@ApiResponses(value = {
			@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = String[].class),
			@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
			@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
			@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
			@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) })
	public Response getNodesAlteredInRange(@Context HttpServletRequest req,
									  @ApiParam(value = "date range from", required = true) @QueryParam("dateFrom") Long dateFrom
	) {
		try {
			if(!AuthorityServiceHelper.isAdmin()){
				throw new NotAnAdminException();
			}
			List<String> tracks=TrackingDAO.getNodesAltered(new Date(dateFrom));
			return Response.ok().entity(tracks).build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}

	@GET
	@Path("/statistics/nodes/node/{id}")
	@ApiOperation(value = "get the range of nodes which had tracked actions since a given timestamp",
			notes = "requires admin"
	)

	@ApiResponses(value = {
			@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = NodeData[].class),
			@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
			@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
			@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
			@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) })
	public Response getNodeData(@Context HttpServletRequest req,
										   @ApiParam(value = "node id to fetch data for", required = true) @PathParam("id") String id,
										   @ApiParam(value = "date range from", required = true) @QueryParam("dateFrom") Long dateFrom
	) {
		try {
			if(!AuthorityServiceHelper.isAdmin()){
				throw DAOException.mapping(new NotAnAdminException(), id);
			}
			List<NodeData> tracks=TrackingDAO.getNodeData(id, new Date(dateFrom));
			return Response.ok().entity(tracks).build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}

	private void validatePermissions(String toolpermission, String mediacenter) throws DAOException {
		if(mediacenter==null || mediacenter.isEmpty()) {
			ToolPermissionHelper.throwIfToolpermissionMissing(toolpermission);
		}else {
			// do NOT allow mediacenter persons to view user statistics
			ToolPermissionHelper.throwIfToolpermissionMissing(toolpermission);
			// MediacenterDao.get(RepositoryDao.getHomeRepository(), mediacenter).checkAdminAccess();
		}
	}

	@POST
	@Path("/statistics/users")

	@ApiOperation(value = "get statistics for user actions (login, logout)",
				  notes = "requires either toolpermission "+CCConstants.CCM_VALUE_TOOLPERMISSION_GLOBAL_STATISTICS_USER+" for global stats or to be admin of the requested mediacenter"
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
									  @ApiParam(value = "the mediacenter to filter for statistics", required = false) @QueryParam("mediacenter") String mediacenter,
									  @ApiParam(value = "additionals fields of the custom json object stored in each query that should be returned", required = false) @QueryParam("additionalFields") List<String> additionalFields,
									  @ApiParam(value = "grouping fields of the custom json object stored in each query (currently only meant to be combined with no grouping by date)", required = false) @QueryParam("groupField") List<String> groupField,
									  @ApiParam(value = "filters for the custom json object stored in each entry", required = false) Map<String,String> filters
	) {
		try {
			validatePermissions(CCConstants.CCM_VALUE_TOOLPERMISSION_GLOBAL_STATISTICS_USER, mediacenter);
			List<Tracking> tracks=AuthenticationUtil.runAsSystem(()->
					TrackingDAO.getUserStatistics(grouping,new Date(dateFrom),new Date(dateTo),mediacenter,additionalFields,groupField,filters)
			);
			return Response.ok().entity(tracks).build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}
}
