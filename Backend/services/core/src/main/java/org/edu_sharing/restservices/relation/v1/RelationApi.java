package org.edu_sharing.restservices.relation.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.StoreRef;
import org.edu_sharing.restservices.ApiService;
import org.edu_sharing.restservices.RelationDao;
import org.edu_sharing.restservices.RepositoryDao;
import org.edu_sharing.restservices.RestConstants;
import org.edu_sharing.restservices.relation.v1.model.CreateRelationRequest;
import org.edu_sharing.restservices.relation.v1.model.UpdateRelationRequest;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.edu_sharing.restservices.relation.v1.model.NodeRelationData;
import org.edu_sharing.service.relations.InputRelationType;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.edu_sharing.service.relations.RelationData;
import org.edu_sharing.service.tracking.ActivityEventService;
import org.edu_sharing.service.tracking.ActivityOnNodeEventType;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Path("/relation/v1")
@Tag(name = "RELATION v1")
@ApiService(value = "RELATION", major = 1)
@Consumes({"application/json"})
@Produces({"application/json"})
@Slf4j
public class RelationApi {

    @Autowired
    private ActivityEventService activityEventService;

    @POST
    @Path("/{repository}")
    @Operation(summary = "create a relation between nodes", description = "Creates a relation between two nodes of the given type.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = NodeRelationData.class))),
            @ApiResponse(responseCode = "400", description = RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response createRelation(
            @Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue = "-home-")) @PathParam("repository") String repository,
            @Valid CreateRelationRequest request
    ) {
        RepositoryDao repoDao = RepositoryDao.getRepository(repository);
        RelationDao relationDao = new RelationDao(repoDao);
        NodeRelationData relation = relationDao.createRelation(request);

        createActivityEvent(request.fromNode(), request.toNode());
        return Response.ok().entity(relation).build();
    }

    @PUT
    @Path("/{repository}")
    @Operation(summary = "update relation metadata between nodes", description = "Updates relation metadata between two nodes of the given type.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = NodeRelationData.class))),
            @ApiResponse(responseCode = "400", description = RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response updateRelation(
            @Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue = "-home-")) @PathParam("repository") String repository,
            @Valid UpdateRelationRequest request
    ) {
        RepositoryDao repoDao = RepositoryDao.getRepository(repository);
        RelationDao relationDao = new RelationDao(repoDao);
        NodeRelationData relation = relationDao.updateRelation(request);

        createActivityEvent(request.fromNode(), request.toNode());
        return Response.ok().entity(relation).build();
    }

