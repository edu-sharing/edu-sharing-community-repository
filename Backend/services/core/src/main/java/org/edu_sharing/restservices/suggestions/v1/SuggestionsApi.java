package org.edu_sharing.restservices.suggestions.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang.NotImplementedException;
import org.edu_sharing.restservices.ApiService;
import org.edu_sharing.restservices.RestConstants;
import org.edu_sharing.restservices.suggestions.v1.dto.CreateSuggestionRequestDTO;
import org.edu_sharing.restservices.suggestions.v1.dto.NodeSuggestionResponseDTO;
import org.edu_sharing.restservices.suggestions.v1.dto.SuggestionResponseDTO;
import org.edu_sharing.service.suggestion.SuggestionStatus;
import org.edu_sharing.service.suggestion.SuggestionType;

import java.util.List;

@Path("/suggestions/v1")
@Tag(name = "SUGGESTIONS v1")
@ApiService(value = "SUGGESTIONS", major = 1, minor = 0)
@Consumes({"application/json"})
@Produces({"application/json"})
public class SuggestionsApi {

    @POST
    @Path("/{repository}/{node}")
    @Operation(summary = "Create suggestions")
    @ApiResponse(responseCode = "200",
            description = "Store suggestions for the given node.",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = SuggestionResponseDTO.class))))
    public Response createSuggestions(
            @Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue = "-home-")) @PathParam("repository") String repository,
            @Parameter(description = RestConstants.MESSAGE_NODE_ID, required = true) @PathParam("node") String node,
            @QueryParam("providerId") String providerId,
            @QueryParam("type") SuggestionType type,
            CreateSuggestionRequestDTO[] suggestions) {
        throw new NotImplementedException();
    }


    @DELETE
    @Path("/{repository}/{node}")
    @Operation(summary = "Create suggestions")
    @ApiResponse(responseCode = "200", description = "Delete all suggestions of a given providerId and node.")
    public Response deleteSuggestions(
            @Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue = "-home-")) @PathParam("repository") String repository,
            @Parameter(description = RestConstants.MESSAGE_NODE_ID, required = true) @PathParam("node") String node,
            @QueryParam("providerId") String providerId) {
        throw new NotImplementedException();
    }

    @PATCH
    @Path("/{repository}")
    @Operation(summary = "Update suggestion status")
    @ApiResponse(responseCode = "200",
            description = "Updates the status of all suggestions by the given suggestion ids",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = SuggestionResponseDTO.class))))
    public Response updateStatus(
            @Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue = "-home-")) @PathParam("repository") String repository,
            @QueryParam("id") List<String> ids,
            @QueryParam("status") SuggestionStatus status) {
        throw new NotImplementedException();
    }

    @GET
    @Path("/{repository}/{node}")
    @Operation(summary = "Retrieve stored suggestion for the given nodeId")
    @ApiResponse(
            responseCode = "200",
            description = "get all suggestions notifications",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = NodeSuggestionResponseDTO.class)))
    public Response getSuggestionsByNodeId(
            @Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue = "-home-")) @PathParam("repository") String repository,
            @Parameter(description = RestConstants.MESSAGE_NODE_ID, required = true) @PathParam("node") String node) {
        throw new NotImplementedException();
    }
}
