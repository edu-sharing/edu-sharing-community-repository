package org.edu_sharing.restservices.relation.v1;

import groovy.util.logging.Log4j;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.alfresco.rest.api.model.NodeRating;
import org.edu_sharing.restservices.ApiService;
import org.edu_sharing.restservices.RelationDao;
import org.edu_sharing.restservices.RepositoryDao;
import org.edu_sharing.restservices.RestConstants;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.edu_sharing.restservices.shared.NodeRelation;
import org.edu_sharing.service.relations.InputRelationType;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

@Path("/relation/v1")
@Tag(name="RELATION v1")
@ApiService(value = "RELATION", major = 1, minor = 0)
@Consumes({"application/json"})
@Produces({"application/json"})
@Log4j
public class RelationApi {
    @PUT
    @Path("/relation/{repository}/{source}/{type}/{target}")
    @Operation(summary = "create a relation between nodes", description = "Creates a relation between two nodes of the given type.")
    @ApiResponses({
            @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema())),
            @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response createRelation(
            @Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
            @Parameter(description = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("source") String source,
            @Parameter(description = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("type") InputRelationType type,
            @Parameter(description = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("target") String target
    ){
        try{
            RepositoryDao repoDao = RepositoryDao.getRepository(repository);
            RelationDao relationDao = new RelationDao(repoDao);
            relationDao.createRelation(source, target, type);
            return Response.status(Response.Status.OK).build();
        }catch (Throwable t) {
            return ErrorResponse.createResponse(t);
        }
    }

    @DELETE
    @Path("/relation/{repository}/{source}/{type}/{target}")
    @Operation(summary = "delete a relation between nodes", description = "Delete a relation between two nodes of the given type.")
    @ApiResponses({
            @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema())),
            @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response deleteRelation(
            @Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
            @Parameter(description = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("source") String source,
            @Parameter(description = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("type") InputRelationType type,
            @Parameter(description = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("target") String target
    ){
        try{
            RepositoryDao repoDao = RepositoryDao.getRepository(repository);
            RelationDao relationDao = new RelationDao(repoDao);
            relationDao.deleteRelation(source, target, type);
            return Response.status(Response.Status.OK).build();
        }catch (Throwable t) {
            return ErrorResponse.createResponse(t);
        }
    }

    @GET
    @Path("/relation/{repository}/{node}")
    @Operation(summary = "get all relation of the node", description = "Returns all relations of the node.")
    @ApiResponses({
            @ApiResponse(responseCode="200", description=RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = NodeRelation.class))),
            @ApiResponse(responseCode="400", description=RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode="401", description=RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode="403", description=RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode="404", description=RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode="500", description=RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response getRelations(
            @Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue="-home-" )) @PathParam("repository") String repository,
            @Parameter(description = RestConstants.MESSAGE_NODE_ID,required=true ) @PathParam("node") String node
    ){
        try{
            RepositoryDao repoDao = RepositoryDao.getRepository(repository);
            RelationDao relationDao = new RelationDao(repoDao);
            NodeRelation nodeRelation = relationDao.getRelations(node);
            return Response.ok().entity(nodeRelation).build();
        }catch (Throwable t) {
            return ErrorResponse.createResponse(t);
        }
    }
}
