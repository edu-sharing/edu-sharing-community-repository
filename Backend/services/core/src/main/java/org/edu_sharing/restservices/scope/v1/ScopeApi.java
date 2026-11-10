package org.edu_sharing.restservices.scope.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.edu_sharing.restservices.ApiService;
import org.edu_sharing.restservices.RepositoryDao;
import org.edu_sharing.restservices.RestConstants;
import org.edu_sharing.restservices.scope.v1.dto.ScopeAssignmentRequestDTO;
import org.edu_sharing.restservices.scope.v1.dto.ScopeCreateRequestDTO;
import org.edu_sharing.restservices.scope.v1.dto.ScopeSelectionRequestDTO;
import org.edu_sharing.restservices.scope.v1.dto.ScopeUpdateRequestDTO;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.edu_sharing.service.scope.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;

/**
 * Scopes: editorial / customer specific assignments of content (see scopes concept §9).
 * Feature flag and permissions are enforced by the services ({@link ScopesFeature}: 404 while
 * repository.scopes.enabled is false; membership, toolpermissions, Read on the original).
 */
@Path("/scope/v1")
@Tag(name = "SCOPE v1")
@ApiService(value = "SCOPE", major = 1, minor = 0)
@Consumes({"application/json"})
@Produces({"application/json"})
public class ScopeApi {

    @Autowired
    private ScopeServiceFactory scopeServiceFactory;
    @Autowired
    private ScopesSettings scopesSettings;
    // implemented by the packages C3 / D1, optional until then
    @Autowired
    private ObjectProvider<ScopeDefinitionService> scopeDefinitionService;
    @Autowired
    private ObjectProvider<ScopeContextResolver> scopeContextResolver;