    @DELETE
    @Path("/{repository}/{source}/{type}/{target}")
    @Operation(summary = "delete a relation between nodes", description = "Delete a relation between two nodes of the given type.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200, content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "400", description = RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response deleteRelation(
            @Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue = "-home-")) @PathParam("repository") String repository,
            @Parameter(description = RestConstants.MESSAGE_NODE_ID, required = true) @PathParam("source") String source,
            @Parameter(description = "Relation Type", required = true) @PathParam("type") InputRelationType type,
            @Parameter(description = RestConstants.MESSAGE_NODE_ID, required = true) @PathParam("target") String target
    ) {
        RepositoryDao repoDao = RepositoryDao.getRepository(repository);
        RelationDao relationDao = new RelationDao(repoDao);
        relationDao.deleteRelation(source, target, type);

        createActivityEvent(source, target);
        return Response.status(Response.Status.OK).build();
    }

    private void createActivityEvent(String source, String target) {
        activityEventService.trackActivityOnNode(new org.alfresco.service.cmr.repository.NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, source), null, ActivityOnNodeEventType.EDIT_MATERIAL_RELATION, AuthenticationUtil.getFullyAuthenticatedUser());
        activityEventService.trackActivityOnNode(new org.alfresco.service.cmr.repository.NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, target), null, ActivityOnNodeEventType.EDIT_MATERIAL_RELATION, AuthenticationUtil.getFullyAuthenticatedUser());
    }

    @GET
    @Path("/{repository}/{node}")
    @Operation(summary = "get all relation of the node", description = "Returns all relations of the node.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200, content = @Content(array = @ArraySchema(schema = @Schema(implementation = NodeRelationData.class)))),
            @ApiResponse(responseCode = "400", description = RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response getRelations(
            @Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue = "-home-")) @PathParam("repository") String repository,
            @Parameter(description = RestConstants.MESSAGE_NODE_ID, required = true) @PathParam("node") String node
    ) {
        RepositoryDao repoDao = RepositoryDao.getRepository(repository);
        RelationDao relationDao = new RelationDao(repoDao);
        List<NodeRelationData> relations = relationDao.getRelations(node);
        return Response.ok().entity(relations).build();
    }


    @GET
    @Path("/{repository}/{node}/raw")
    @Operation(summary = "get all relation of the node without resolving node details", description = "Returns all relations of the node.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200, content = @Content(array = @ArraySchema(schema = @Schema(implementation = RelationData.class)))),
            @ApiResponse(responseCode = "400", description = RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response getRawRelations(
            @Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue = "-home-")) @PathParam("repository") String repository,
            @Parameter(description = RestConstants.MESSAGE_NODE_ID, required = true) @PathParam("node") String node
    ) {
        RepositoryDao repoDao = RepositoryDao.getRepository(repository);
        RelationDao relationDao = new RelationDao(repoDao);
        List<RelationData> relations = relationDao.getRawRelations(node);
        return Response.ok().entity(relations).build();
    }


    @GET
    @Path("/{repository}/{node}/trace")
    @Operation(summary = "traces relation of the node", description = "Recursifly returns all relations of the node and its successors.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200, content = @Content(array = @ArraySchema(schema = @Schema(implementation = NodeRelationData.class)))),
            @ApiResponse(responseCode = "400", description = RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response traceRelations(
            @Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue = "-home-")) @PathParam("repository") String repository,
            @Parameter(description = RestConstants.MESSAGE_NODE_ID, required = true) @PathParam("node") String node,
            @Parameter(description = "Max depth of the trace") @QueryParam("maxDepth") Integer maxDepth
    ) {
        RepositoryDao repoDao = RepositoryDao.getRepository(repository);
        RelationDao relationDao = new RelationDao(repoDao);
        List<NodeRelationData> relations = relationDao.traceRelations(node, maxDepth);
        return Response.ok().entity(relations).build();
    }


    @POST
    @Path("/{repository}/{source}/{type}/{target}/approve")
    @Operation(summary = "create a relation between nodes", description = "Creates a relation between two nodes of the given type.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = NodeRelationData.class))),
            @ApiResponse(responseCode = "400", description = RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response approveRelation(
            @Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue = "-home-")) @PathParam("repository") String repository,
            @Parameter(description = RestConstants.MESSAGE_NODE_ID, required = true) @PathParam("source") String source,
            @Parameter(description = "Relation Type", required = true) @PathParam("type") InputRelationType type,
            @Parameter(description = RestConstants.MESSAGE_NODE_ID, required = true) @PathParam("target") String target
    ) {
        RepositoryDao repoDao = RepositoryDao.getRepository(repository);
        RelationDao relationDao = new RelationDao(repoDao);
        NodeRelationData relationData = relationDao.approveRelation(source, target, type);
        return Response.ok().entity(relationData).build();
    }

//    @Path("/{repository}/{node}/raw")
//    @Operation(summary = "get all relation of the node", description = "Returns all relations of the node.")
//    @ApiResponses({
//            @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200, content = @Content(array = @ArraySchema(schema = @Schema(implementation = RelationData.class)))),
//            @ApiResponse(responseCode = "400", description = RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
//            @ApiResponse(responseCode = "401", description = RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
//            @ApiResponse(responseCode = "403", description = RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
//            @ApiResponse(responseCode = "404", description = RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
//            @ApiResponse(responseCode = "500", description = RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
//    })
//    public Response getRelationsRaw(
//            @Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue = "-home-")) @PathParam("repository") String repository,
//            @Parameter(description = RestConstants.MESSAGE_NODE_ID, required = true) @PathParam("node") String node
//    ) {
//        RepositoryDao repoDao = RepositoryDao.getRepository(repository);
//        RelationDao relationDao = new RelationDao(repoDao);
//        List<RelationData> relationData = relationDao.getRelationsRaw(node);
//        return Response.ok().entity(relationData).build();
//    }
}
