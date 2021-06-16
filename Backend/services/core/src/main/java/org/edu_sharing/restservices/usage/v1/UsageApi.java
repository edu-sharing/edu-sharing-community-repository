package org.edu_sharing.restservices.usage.v1;

import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.restservices.ApiService;
import org.edu_sharing.restservices.RepositoryDao;
import org.edu_sharing.restservices.UsageDao;
import org.edu_sharing.restservices.collection.v1.model.Collection;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.edu_sharing.restservices.usage.v1.model.Usages;
import org.edu_sharing.service.toolpermission.ToolPermissionHelper;
import org.edu_sharing.service.toolpermission.ToolPermissionService;
import org.edu_sharing.service.toolpermission.ToolPermissionServiceFactory;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Path("/usage/v1")
@Api(tags = { "USAGE v1" })
@ApiService(value = "USAGE", major = 1, minor = 0)
public class UsageApi {

	private static Logger logger = Logger.getLogger(UsageApi.class);

	@GET
	@Path("/usages/{appId}")

	@ApiOperation(value = "Get all usages of an application.", notes = "Get all usages of an application.")

	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK.", response = Usages.class),
			@ApiResponse(code = 400, message = "Preconditions are not present.", response = ErrorResponse.class),
			@ApiResponse(code = 401, message = "Authorization failed.", response = ErrorResponse.class),
			@ApiResponse(code = 403, message = "Session user has insufficient rights to perform this operation.", response = ErrorResponse.class),
			@ApiResponse(code = 404, message = "Ressources are not found.", response = ErrorResponse.class),
			@ApiResponse(code = 500, message = "Fatal error occured.", response = ErrorResponse.class) })

	public Response getUsages(@ApiParam(value = "ID of application (or \"-home-\" for home repository)", required = true) @PathParam("appId") String appId) {
		try {
			
			
			ApplicationInfo appInfo = ApplicationInfoList.getRepositoryInfoById(appId);
			
			if (appInfo == null) {
				return Response.status(Response.Status.NOT_FOUND).build();
			}
			
			
			return Response.status(Response.Status.OK).entity(new Usages(new UsageDao(RepositoryDao.getRepository(RepositoryDao.HOME)).getUsages(appId))).build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}

	@OPTIONS
	@Path("/usages/{appId}")
	@ApiOperation(hidden = true, value = "")
	public Response options1() {
		return Response.status(Response.Status.OK).header("Allow", "OPTIONS,  GET").build();
	}

	@GET
	@Path("/usages/course/{appId}/{courseId}")

	@ApiOperation(value = "Get all usages of an course.", notes = "Get all usages of an course.")

	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK.", response = Usages.class),
			@ApiResponse(code = 400, message = "Preconditions are not present.", response = ErrorResponse.class),
			@ApiResponse(code = 401, message = "Authorization failed.", response = ErrorResponse.class),
			@ApiResponse(code = 403, message = "Session user has insufficient rights to perform this operation.", response = ErrorResponse.class),
			@ApiResponse(code = 404, message = "Ressources are not found.", response = ErrorResponse.class),
			@ApiResponse(code = 500, message = "Fatal error occured.", response = ErrorResponse.class) })

	public Response getUsagesByCourse(
			@ApiParam(value = "ID of application (or \"-home-\" for home repository)", required = true) @PathParam("appId") String appId,
			@ApiParam(value = "ID of course", required = true) @PathParam("courseId") String courseId,
			@Context HttpServletRequest req) {
		try {
			
			ApplicationInfo appInfo = ApplicationInfoList.getRepositoryInfoById(appId);
			
			if (appInfo == null) {
				return Response.status(Response.Status.NOT_FOUND).build();
			}
			
			return Response.status(Response.Status.OK).entity(new Usages(new UsageDao(RepositoryDao.getRepository(RepositoryDao.HOME)).getUsagesByCourse(appId, courseId)))
					.build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}

	@OPTIONS
	@Path("/usages/course/{appId}/{courseId}")
	@ApiOperation(hidden = true, value = "")
	public Response options2() {
		return Response.status(Response.Status.OK).header("Allow", "OPTIONS,  GET").build();
	}
	
	
	@GET
	@Path("/usages/node/{nodeId}")

	@ApiOperation(value = "Get all usages of an node.", notes = "Get all usages of an node.")

	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK.", response = Usages.class),
			@ApiResponse(code = 400, message = "Preconditions are not present.", response = ErrorResponse.class),
			@ApiResponse(code = 401, message = "Authorization failed.", response = ErrorResponse.class),
			@ApiResponse(code = 403, message = "Session user has insufficient rights to perform this operation.", response = ErrorResponse.class),
			@ApiResponse(code = 404, message = "Ressources are not found.", response = ErrorResponse.class),
			@ApiResponse(code = 500, message = "Fatal error occured.", response = ErrorResponse.class) })

	public Response getUsagesByNode(
			@ApiParam(value = "ID of node", required = true) @PathParam("nodeId") String nodeId,
			@Context HttpServletRequest req) {
		try {
			return Response.status(Response.Status.OK).entity(new Usages(new UsageDao(RepositoryDao.getRepository(RepositoryDao.HOME)).getUsagesByNode(nodeId)))
					.build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}

	@OPTIONS
	@Path("/usages/node/{nodeId}")
	@ApiOperation(hidden = true, value = "")
	public Response options3() {
		return Response.status(Response.Status.OK).header("Allow", "OPTIONS,  GET").build();
	}
	
	@GET
	@Path("/usages/node/{nodeId}/collections")

	@ApiOperation(value = "Get all collections where this node is used.")

	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK.", response = Collection[].class),
			@ApiResponse(code = 400, message = "Preconditions are not present.", response = ErrorResponse.class),
			@ApiResponse(code = 401, message = "Authorization failed.", response = ErrorResponse.class),
			@ApiResponse(code = 403, message = "Session user has insufficient rights to perform this operation.", response = ErrorResponse.class),
			@ApiResponse(code = 404, message = "Ressources are not found.", response = ErrorResponse.class),
			@ApiResponse(code = 500, message = "Fatal error occured.", response = ErrorResponse.class) })

	public Response getUsagesByNodeCollections(
			@ApiParam(value = "ID of node", required = true) @PathParam("nodeId") String nodeId,
			@Context HttpServletRequest req) {
		try {
			Set<Usages.CollectionUsage> collections = new UsageDao(RepositoryDao.getRepository(RepositoryDao.HOME)).getUsagesByNodeCollection(nodeId);
			return Response.status(Response.Status.OK).entity(collections).build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}

	@DELETE
	@Path("/usages/node/{nodeId}/{usageId}")

	@ApiOperation(value = "Delete an usage of a node.")

	@ApiResponses(value = { @ApiResponse(code = 200, message = "OK.", response = Usages.class),
			@ApiResponse(code = 400, message = "Preconditions are not present.", response = ErrorResponse.class),
			@ApiResponse(code = 401, message = "Authorization failed.", response = ErrorResponse.class),
			@ApiResponse(code = 403, message = "Session user has insufficient rights to perform this operation.", response = ErrorResponse.class),
			@ApiResponse(code = 404, message = "Ressources are not found.", response = ErrorResponse.class),
			@ApiResponse(code = 500, message = "Fatal error occured.", response = ErrorResponse.class) })

	public Response deleteUsage(
			@ApiParam(value = "ID of node", required = true) @PathParam("nodeId") String nodeId,
			@ApiParam(value = "ID of usage", required = true) @PathParam("usageId") String usageId,
			@Context HttpServletRequest req) {
		try {
			new UsageDao(RepositoryDao.getRepository(RepositoryDao.HOME)).deleteUsage(nodeId,usageId);
			return Response.status(Response.Status.OK).build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}
	
	@GET
	@Path("/usages/repository/{repositoryId}/{nodeid}")
	public Response getUsages(@ApiParam(value = "ID of repository", required = true, defaultValue=RepositoryDao.HOME)  @PathParam("repositoryId") String repositoryId,
			@ApiParam(value = "ID of node. Use -all- for getting usages of all nodes", required = true, defaultValue="-all-") @PathParam("nodeId") String nodeId,
			@ApiParam(value = "from date", required = false) @QueryParam("from") Long from,
			@ApiParam(value = "to date", required = false) @QueryParam("to") Long to,
			@Context HttpServletRequest req) {
		
		try {
			RepositoryDao homeRepo = RepositoryDao.getRepository(RepositoryDao.HOME);
			if(RepositoryDao.HOME.equals(repositoryId)) {
				repositoryId = homeRepo.getId();
			}
			
			if("-all-".equals(nodeId)) {
				nodeId = null;
			}
			ToolPermissionHelper.throwIfToolpermissionMissing(CCConstants.CCM_VALUE_TOOLPERMISSION_USAGE_STATISTIC);
			
			List<Usages.NodeUsage> usages = new UsageDao(homeRepo).
					getUsages(repositoryId, nodeId, from, to);
			return Response.status(Response.Status.OK).entity(usages).build();

		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
		
	}
	
	
}
