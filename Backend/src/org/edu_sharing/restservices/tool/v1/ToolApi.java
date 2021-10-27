package org.edu_sharing.restservices.tool.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.log4j.Logger;
import org.edu_sharing.restservices.*;
import org.edu_sharing.restservices.node.v1.model.NodeEntries;
import org.edu_sharing.restservices.node.v1.model.NodeEntry;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.edu_sharing.restservices.shared.Node;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;

@Path("/tool/v1")
@Tag(name= "TOOL v1" )
@ApiService(value = "TOOL", major = 1, minor = 0)
@Consumes({ "application/json" })
@Produces({"application/json"})
public class ToolApi {

	private static Logger logger = Logger.getLogger(ToolApi.class);

	@POST
	@Path("/tools/{repository}/tooldefinitions")

	@Operation(summary = "Create a new tool definition object.", description = "Create a new tool definition object.")

	@ApiResponses(value = { @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = NodeEntry.class))),
			@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="409", description=RestConstants.HTTP_409, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })

	public Response createToolDefintition(
			@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-")) @PathParam("repository") String repository,
			@Parameter(description = "rename if the same node name exists", required = false, schema = @Schema(defaultValue="false")) @QueryParam("renameIfExists") Boolean renameIfExists,
			@Parameter(description = "comment, leave empty = no inital version", required = false) @QueryParam("versionComment") String versionComment,
			@Parameter(description = "properties, example: {\"{http://www.alfresco.org/model/content/1.0}name\": [\"test\"]}", required = true) HashMap<String, String[]> properties,
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

	@Operation(summary = "Create a new tool Instance object.", description = "Create a new tool Instance object.")

	@ApiResponses(value = { @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = NodeEntry.class))),
			@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="409", description=RestConstants.HTTP_409, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })

	public Response createToolInstance(
			@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-")) @PathParam("repository") String repository,
			@Parameter(description = RestConstants.MESSAGE_PARENT_NODE
					+ " must have tool_definition aspect", required = true) @PathParam("toolDefinition") String toolDefinition,
			@Parameter(description = "rename if the same node name exists", required = false, schema = @Schema(defaultValue="false")) @QueryParam("renameIfExists") Boolean renameIfExists,
			@Parameter(description = "comment, leave empty = no inital version", required = false) @QueryParam("versionComment") String versionComment,
			@Parameter(description = "properties, example: {\"{http://www.alfresco.org/model/content/1.0}name\": [\"test\"]}", required = true) HashMap<String, String[]> properties,
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


	@Operation(summary = "Create a new tool object for a given tool instance.", description = "Create a new tool object for a given tool instance.")

	@ApiResponses(value = { @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = NodeEntry.class))),
			@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="409", description=RestConstants.HTTP_409, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })

	public Response createToolObject(
			@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-")) @PathParam("repository") String repository,
			@Parameter(description = RestConstants.MESSAGE_PARENT_NODE
					+ " (a tool instance object)", required = true) @PathParam("toolinstance") String toolinstance,
			@Parameter(description = "rename if the same node name exists", required = false, schema = @Schema(defaultValue="false")) @QueryParam("renameIfExists") Boolean renameIfExists,
			@Parameter(description = "comment, leave empty = no inital version", required = false) @QueryParam("versionComment") String versionComment,
			@Parameter(description = "properties, example: {\"{http://www.alfresco.org/model/content/1.0}name\": [\"test\"]}", required = true) HashMap<String, String[]> properties,
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

	@Operation(summary = "Get Instances of a ToolDefinition.", description = "Get Instances of a ToolDefinition.")

	@ApiResponses(value = { @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = NodeEntry.class))),
			@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })

	public Response getInstances(
			@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-")) @PathParam("repository") String repository,
			@Parameter(description = RestConstants.MESSAGE_NODE_ID, required = true) @PathParam("toolDefinition") String toolDefinition,
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

	@Operation(summary = "Get Instances of a ToolDefinition.", description = "Get Instances of a ToolDefinition.")

	@ApiResponses(value = { @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = NodeEntry.class))),
			@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })

	public Response getInstance(
			@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-")) @PathParam("repository") String repository,
			@Parameter(description = RestConstants.MESSAGE_NODE_ID, required = true) @PathParam("nodeid") String nodeid,
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

	@Operation(summary = "Get all ToolDefinitions.", description = "Get all ToolDefinitions.")

	@ApiResponses(value = { @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = NodeEntry.class))),
			@ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })

	public Response getAllToolDefinitions(
			@Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-")) @PathParam("repository") String repository,
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
