package org.edu_sharing.restservices.suggestions.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.NotImplementedException;
import org.edu_sharing.restservices.ApiService;
import org.edu_sharing.restservices.DAOException;
import org.edu_sharing.restservices.RestConstants;
import org.edu_sharing.restservices.suggestions.v1.dto.CreateSuggestionRequestDTO;
import org.edu_sharing.restservices.suggestions.v1.dto.NodeSuggestionResponseDTO;
import org.edu_sharing.restservices.suggestions.v1.dto.SuggestionResponseDTO;
import org.edu_sharing.service.suggestion.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/suggestions/v1")
@Tag(name = "SUGGESTIONS v1")
@ApiService(value = "SUGGESTIONS", major = 1, minor = 0)
@Consumes({"application/json"})
@Produces({"application/json"})
@RequiredArgsConstructor
public class SuggestionsApi {

    private final SuggestionServiceFactory suggestionServiceFactory;

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
            CreateSuggestionRequestDTO[] suggestions) throws DAOException {
        try {
            SuggestionService suggestionService = suggestionServiceFactory.getServiceByAppId(repository);
            Suggestion suggestion = suggestionService.createSuggestion(node, providerId, type, suggestions);
            return Response.ok(map(suggestion)).build();
        } catch (Throwable t) {
            throw DAOException.mapping(t);
        }
    }

    @DELETE
    @Path("/{repository}/{node}")
    @Operation(summary = "Create suggestions")
    @ApiResponse(responseCode = "200", description = "Delete all suggestions of a given providerId and node.")
    public Response deleteSuggestions(
            @Parameter(description = RestConstants.MESSAGE_REPOSITORY_ID, required = true, schema = @Schema(defaultValue = "-home-")) @PathParam("repository") String repository,
            @Parameter(description = RestConstants.MESSAGE_NODE_ID, required = true) @PathParam("node") String node,
            @QueryParam("providerId") String providerId) throws DAOException {
        try {
            SuggestionService suggestionService = suggestionServiceFactory.getServiceByAppId(repository);
            suggestionService.deleteSuggestions(node, providerId);
            return Response.ok().build();
        } catch (Throwable t) {
            throw DAOException.mapping(t);
        }
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
            @QueryParam("status") SuggestionStatus status) throws DAOException {
        try {
            SuggestionService suggestionService = suggestionServiceFactory.getServiceByAppId(repository);
            List<Suggestion> suggestions = suggestionService.updateStatus(ids, status);
            return Response.ok(suggestions.stream().map(this::map).toArray(SuggestionResponseDTO[]::new)).build();
        } catch (Throwable t) {
            throw DAOException.mapping(t);
        }
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
            @Parameter(description = RestConstants.MESSAGE_NODE_ID, required = true) @PathParam("node") String node) throws DAOException {
        try {
            SuggestionService suggestionService = suggestionServiceFactory.getServiceByAppId(repository);
            Map<String, List<Suggestion>> nodeSuggestions = suggestionService.getSuggestionsByNodeId(node);
            return Response.ok(map(node, nodeSuggestions)).build();
        } catch (Throwable t) {
            throw DAOException.mapping(t);
        }
    }

    private NodeSuggestionResponseDTO map(String node, Map<String, List<Suggestion>> nodeSuggestions) {
        return new NodeSuggestionResponseDTO(
                node,
                map(nodeSuggestions)
        );
    }

    private Map<String, List<SuggestionResponseDTO>> map(Map<String, List<Suggestion>> nodeSuggestions) {
        return nodeSuggestions
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, y->y.getValue().stream().map(this::map).collect(Collectors.toList())));
    }

    private SuggestionResponseDTO map(Suggestion suggestion) {
        return new SuggestionResponseDTO(
                suggestion.getId(),
                suggestion.getPropertyId(),
                suggestion.getValue(),
                suggestion.getType(),
                suggestion.getStatus(),
                suggestion.getDescription(),
                suggestion.getCreated(),
                suggestion.getCreatedBy(),
                suggestion.getModified(),
                suggestion.getModifiedBy());
    }
}
