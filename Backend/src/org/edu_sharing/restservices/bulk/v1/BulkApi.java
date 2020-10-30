package org.edu_sharing.restservices.bulk.v1;

import io.swagger.annotations.*;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.log4j.Logger;
import org.edu_sharing.restservices.*;
import org.edu_sharing.restservices.node.v1.model.NodeEntry;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.edu_sharing.restservices.shared.Filter;
import org.edu_sharing.service.bulk.BulkServiceFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

@Path("/bulk/v1")
@Api(tags = { "BULK v1" })
@ApiService(value = "BULK", major = 1, minor = 0)
public class BulkApi {
	private static Logger logger = Logger.getLogger(BulkApi.class);

	@PUT
	@Path("/sync/{group}")

	@ApiOperation(value = "Create or update a given node", notes = "Depending on the given \"match\" properties either a new node will be created or the existing one will be updated")

	@ApiResponses(value = { @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = NodeEntry.class),
			@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
			@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
			@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
			@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) })
	public Response sync(@Context HttpServletRequest req,
		   @ApiParam(value = "The group to which this node belongs to. Used for internal structuring. Please use simple names only", required = true) @PathParam("group") String group,
		   @ApiParam(value = "The properties that must match to identify if this node exists. Multiple properties will be and combined and compared", required = true) @QueryParam("match") List<String> match,
		   @ApiParam(value = "The properties on which the imported nodes should be grouped (for each value, a folder with the corresponding data is created)", required = false) @QueryParam("groupBy") List<String> groupBy,
		   @ApiParam(value = "type of node. If the node already exists, this will not change the type afterwards",required=true ) @QueryParam("type") String type,
		   @ApiParam(value = "aspects of node" ) @QueryParam("aspects") List<String> aspects,
		   @ApiParam(value = "properties, they'll not get filtered via mds, so be careful what you add here" , required=true) HashMap<String, String[]> properties,
		   @ApiParam(value = "reset all versions (like a complete reimport), all data inside edu-sharing will be lost" , required=false) @QueryParam("resetVersion") Boolean resetVersion

	) {
		try {
			NodeDao nodeDao = NodeDao.getNode(RepositoryDao.getHomeRepository(),
					BulkServiceFactory.getInstance().sync(group, match, groupBy, type, aspects, properties, resetVersion==null ? false : resetVersion).getId(),
					Filter.createShowAllFilter());
			NodeEntry entry = new NodeEntry();
			entry.setNode(nodeDao.asNode());
			return Response.ok().entity(entry).build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}


	@POST
	@Path("/find")

	@ApiOperation(value = "gets a given node", notes = "Get a given node based on the posted, multiple criterias. Make sure that they'll provide an unique result")

	@ApiResponses(value = { @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = NodeEntry.class),
			@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
			@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
			@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
			@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
			@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) })
	public Response find(@Context HttpServletRequest req,
						 @ApiParam(value = "properties that must match (with \"AND\" concatenated)" , required=true ) HashMap<String, String[]> properties
	) {
		try {
			NodeRef node = BulkServiceFactory.getInstance().find(properties);
			if(node==null) {
				throw new DAOMissingException(new Throwable("No node matched the criteria"));
			}
			NodeDao nodeDao = NodeDao.getNode(RepositoryDao.getHomeRepository(), node.getId(), Filter.createShowAllFilter());
			NodeEntry entry = new NodeEntry();
			entry.setNode(nodeDao.asNode());
			return Response.ok().entity(entry).build();
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t, ErrorResponse.ErrorResponseLogging.relaxed);
		}
	}
}
