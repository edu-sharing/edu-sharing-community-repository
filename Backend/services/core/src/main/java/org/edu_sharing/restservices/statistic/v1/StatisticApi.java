package org.edu_sharing.restservices.statistic.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.restservices.*;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.edu_sharing.restservices.statistic.v1.model.Filter;
import org.edu_sharing.restservices.statistic.v1.model.Statistics;
import org.edu_sharing.restservices.tracking.v1.model.Tracking;
import org.edu_sharing.restservices.tracking.v1.model.TrackingNode;
import org.edu_sharing.service.NotAnAdminException;
import org.edu_sharing.service.authority.AuthorityServiceHelper;
import org.edu_sharing.service.statistic.StatisticsGlobal;
import org.edu_sharing.service.toolpermission.ToolPermissionHelper;
import org.edu_sharing.service.tracking.TrackingService;
import org.edu_sharing.service.tracking.ibatis.NodeData;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Path("/statistic/v1")
@Tag(name= "STATISTIC v1" )
@ApiService(value = "STATISTIC", major = 1, minor = 0)
@Consumes({ "application/json" })
@Produces({"application/json"})
public class StatisticApi {

	@POST
	@Path("/facets/{context}")
	@Operation(summary = "Get statistics of repository.", description = "Statistics.")

	@ApiResponses(value = { @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Statistics.class))), @ApiResponse(responseCode="400", description="Preconditions are not present.", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })
	public Response get(
			@Parameter(description = "context, the node where to start", required = true, schema = @Schema(defaultValue="-root-")) @PathParam("context") String context,
			@Parameter(description = "filter", required = true) Filter filter,
			@Parameter(description = "properties")
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
	    @Operation(summary = "Get stats.", description = "Get global statistics for this repository.")
	    
	    @ApiResponses(
	    	value = { 
				@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = StatisticsGlobal.class))),
		        @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),        
		        @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) 
	    	})

	    public Response getGlobalStatistics(
                @Parameter(description = "primary property to build facets and count+group values", required = false) @QueryParam("group") String group,
                @Parameter(description = "additional properties to build facets and count+sub-group values", required = false) @QueryParam("subGroup") List<String> subGroup) {

	    	try {
		    	StatisticsGlobal statistics=StatisticDao.getGlobal(group,subGroup);
		    	return Response.status(Response.Status.OK).entity(statistics).build();
		    	
			} catch (Throwable t) {
				return ErrorResponse.createResponse(t);				
			}
	
		}

	@POST
	@Path("/statistics/nodes")

	@Operation(summary = "get statistics for node actions", description = "requires either toolpermission "+CCConstants.CCM_VALUE_TOOLPERMISSION_GLOBAL_STATISTICS_NODES+" for global stats or to be admin of the requested mediacenter")

	@ApiResponses(value = {
			@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = TrackingNode[].class))),
			@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })
	public Response getStatisticsNode(@Context HttpServletRequest req,
									  @Parameter(description = "Grouping type (by date)", required = true) @QueryParam("grouping")TrackingService.GroupingType grouping,
									  @Parameter(description = "date range from", required = true) @QueryParam("dateFrom") Long dateFrom,
									  @Parameter(description = "date range to", required = true) @QueryParam("dateTo") Long dateTo,
									  @Parameter(description = "the mediacenter to filter for statistics", required = false) @QueryParam("mediacenter") String mediacenter,
									  @Parameter(description = "additionals fields of the custom json object stored in each query that should be returned", required = false) @QueryParam("additionalFields") List<String> additionalFields,
									  @Parameter(description = "grouping fields of the custom json object stored in each query (currently only meant to be combined with no grouping by date)", required = false) @QueryParam("groupField") List<String> groupField,
									  @Parameter(description = "filters for the custom json object stored in each entry", required = false) Map<String,String> filters
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
	@Operation(summary = "get the range of nodes which had tracked actions since a given timestamp", description = "requires admin")

	@ApiResponses(value = {
			@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = String[].class))),
			@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })
	public Response getNodesAlteredInRange(@Context HttpServletRequest req,
									  @Parameter(description = "date range from", required = true) @QueryParam("dateFrom") Long dateFrom
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
	@Operation(summary = "get the range of nodes which had tracked actions since a given timestamp", description = "requires admin")

	@ApiResponses(value = {
			@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = NodeData[].class))),
			@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })
	public Response getNodeData(@Context HttpServletRequest req,
										   @Parameter(description = "node id to fetch data for", required = true) @PathParam("id") String id,
										   @Parameter(description = "date range from", required = true) @QueryParam("dateFrom") Long dateFrom
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

	@Operation(summary = "get statistics for user actions (login, logout)", description = "requires either toolpermission "+CCConstants.CCM_VALUE_TOOLPERMISSION_GLOBAL_STATISTICS_USER+" for global stats or to be admin of the requested mediacenter")
			@ApiResponses(value = {
			@ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Tracking[].class))),
			@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })
	public Response getStatisticsUser(@Context HttpServletRequest req,
									  @Parameter(description = "Grouping type (by date)", required = true) @QueryParam("grouping")TrackingService.GroupingType grouping,
									  @Parameter(description = "date range from", required = true) @QueryParam("dateFrom") Long dateFrom,
									  @Parameter(description = "date range to", required = true) @QueryParam("dateTo") Long dateTo,
									  @Parameter(description = "the mediacenter to filter for statistics", required = false) @QueryParam("mediacenter") String mediacenter,
									  @Parameter(description = "additionals fields of the custom json object stored in each query that should be returned", required = false) @QueryParam("additionalFields") List<String> additionalFields,
									  @Parameter(description = "grouping fields of the custom json object stored in each query (currently only meant to be combined with no grouping by date)", required = false) @QueryParam("groupField") List<String> groupField,
									  @Parameter(description = "filters for the custom json object stored in each entry", required = false) Map<String,String> filters
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
