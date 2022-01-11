package org.edu_sharing.restservices.tool.v1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.NameSpaceTool;
import org.edu_sharing.restservices.ApiService;
import org.edu_sharing.restservices.DAOException;
import org.edu_sharing.restservices.NodeDao;
import org.edu_sharing.restservices.RepositoryDao;
import org.edu_sharing.restservices.RestConstants;
import org.edu_sharing.restservices.ToolDao;
import org.edu_sharing.restservices.node.v1.model.NodeEntries;
import org.edu_sharing.restservices.node.v1.model.NodeEntry;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.edu_sharing.restservices.shared.Node;
import org.edu_sharing.restservices.shared.NodeRef;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Path("/tool/v1")
@Api(tags = { "TOOL v1" })
@ApiService(value = "TOOL", major = 1, minor = 0)
public class ToolApi {

	private static Logger logger = Logger.getLogger(ToolApi.class);

	@POST
	@Path("/tools/{repository}/tooldefinitions")

	@ApiOperation(value = "Create a new tool definition object.", notes = "Create a new tool definition object.")

	@ApiResponses(value = { @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = NodeEntry.class),
			@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
			@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
			@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
			@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
			@ApiResponse(code = 409, message = RestConstants.HTTP_409, response = ErrorResponse.class),
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) })

	public Response createToolDefintition(
			@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID, required = true, defaultValue = "-home-") @PathParam("repository") String repository,
			@ApiParam(value = "rename if the same node name exists", required = false, defaultValue = "false") @QueryParam("renameIfExists") Boolean renameIfExists,
			@ApiParam(value = "comment, leave empty = no inital version", required = false) @QueryParam("versionComment") String versionComment,
			@ApiParam(value = "properties, example: {\"{http://www.alfresco.org/model/content/1.0}name\": [\"test\"]}", required = true) HashMap<String, String[]> properties,
			@Context HttpServletRequest req) {

		try {
			NodeDao child = new ToolDao().createToolDefinition(repository, renameIfExists, versionComment, properties);
			NodeEntry response = new NodeEntry();
			response.setNode(child.asNode());
			return Response.status(Response.Status.OK).entity(response).build();
		}catch(DAOException e) {
			return ErrorResponse.createResponse(e);
		}
		
		

	}

	@POST
	@Path("/tools/{repository}/{toolDefinition}/toolinstances")

	@ApiOperation(value = "Create a new tool Instance object.", notes = "Create a new tool Instance object.")

	@ApiResponses(value = { @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = NodeEntry.class),
			@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
			@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
			@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
			@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
			@ApiResponse(code = 409, message = RestConstants.HTTP_409, response = ErrorResponse.class),
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) })

	public Response createToolInstance(
			@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID, required = true, defaultValue = "-home-") @PathParam("repository") String repository,
			@ApiParam(value = RestConstants.MESSAGE_PARENT_NODE
					+ " must have tool_definition aspect", required = true) @PathParam("toolDefinition") String toolDefinition,
			@ApiParam(value = "rename if the same node name exists", required = false, defaultValue = "false") @QueryParam("renameIfExists") Boolean renameIfExists,
			@ApiParam(value = "comment, leave empty = no inital version", required = false) @QueryParam("versionComment") String versionComment,
			@ApiParam(value = "properties, example: {\"{http://www.alfresco.org/model/content/1.0}name\": [\"test\"]}", required = true) HashMap<String, String[]> properties,
			@Context HttpServletRequest req) {

		try {
			NodeDao child = new ToolDao().createToolInstance(repository, toolDefinition, renameIfExists, versionComment, properties);
			NodeEntry response = new NodeEntry();
			response.setNode(child.asNode());
			return Response.status(Response.Status.OK).entity(response).build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}
	@POST
	@Path("/tools/{repository}/{toolinstance}/toolobject")


	@ApiOperation(value = "Create a new tool object for a given tool instance.", notes = "Create a new tool object for a given tool instance.")

	@ApiResponses(value = { @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = NodeEntry.class),
			@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
			@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
			@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
			@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
			@ApiResponse(code = 409, message = RestConstants.HTTP_409, response = ErrorResponse.class),
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) })

	public Response createToolObject(
			@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID, required = true, defaultValue = "-home-") @PathParam("repository") String repository,
			@ApiParam(value = RestConstants.MESSAGE_PARENT_NODE
					+ " (a tool instance object)", required = true) @PathParam("toolinstance") String toolinstance,
			@ApiParam(value = "rename if the same node name exists", required = false, defaultValue = "false") @QueryParam("renameIfExists") Boolean renameIfExists,
			@ApiParam(value = "comment, leave empty = no inital version", required = false) @QueryParam("versionComment") String versionComment,
			@ApiParam(value = "properties, example: {\"{http://www.alfresco.org/model/content/1.0}name\": [\"test\"]}", required = true) HashMap<String, String[]> properties,
			@Context HttpServletRequest req) {

		try {
			RepositoryDao repoDao = RepositoryDao.getRepository(repository);
			NodeDao nodeDao = NodeDao.getNode(repoDao, toolinstance);

			// TODO: Check node type of parent is Tool instance
			// nodeDao.asNode().getType()
			
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
		return Response.noContent().build();
		/*
		return create(repository, toolinstance, renameIfExists, versionComment, properties,
				CCConstants.CCM_TYPE_TOOL_, null, CCConstants.CCM_ASSOC_TOOL_INSTANCES);
		*/

	}


	@GET
	@Path("/tools/{repository}/{toolDefinition}/toolinstances")

	@ApiOperation(value = "Get Instances of a ToolDefinition.", notes = "Get Instances of a ToolDefinition.")

	@ApiResponses(value = { @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = NodeEntry.class),
			@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
			@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
			@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
			@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) })

	public Response getInstances(
			@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID, required = true, defaultValue = "-home-") @PathParam("repository") String repository,
			@ApiParam(value = RestConstants.MESSAGE_NODE_ID, required = true) @PathParam("toolDefinition") String toolDefinition,
			@Context HttpServletRequest req) {

		try {
			
			List<Node> result = new ToolDao().getInstances(repository, toolDefinition);
			NodeEntries response = new NodeEntries();
			response.setNodes(result);

			return Response.status(Response.Status.OK).entity(response).build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}


	}
	
	@GET
	@Path("/tools/{repository}/{nodeid}/toolinstance")

	@ApiOperation(value = "Get Instances of a ToolDefinition.", notes = "Get Instances of a ToolDefinition.")

	@ApiResponses(value = { @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = NodeEntry.class),
			@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
			@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
			@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
			@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) })

	public Response getInstance(
			@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID, required = true, defaultValue = "-home-") @PathParam("repository") String repository,
			@ApiParam(value = RestConstants.MESSAGE_NODE_ID, required = true) @PathParam("nodeid") String nodeid,
			@Context HttpServletRequest req) {

		try {
			
			Node result = new ToolDao().getInstance(repository, nodeid);
			NodeEntry response = new NodeEntry();
			response.setNode(result);

			return Response.status(Response.Status.OK).entity(response).build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}


	}
	
	@GET
	@Path("/tools/{repository}/tooldefinitions")

	@ApiOperation(value = "Get all ToolDefinitions.", notes = "Get all ToolDefinitions.")

	@ApiResponses(value = { @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = NodeEntry.class),
			@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
			@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
			@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
			@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) })

	public Response getAllToolDefinitions(
			@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID, required = true, defaultValue = "-home-") @PathParam("repository") String repository,
			@Context HttpServletRequest req) {

		try {
			
			List<Node> result = new ToolDao().getAllToolDefinitions(repository);
			NodeEntries response = new NodeEntries();
			response.setNodes(result);

			return Response.status(Response.Status.OK).entity(response).build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}


	}

}