    @GET
    @Path("/{repository}/scopes")
    @Operation(summary = "Scopes for the scope switcher", description = "Scopes of all scope groups of the current user, plus the temporary (read only) scope of the current domain.")
    @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200, content = @Content(array = @ArraySchema(schema = @Schema(implementation = ScopeInfo.class))))
    @ApiResponse(responseCode = "404", description = RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public Response getScopes(
            @Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue = "-home-")) @PathParam("repository") String repository) {
        return Response.ok(getContextResolver().getAvailableScopes()).build();
    }

    @GET
    @Path("/{repository}/scopes/current")
    @Operation(summary = "Active scope", description = "Active scope including field list and role. 204 if the user works globally.")
    @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = ActiveScope.class)))
    @ApiResponse(responseCode = "204", description = "no active scope")
    @ApiResponse(responseCode = "404", description = RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public Response getCurrentScope(
            @Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue = "-home-")) @PathParam("repository") String repository) {
        return getContextResolver().resolve()
                .map(scope -> Response.ok(scope).build())
                .orElseGet(() -> Response.noContent().build());
    }

    @PUT
    @Path("/{repository}/scopes/current")
    @Operation(summary = "Switch the active scope", description = "Only scopes of the user's own scope groups. scopeId = null switches to global.")
    @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200)
    @ApiResponse(responseCode = "403", description = RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public Response setCurrentScope(
            @Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue = "-home-")) @PathParam("repository") String repository,
            ScopeSelectionRequestDTO selection) {
        getContextResolver().setSelectedScope(selection == null ? null : selection.getScopeId());
        return Response.ok().build();
    }

    @GET
    @Path("/{repository}/admin/scopes")
    @Operation(summary = "All scope groups", description = "Admin only.")
    @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200, content = @Content(array = @ArraySchema(schema = @Schema(implementation = ScopeDefinition.class))))
    @ApiResponse(responseCode = "403", description = RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public Response getAllScopes(
            @Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue = "-home-")) @PathParam("repository") String repository) {
        return Response.ok(getDefinitionService().getAllScopes()).build();
    }

    @GET
    @Path("/{repository}/admin/scopeOnlyProperties")
    @Operation(summary = "Properties that only exist in scope entries", description = "Every other property of a field list replaces the original value inside the scope (ScopeField.replaces). Admin only.")
    @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200, content = @Content(array = @ArraySchema(schema = @Schema(implementation = String.class))))
    @ApiResponse(responseCode = "403", description = RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public Response getScopeOnlyProperties(
            @Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue = "-home-")) @PathParam("repository") String repository) {
        return Response.ok(getDefinitionService().getScopeOnlyProperties()).build();
    }

    @POST
    @Path("/{repository}/admin/scopes")
    @Operation(summary = "Create a scope group", description = "Creates the group like an organization (org admins, shared folder) and marks it as scope group. Admin only.")
    @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = ScopeDefinition.class)))
    @ApiResponse(responseCode = "400", description = RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public Response createScope(
            @Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue = "-home-")) @PathParam("repository") String repository,
            ScopeCreateRequestDTO request) {
        if (request == null || request.getName() == null || request.getName().isBlank()) {
            throw new BadRequestException("name must be set");
        }
        return Response.ok(getDefinitionService().createScope(request.getName(), request.getDisplayName(), request.getMds(), request.getFields(), request.getPresentation())).build();
    }

    @POST
    @Path("/{repository}/admin/scopes/{scopeId}/convert")
    @Operation(summary = "Convert an existing organization into a scope group",
            description = "One way: marks the organization as scope group, gives its members Collaborator on the shared folder and starts a bulk job creating the entries of the content already in the shared folder (uploadValues of the field list). Admin only. mds = null uses the metadataset of the shared folder.")
    @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = ScopeDefinition.class)))
    @ApiResponse(responseCode = "400", description = RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public Response convertToScope(
            @Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue = "-home-")) @PathParam("repository") String repository,
            @Parameter(description = "authority name of the organization", required = true) @PathParam("scopeId") String scopeId,
            ScopeUpdateRequestDTO request) {
        if (request == null) {
            throw new BadRequestException("body must be set");
        }
        return Response.ok(getDefinitionService().convertToScope(scopeId, request.getMds(), request.getFields(), request.getPresentation())).build();
    }

    @PUT
    @Path("/{repository}/admin/scopes/{scopeId}")
    @Operation(summary = "Change mds, field list and presentation settings of a scope group", description = "Admin only. presentation = null removes the settings.")
    @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = ScopeDefinition.class)))
    @ApiResponse(responseCode = "400", description = RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public Response updateScope(
            @Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue = "-home-")) @PathParam("repository") String repository,
            @Parameter(description = "authority name of the scope group", required = true) @PathParam("scopeId") String scopeId,
            ScopeUpdateRequestDTO request) {
        if (request == null) {
            throw new BadRequestException("body must be set");
        }
        return Response.ok(getDefinitionService().updateScope(scopeId, request.getMds(), request.getFields(), request.getPresentation())).build();
    }

    @PUT
    @Path("/{repository}/scopes/{scopeId}/baseQuery")
    @Operation(summary = "Set the base query of a scope", description = "Requires the org admin role of the scope group. Increases the version.")
    @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = ScopeBaseQuery.class)))
    @ApiResponse(responseCode = "400", description = RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public Response setBaseQuery(
            @Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue = "-home-")) @PathParam("repository") String repository,
            @Parameter(description = "authority name of the scope group", required = true) @PathParam("scopeId") String scopeId,
            ScopeBaseQuery query) {
        return Response.ok(getDefinitionService().setBaseQuery(scopeId, query)).build();
    }

    @GET
    @Path("/{repository}/scopes/{scopeId}/nodes/{node}")
    @Operation(summary = "Get the entry of a node in a scope")
    @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = ScopeAssignment.class)))
    @ApiResponse(responseCode = "403", description = RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public Response getAssignment(
            @Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue = "-home-")) @PathParam("repository") String repository,
            @Parameter(description = "authority name of the scope group", required = true) @PathParam("scopeId") String scopeId,
            @Parameter(description = RestConstants.MESSAGE_NODE_ID, required = true) @PathParam("node") String node) {
        ScopeAssignment assignment = getScopeService(repository).getAssignment(scopeId, node);
        if (assignment == null) {
            throw new NotFoundException("no entry for node " + node + " in scope " + scopeId);
        }
        return Response.ok(assignment).build();
    }

    @PUT
    @Path("/{repository}/scopes/{scopeId}/nodes/{node}")
    @Operation(summary = "Set properties of the entry of a node in a scope", description = "Creates the entry if missing. An empty value list removes the property. Requires membership, TOOLPERMISSION_SCOPE_WRITE and Read on the original.")
    @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = ScopeAssignment.class)))
    @ApiResponse(responseCode = "400", description = RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public Response setAssignment(
            @Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue = "-home-")) @PathParam("repository") String repository,
            @Parameter(description = "authority name of the scope group", required = true) @PathParam("scopeId") String scopeId,
            @Parameter(description = RestConstants.MESSAGE_NODE_ID, required = true) @PathParam("node") String node,
            ScopeAssignmentRequestDTO request) {
        if (request == null || request.getProperties() == null) {
            throw new BadRequestException("properties must be set");
        }
        return Response.ok(getScopeService(repository).setProperties(scopeId, node, request.getProperties(), request.getReason())).build();
    }

    @DELETE
    @Path("/{repository}/scopes/{scopeId}/nodes/{node}")
    @Operation(summary = "Remove the entry of a node from a scope", description = "The history keeps a deletion record.")
    @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200)
    @ApiResponse(responseCode = "403", description = RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public Response deleteAssignment(
            @Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue = "-home-")) @PathParam("repository") String repository,
            @Parameter(description = "authority name of the scope group", required = true) @PathParam("scopeId") String scopeId,
            @Parameter(description = RestConstants.MESSAGE_NODE_ID, required = true) @PathParam("node") String node,
            @Parameter(description = "optional reason, stored in the history") @QueryParam("reason") String reason) {
        getScopeService(repository).deleteAssignment(scopeId, node, reason);
        return Response.ok().build();
    }

    @GET
    @Path("/{repository}/scopes/{scopeId}/nodes/{node}/history")
    @Operation(summary = "History of the entry of a node in a scope", description = "Newest first. Members of the scope group only.")
    @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200, content = @Content(array = @ArraySchema(schema = @Schema(implementation = ScopeHistoryEntry.class))))
    @ApiResponse(responseCode = "403", description = RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public Response getHistory(
            @Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue = "-home-")) @PathParam("repository") String repository,
            @Parameter(description = "authority name of the scope group", required = true) @PathParam("scopeId") String scopeId,
            @Parameter(description = RestConstants.MESSAGE_NODE_ID, required = true) @PathParam("node") String node) {
        return Response.ok(getScopeService(repository).getHistory(scopeId, node)).build();
    }

    @POST
    @Path("/{repository}/scopes/{scopeId}/bulk")
    @Operation(summary = "Start a bulk change", description = "Sets properties for (or removes the entries of) a list of nodes or all hits of a search. Runs asynchronously, returns the initial state of the job (jobId).")
    @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = ScopeBulkStatus.class)))
    @ApiResponse(responseCode = "400", description = RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public Response startBulk(
            @Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue = "-home-")) @PathParam("repository") String repository,
            @Parameter(description = "authority name of the scope group", required = true) @PathParam("scopeId") String scopeId,
            ScopeBulkRequest request) {
        if (request == null || (request.getNodeIds() == null) == (request.getSearch() == null)) {
            throw new BadRequestException("exactly one of nodeIds or search must be set");
        }
        ScopeService scopeService = getScopeService(repository);
        String jobId = scopeService.startBulk(scopeId, request);
        ScopeBulkStatus status = scopeService.getBulkStatus(scopeId, jobId);
        return Response.ok(status != null ? status : ScopeBulkStatus.builder().jobId(jobId).scopeId(scopeId).state(ScopeBulkStatus.State.QUEUED).build()).build();
    }

    @GET
    @Path("/{repository}/scopes/{scopeId}/bulk/{jobId}")
    @Operation(summary = "State of a bulk job")
    @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = ScopeBulkStatus.class)))
    @ApiResponse(responseCode = "403", description = RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public Response getBulkStatus(
            @Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue = "-home-")) @PathParam("repository") String repository,
            @Parameter(description = "authority name of the scope group", required = true) @PathParam("scopeId") String scopeId,
            @Parameter(description = "id of the bulk job", required = true) @PathParam("jobId") String jobId) {
        ScopeBulkStatus status = getScopeService(repository).getBulkStatus(scopeId, jobId);
        if (status == null) {
            throw new NotFoundException("unknown bulk job " + jobId);
        }
        return Response.ok(status).build();
    }

    @GET
    @Path("/{repository}/scopes/{scopeId}/suggest/{property}")
    @Operation(summary = "Autocomplete for values of a scope property", description = "Only values of this scope. Members of the scope group only.")
    @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200, content = @Content(array = @ArraySchema(schema = @Schema(implementation = String.class))))
    @ApiResponse(responseCode = "403", description = RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public Response suggest(
            @Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue = "-home-")) @PathParam("repository") String repository,
            @Parameter(description = "authority name of the scope group", required = true) @PathParam("scopeId") String scopeId,
            @Parameter(description = "local property name", required = true) @PathParam("property") String property,
            @Parameter(description = "prefix of the value") @QueryParam("value") String value,
            @Parameter(description = "maximum number of values", schema = @Schema(defaultValue = "10")) @QueryParam("maxItems") @DefaultValue("10") int maxItems) {
        // terms/prefix aggregation on scopes.properties.<property>, implemented by package F6
        throw new ServerErrorException("scope suggestions are not implemented yet", Response.Status.NOT_IMPLEMENTED);
    }

    @GET
    @Path("/{repository}/changes")
    @Operation(summary = "Change feed for the scope tracker", description = "Entries ordered by (at, seq). Same cursor contract as the share info oplog. Admin / system user only.")
    @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200, content = @Content(array = @ArraySchema(schema = @Schema(implementation = ScopeChange.class))))
    @ApiResponse(responseCode = "403", description = RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public Response getChanges(
            @Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue = "-home-")) @PathParam("repository") String repository,
            @Parameter(description = "Date to filter changes from (exclusive)") @QueryParam("after") Date after,
            @Parameter(description = "Without 'after', a standalone seq cursor. Together with 'after', a tiebreaker for changes sharing the exact same 'after' timestamp.") @QueryParam("afterSeq") Long afterSeq,
            @Parameter(description = "Date to filter changes until (inclusive)") @QueryParam("until") Date until,
            @Parameter(description = "maximum items per page") @QueryParam("maxItems") Integer maxItems) {
        int pageSize = scopesSettings.getChangeFeed().getPageSize();
        int limit = maxItems == null ? pageSize : Math.min(maxItems, pageSize);
        return Response.ok(getScopeService(repository).getChanges(after, afterSeq, until, limit)).build();
    }

    @GET
    @Path("/{repository}/index/{node}")
    @Operation(summary = "Index fragment of a node for the scope tracker", description = "Child objects get the fragment of their parent. Admin / system user only.")
    @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = ScopeIndexData.class)))
    @ApiResponse(responseCode = "403", description = RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public Response getIndexData(
            @Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue = "-home-")) @PathParam("repository") String repository,
            @Parameter(description = RestConstants.MESSAGE_NODE_ID, required = true) @PathParam("node") String node) {
        return Response.ok(getScopeService(repository).getIndexData(node)).build();
    }

    @POST
    @Path("/{repository}/index")
    @Operation(summary = "Index fragments of several nodes for the scope tracker", description = "Keyed by node id. Admin / system user only.")
    @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200, content = @Content(array = @ArraySchema(schema = @Schema(implementation = ScopeIndexData.class))))
    @ApiResponse(responseCode = "403", description = RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public Response getIndexDataBulk(
            @Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue = "-home-")) @PathParam("repository") String repository,
            List<String> nodeIds) {
        return Response.ok(List.copyOf(getScopeService(repository).getIndexData(nodeIds).values())).build();
    }

    private ScopeService getScopeService(String repository) {
        return scopeServiceFactory.getService(RepositoryDao.getRepository(repository).getId());
    }

    private ScopeContextResolver getContextResolver() {
        ScopeContextResolver resolver = scopeContextResolver.getIfAvailable();
        if (resolver == null) {
            throw new ServerErrorException("scope resolution is not implemented yet", Response.Status.NOT_IMPLEMENTED);
        }
        return resolver;
    }

    private ScopeDefinitionService getDefinitionService() {
        ScopeDefinitionService service = scopeDefinitionService.getIfAvailable();
        if (service == null) {
            throw new ServerErrorException("scope definitions are not implemented yet", Response.Status.NOT_IMPLEMENTED);
        }
        return service;
    }
}
