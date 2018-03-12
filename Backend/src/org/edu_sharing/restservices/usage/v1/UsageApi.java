package org.edu_sharing.restservices.usage.v1;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.restservices.ApiService;
import org.edu_sharing.restservices.DAOMissingException;
import org.edu_sharing.restservices.DAOSecurityException;
import org.edu_sharing.restservices.DAOValidationException;
import org.edu_sharing.restservices.RepositoryDao;
import org.edu_sharing.restservices.UsageDao;
import org.edu_sharing.restservices.collection.v1.model.Collection;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.edu_sharing.restservices.usage.v1.model.Usages;

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
		} catch (DAOValidationException t) {
			logger.warn(t.getMessage(), t);
			return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorResponse(t)).build();

		} catch (DAOSecurityException t) {
			logger.warn(t.getMessage(), t);
			return Response.status(Response.Status.FORBIDDEN).entity(new ErrorResponse(t)).build();

		} catch (DAOMissingException t) {
			logger.warn(t.getMessage(), t);
			return Response.status(Response.Status.NOT_FOUND).entity(new ErrorResponse(t)).build();

		} catch (Throwable t) {
			logger.error(t.getMessage(), t);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(t)).build();
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
		} catch (DAOValidationException t) {
			logger.warn(t.getMessage(), t);
			return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorResponse(t)).build();

		} catch (DAOSecurityException t) {
			logger.warn(t.getMessage(), t);
			return Response.status(Response.Status.FORBIDDEN).entity(new ErrorResponse(t)).build();

		} catch (DAOMissingException t) {
			logger.warn(t.getMessage(), t);
			return Response.status(Response.Status.NOT_FOUND).entity(new ErrorResponse(t)).build();

		} catch (Throwable t) {
			logger.error(t.getMessage(), t);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(t)).build();
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
		} catch (DAOValidationException t) {
			logger.warn(t.getMessage(), t);
			return Response.status(Response.Status.BAD_REQUEST).entity(new ErrorResponse(t)).build();

		} catch (DAOSecurityException t) {
			logger.warn(t.getMessage(), t);
			return Response.status(Response.Status.FORBIDDEN).entity(new ErrorResponse(t)).build();

		} catch (DAOMissingException t) {
			logger.warn(t.getMessage(), t);
			return Response.status(Response.Status.NOT_FOUND).entity(new ErrorResponse(t)).build();

		} catch (Throwable t) {
			logger.error(t.getMessage(), t);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(t)).build();
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
			List<Collection> collections = new UsageDao(RepositoryDao.getRepository(RepositoryDao.HOME)).getUsagesByNodeCollection(nodeId);
			return Response.status(Response.Status.OK).entity(collections).build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);

		}
	}


}
