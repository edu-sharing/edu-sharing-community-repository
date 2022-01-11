package org.edu_sharing.restservices.sharing.v1;

import io.swagger.annotations.*;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.apache.log4j.Logger;
import org.edu_sharing.restservices.*;
import org.edu_sharing.restservices.node.v1.NodeApi;
import org.edu_sharing.restservices.node.v1.model.NodeEntries;
import org.edu_sharing.restservices.node.v1.model.NodeEntry;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.edu_sharing.restservices.shared.Filter;
import org.edu_sharing.restservices.shared.Node;
import org.edu_sharing.restservices.shared.NodeRef;
import org.edu_sharing.restservices.sharing.v1.model.SharingInfo;
import org.edu_sharing.service.search.model.SortDefinition;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

@Path("/sharing/v1")
@Api(tags = {"SHARING v1"})
@ApiService(value="SHARING", major=1, minor=0)
public class SharingApi {


	private static Logger logger = Logger.getLogger(SharingApi.class);
	  @GET
	    @Path("/sharing/{repository}/{node}/{share}")
	        
	    @ApiOperation(
	    	value = "Get general info of a share."
		)
	    
	    @ApiResponses(
	    	value = { 
		        @ApiResponse(code = 200, message = RestConstants.HTTP_200, response = SharingInfo.class),
		        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
		        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
		        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
		        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
		        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
		    })

	    public Response getInfo(
	    	@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
			@ApiParam(value = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
			@ApiParam(value = "Share token",required=true ) @PathParam("share") String token,
			@ApiParam(value = "Password to validate (optional)",required=false, defaultValue = "") @QueryParam("password") String password,
			@Context HttpServletRequest req) {

		  try {
			  return AuthenticationUtil.runAsSystem(()-> {
				  RepositoryDao repoDao = RepositoryDao.getRepository(repository);
				  return Response.status(Response.Status.OK).entity(SharingDao.getInfo(repoDao, node, token, password)).build();
			  });
		  } catch (Throwable t) {
			  return ErrorResponse.createResponse(t);
		  }
	  }
	@GET
	@Path("/sharing/{repository}/{node}/{share}/children")

	@ApiOperation(
			value = "Get all children of this share.",
			notes = "Only valid for shared folders"
	)

	@ApiResponses(
			value = {
					@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = NodeEntries.class),
					@ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),
					@ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),
					@ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),
					@ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class),
					@ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class)
			})

	public Response getChildren(
			@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
			@ApiParam(value = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
			@ApiParam(value = "Share token",required=true ) @PathParam("share") String token,
			@ApiParam(value = "Password (required if share is locked)",required=false, defaultValue = "") @QueryParam("password") String password,
			@ApiParam(value = RestConstants.MESSAGE_MAX_ITEMS, defaultValue="500" ) @QueryParam("maxItems") Integer maxItems,
			@ApiParam(value = RestConstants.MESSAGE_SKIP_COUNT, defaultValue="0" ) @QueryParam("skipCount") Integer skipCount,
			@ApiParam(value = RestConstants.MESSAGE_SORT_PROPERTIES) @QueryParam("sortProperties") List<String> sortProperties,
			@ApiParam(value = RestConstants.MESSAGE_SORT_ASCENDING) @QueryParam("sortAscending") List<Boolean> sortAscending,
			@Context HttpServletRequest req) {

		try {
			return AuthenticationUtil.runAsSystem(()->{
				RepositoryDao repoDao = RepositoryDao.getRepository(repository);
				SortDefinition sortDefinition = new SortDefinition(sortProperties,sortAscending);
				List<NodeRef> children=SharingDao.getChildren(repoDao, node, token, password);
				List<String> filter=new ArrayList<>();
				filter.add("files");
				children=NodeDao.sortApiNodeRefs(repoDao,children,filter,sortDefinition);
				NodeEntries response = NodeDao.convertToRest(repoDao, Filter.createShowAllFilter(),children,skipCount==null ? 0 : skipCount,maxItems==null ? RestConstants.DEFAULT_MAX_ITEMS : maxItems);
				return Response.status(Response.Status.OK).entity(response).build();
			});
		} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
	}
}

