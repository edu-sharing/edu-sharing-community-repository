package org.edu_sharing.restservices.contributor.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.edu_sharing.restservices.ApiService;
import org.edu_sharing.restservices.ContributorDao;
import org.edu_sharing.restservices.RepositoryDao;
import org.edu_sharing.restservices.RestConstants;
import org.edu_sharing.restservices.contributor.v1.model.ContributorData;
import org.edu_sharing.restservices.contributor.v1.model.ContributorSearchResult;
import org.edu_sharing.restservices.contributor.v1.model.CreateContributorRequest;
import org.edu_sharing.restservices.contributor.v1.model.UpdateContributorRequest;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.edu_sharing.service.contributor.ContributorIdType;
import org.edu_sharing.service.contributor.ContributorSortProperty;
import org.edu_sharing.service.search.SearchService;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

@Path("/contributor/v1")
@Tag(name = "CONTRIBUTOR v1")
@ApiService(value = "CONTRIBUTOR", major = 1)
@Consumes({"application/json"})
@Produces({"application/json"})
@Slf4j
public class ContributorApi {

    @GET
    @Path("/{repository}")
    @Operation(summary = "search managed contributors", description = "Search the contributor registry (autocomplete / management list).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200, content = @Content(array = @ArraySchema(schema = @Schema(implementation = ContributorData.class)))),
            @ApiResponse(responseCode = "400", description = RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response getContributors(
            @Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue = "-home-")) @PathParam("repository") String repository,
            @Parameter(description = "search word") @QueryParam("searchWord") String searchWord,
            @Parameter(description = "contributor kind") @QueryParam("kind") SearchService.ContributorKind kind,
            @Parameter(description = "max number of results") @QueryParam("limit") @DefaultValue("50") int limit,
            @Context HttpServletRequest req) {
        try {
            RepositoryDao repoDao = RepositoryDao.getRepository(repository);
            ContributorDao dao = new ContributorDao(repoDao);
            return Response.ok().entity(dao.search(searchWord, kind, limit)).build();
        } catch (Throwable t) {
            return ErrorResponse.createResponse(t);
        }
    }

    @GET
    @Path("/{repository}/list")
    @Operation(summary = "list managed contributors", description = "Filtered, sorted and paginated management list of the contributor registry, including the total match count. Requires the TOOLPERMISSION_MANAGE_CONTRIBUTORS toolpermission.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = ContributorSearchResult.class))),
            @ApiResponse(responseCode = "400", description = RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response listContributors(
            @Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue = "-home-")) @PathParam("repository") String repository,
            @Parameter(description = "search word") @QueryParam("searchWord") String searchWord,
            @Parameter(description = "contributor kind") @QueryParam("kind") SearchService.ContributorKind kind,
            @Parameter(description = "only contributors carrying at least one of these id types") @QueryParam("hasId") List<ContributorIdType> hasId,
            @Parameter(description = "sort column") @QueryParam("sortBy") @DefaultValue("NAME") ContributorSortProperty sortBy,
            @Parameter(description = "sort direction") @QueryParam("sortAscending") @DefaultValue("true") boolean sortAscending,
            @Parameter(description = "pagination offset") @QueryParam("skip") @DefaultValue("0") int skip,
            @Parameter(description = "max number of results") @QueryParam("limit") @DefaultValue("50") int limit,
            @Context HttpServletRequest req) {
        try {
            RepositoryDao repoDao = RepositoryDao.getRepository(repository);
            ContributorDao dao = new ContributorDao(repoDao);
            return Response.ok().entity(dao.searchManaged(searchWord, kind, hasId, sortBy, sortAscending, skip, limit)).build();
        } catch (Throwable t) {
            return ErrorResponse.createResponse(t);
        }
    }

    @GET
    @Path("/{repository}/{id}")
    @Operation(summary = "get a single managed contributor", description = "Returns a single contributor of the registry by its id.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = ContributorData.class))),
            @ApiResponse(responseCode = "400", description = RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response getContributor(
            @Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue = "-home-")) @PathParam("repository") String repository,
            @Parameter(description = "id of the contributor", required = true) @PathParam("id") long id,
            @Context HttpServletRequest req) {
        try {
            RepositoryDao repoDao = RepositoryDao.getRepository(repository);
            ContributorDao dao = new ContributorDao(repoDao);
            return Response.ok().entity(dao.getById(id)).build();
        } catch (Throwable t) {
            return ErrorResponse.createResponse(t);
        }
    }

    @POST
    @Path("/{repository}")
    @Operation(summary = "create a managed contributor", description = "Creates a contributor in the registry. Requires the TOOLPERMISSION_MANAGE_CONTRIBUTORS toolpermission.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = ContributorData.class))),
            @ApiResponse(responseCode = "400", description = RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response createContributor(
            @Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue = "-home-")) @PathParam("repository") String repository,
            @Valid CreateContributorRequest request,
            @Context HttpServletRequest req) {
        try {
            RepositoryDao repoDao = RepositoryDao.getRepository(repository);
            ContributorDao dao = new ContributorDao(repoDao);
            return Response.ok().entity(dao.create(request)).build();
        } catch (Throwable t) {
            return ErrorResponse.createResponse(t);
        }
    }

    @PUT
    @Path("/{repository}/{id}")
    @Operation(summary = "update a managed contributor", description = "Updates a contributor. With applyToExisting=true the change is propagated to all media carrying this contributor. Requires the TOOLPERMISSION_MANAGE_CONTRIBUTORS toolpermission.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = ContributorData.class))),
            @ApiResponse(responseCode = "400", description = RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response updateContributor(
            @Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue = "-home-")) @PathParam("repository") String repository,
            @Parameter(description = "id of the contributor", required = true) @PathParam("id") long id,
            @Valid UpdateContributorRequest request,
            @Context HttpServletRequest req) {
        try {
            RepositoryDao repoDao = RepositoryDao.getRepository(repository);
            ContributorDao dao = new ContributorDao(repoDao);
            return Response.ok().entity(dao.update(id, request)).build();
        } catch (Throwable t) {
            return ErrorResponse.createResponse(t);
        }
    }

    @DELETE
    @Path("/{repository}/{id}")
    @Operation(summary = "delete a managed contributor", description = "Removes a contributor from the registry. The media keep their embedded contributor untouched. Requires the TOOLPERMISSION_MANAGE_CONTRIBUTORS toolpermission.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200, content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "400", description = RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response deleteContributor(
            @Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue = "-home-")) @PathParam("repository") String repository,
            @Parameter(description = "id of the contributor", required = true) @PathParam("id") long id,
            @Context HttpServletRequest req) {
        try {
            RepositoryDao repoDao = RepositoryDao.getRepository(repository);
            ContributorDao dao = new ContributorDao(repoDao);
            dao.delete(id);
            return Response.ok().build();
        } catch (Throwable t) {
            return ErrorResponse.createResponse(t);
        }
    }
}
