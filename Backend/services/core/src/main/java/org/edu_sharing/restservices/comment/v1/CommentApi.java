package org.edu_sharing.restservices.comment.v1;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Context;


import org.apache.log4j.Logger;
import org.edu_sharing.restservices.ApiService;
import org.edu_sharing.restservices.CommentDao;
import org.edu_sharing.restservices.RepositoryDao;
import org.edu_sharing.restservices.RestConstants;
import org.edu_sharing.restservices.comment.v1.model.Comments;
import org.edu_sharing.restservices.shared.ErrorResponse;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Path("/comment/v1")
@Api(tags = { "COMMENT v1" })
@ApiService(value = "COMMENT", major = 1, minor = 0)
public class CommentApi {	
	private static Logger logger = Logger.getLogger(CommentApi.class);
	@PUT
	@Path("/comments/{repository}/{node}")
	@ApiOperation(value = "create a new comment", notes = "Adds a comment to the given node")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Void.class),
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })
	public Response addComment(
			@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
	    	@ApiParam(value = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
	    	@ApiParam(value = "In reply to an other comment, can be null",required=false) @QueryParam("commentReference") String commentReference,
	    	@ApiParam(value = "Text content of comment",required=true) String comment,
			@Context HttpServletRequest req) {
    	try {
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	new CommentDao(repoDao).addComment(node, commentReference, comment);
	    	return Response.status(Response.Status.OK).build();
    	} catch (Throwable t) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(t)).build();
		}
    }
	@POST
	@Path("/comments/{repository}/{comment}")
	@ApiOperation(value = "edit a comment", notes = "Edit the comment with the given id")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Void.class),
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })
	public Response editComment(
			@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
	    	@ApiParam(value = "id of the comment to edit",required=true ) @PathParam("comment") String commentId,
	    	@ApiParam(value = "Text content of comment",required=true) String comment,
			@Context HttpServletRequest req) {
    	try {
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	new CommentDao(repoDao).editComment(commentId, comment);
	    	return Response.status(Response.Status.OK).build();
    	} catch (Throwable t) {
    		return ErrorResponse.createResponse(t);
		}
    }
	@DELETE
	@Path("/comments/{repository}/{comment}")
	@ApiOperation(value = "delete a comment", notes = "Delete the comment with the given id")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Void.class),
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })
	public Response deleteComment(
			@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
	    	@ApiParam(value = "id of the comment to delete",required=true ) @PathParam("comment") String commentId,
			@Context HttpServletRequest req) {
    	try {
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	new CommentDao(repoDao).deleteComment(commentId);
	    	return Response.status(Response.Status.OK).build();
    	} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
    }
	@GET
	@Path("/comments/{repository}/{node}")
	@ApiOperation(value = "list comments", notes = "List all comments")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = RestConstants.HTTP_200, response = Comments.class),
	        @ApiResponse(code = 400, message = RestConstants.HTTP_400, response = ErrorResponse.class),        
	        @ApiResponse(code = 401, message = RestConstants.HTTP_401, response = ErrorResponse.class),        
	        @ApiResponse(code = 403, message = RestConstants.HTTP_403, response = ErrorResponse.class),        
	        @ApiResponse(code = 404, message = RestConstants.HTTP_404, response = ErrorResponse.class), 
	        @ApiResponse(code = 500, message = RestConstants.HTTP_500, response = ErrorResponse.class) 
	    })
	public Response getComments(
			@ApiParam(value = RestConstants.MESSAGE_REPOSITORY_ID,required=true, defaultValue="-home-" ) @PathParam("repository") String repository,
	    	@ApiParam(value = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node,
			@Context HttpServletRequest req) {
    	try {
	    	RepositoryDao repoDao = RepositoryDao.getRepository(repository);
	    	Comments comments=new CommentDao(repoDao).getComments(node);
	    	return Response.status(Response.Status.OK).entity(comments).build();
    	} catch (Throwable t) {
			return ErrorResponse.createResponse(t);
		}
    }
}
