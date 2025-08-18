package org.edu_sharing.restservices.qa.v1;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.edu_sharing.restservices.*;
import org.edu_sharing.restservices.qa.v1.domain.CreateQAEntryDTO;
import org.edu_sharing.restservices.qa.v1.domain.QAEntryResponseDTO;
import org.edu_sharing.restservices.qa.v1.domain.UpdateQAEntryDTO;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.edu_sharing.restservices.shared.UserSimple;
import org.edu_sharing.service.qa.QAService;
import org.edu_sharing.service.qa.domain.QAEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.stream.Collectors;

@Path("/qa/v1")
@Tag(name = "QUESTION ANSWER v1", description = "Question answers storage endpoint")
@ApiService(value = "QUESTION ANSWER", major = 1)
@Consumes({"application/json"})
@Produces({"application/json"})
public class QuestionAnswerApi {

    @Autowired
    private QAService qaService;

    @POST
    @Path("/{nodeId}")
    @Operation(summary = "Create QA Entries of a specific sourceId and nodeId",
            responses = {
                    @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = QAEntry[].class))),
                    @ApiResponse(responseCode = "400", description = RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            })
    public List<QAEntry> createQAEntries(@Valid @PathParam("nodeId") String nodeId, List<CreateQAEntryDTO> qaEntries) {
        return qaService.createQAEntries(nodeId, qaEntries);
    }


    @PATCH
    @Path("/{nodeId}")
    @Operation(summary = "Update QA Entries of a specific sourceId and nodeId",
            responses = {
                    @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = QAEntry[].class))),
                    @ApiResponse(responseCode = "400", description = RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            })
    public List<QAEntry> updateQAEntries(@Valid @PathParam("nodeId") String nodeId, List<UpdateQAEntryDTO> qaEntries) {
        return qaService.updateQAEntries(nodeId, qaEntries);
    }



    @GET
    @Path("/{nodeId}")
    @Operation(summary = "Get all QA Entries of a specific nodeId or nodeId and creator",
            responses = {
                    @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = QAEntry[].class))),
                    @ApiResponse(responseCode = "400", description = RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            })
    public List<QAEntryResponseDTO> getQAEntries(@PathParam("nodeId") String nodeId, @QueryParam("creator") String creator) {
        Mapper mapper = new Mapper(RepositoryDao.getHomeRepository());
        return qaService.getAllQAEntriesOf(nodeId, creator).stream().map(mapper::map).collect(Collectors.toList());
    }

    @DELETE
    @Path("/{nodeId}/node")
    @Operation(summary = "Delete all QA entries by nodeId or nodeId and creator",
            responses = {
                    @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200),
                    @ApiResponse(responseCode = "400", description = RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            })
    public ResponseEntity<Void> deleteAllQANodes(@PathParam("nodeId") String nodeId, @QueryParam("creator") String creator) {
        qaService.delete(nodeId, creator);
        return ResponseEntity.ok().build();
    }

    @DELETE
    @Path("/")
    @Operation(summary = "Delete QA entry by id",
            responses = {
                    @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200),
                    @ApiResponse(responseCode = "400", description = RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            })
    public ResponseEntity<Void> deleteQANodes(@QueryParam("id") List<String> ids) {
        qaService.delete(ids);
        return ResponseEntity.ok().build();
    }


    @Slf4j
    @RequiredArgsConstructor
    private static class Mapper {
        private final RepositoryDao repositoryDao;

        private UserSimple getPerson(String user) {
            try {
                return PersonDao.getPerson(repositoryDao, user).asPersonSimple(false);
            } catch (DAOException daoException) {
                log.error(daoException.getMessage());
                return null;
            }
        }

        public QAEntryResponseDTO map(QAEntry entry) {

            UserSimple createBy = getPerson(entry.getCreatedBy());
            UserSimple reviewedBy = getPerson(entry.getReviewedBy());

            return new QAEntryResponseDTO(
                    entry.getId(),
                    entry.getNodeId(),
                    entry.getQuestion(),
                    entry.getAnswer(),
                    entry.getUsedText(),
                    entry.getEducationalLevel(),
                    entry.getCreated(),
                    createBy,
                    entry.getLastReviewed(),
                    reviewedBy,
                    entry.isEdited()
            );
        }
    }

}
