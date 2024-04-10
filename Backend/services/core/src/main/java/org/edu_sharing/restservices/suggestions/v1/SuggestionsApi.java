package org.edu_sharing.restservices.suggestions.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang.NotImplementedException;
import org.edu_sharing.restservices.ApiService;
import org.edu_sharing.restservices.suggestions.v1.dto.CreateSuggestionRequestDTO;
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
    @Operation(summary = "Create suggestions",
            responses = @ApiResponse(responseCode = "200",
                    description = "Store suggestions for the given nodeId.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SuggestionResponseDTO[].class))))
    public Response createSuggestions(String providerId, SuggestionType type, String nodeId, List<CreateSuggestionRequestDTO> suggestions) {
        throw new NotImplementedException();
    }


    @DELETE
    @Operation(summary = "Create suggestions",
            responses = @ApiResponse(responseCode = "200",
                    description = "Delete all suggestions of a given providerId and nodeId.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SuggestionResponseDTO[].class))))
    public Response createSuggestions(String providerId, String nodeId) {
        throw new NotImplementedException();
    }

    @PATCH
    @Operation(summary = "Update suggestion status",
            responses = @ApiResponse(responseCode = "200",
                    description = "Updates the status of all suggestions by the given suggestion ids",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SuggestionResponseDTO[].class))))
    public Response updateStatus(List<String> ids, SuggestionStatus status) {
        throw new NotImplementedException();
    }

    @GET
    @Operation(summary = "Retrieve stored suggestion for the given nodeId",
            responses = @ApiResponse(responseCode = "200",
                    description = "get all suggestions notifications",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = SuggestionResponseDTO[].class))))
    public Response getSuggestionsByNodeId(String nodeId) {
        throw new NotImplementedException();
    }
}
