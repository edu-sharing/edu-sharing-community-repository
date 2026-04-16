package org.edu_sharing.restservices.assignment.v1;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.edu_sharing.restservices.ApiService;
import org.edu_sharing.restservices.RestConstants;
import org.edu_sharing.restservices.assignment.v1.model.*;
import org.edu_sharing.restservices.search.v1.model.SearchParameters;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.edu_sharing.restservices.shared.SearchResult;
import org.edu_sharing.service.assignment.AssignmentDao;
import org.edu_sharing.service.assignment.AssignmentFileDao;
import org.edu_sharing.service.assignment.SubmissionDao;
import org.edu_sharing.service.assignment.SubmissionFileDao;
import org.edu_sharing.service.assignment.dao.AssignmentDaoFactory;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.model.SearchToken;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.edu_sharing.restservices.search.v1.SearchApi.getSearchToken;

@Slf4j
@Path("/assignment/v1")
@Tag(name = "Assignment v1", description = "Assignment API")
@ApiService(value = "ASSIGNMENT", major = 1, minor = 0)
@Consumes({"application/json"})
@Produces({"application/json"})
public class AssignmentApi {

    @Setter(onMethod_ = @Autowired)
    private AssignmentDaoFactory assignmentDaoFactory;


    /******************
     * Assignment API *
     ******************/
    @PUT
    @Path("/")
    @Operation(summary = "Create or update an assignment", description = "Create or update an assignment.")
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Assignment.class))),
                    @ApiResponse(responseCode = "400", description = RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = RestConstants.HTTP_409, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    public Response createOrUpdateAssignment(@Valid CreateAssignmentRequest request) {
        AssignmentDao assignment = Objects.isNull(request.id())
                ? assignmentDaoFactory.assignmentDaoByType(request.type())
                : assignmentDaoFactory.assignmentDaoByNodeId(request.id());
        assignment.createOrUpdate(request);
        return Response.ok().entity(assignment.getAssignment()).build();
    }

    @PUT
    @Path("/{assignmentId}/status")
    @Operation(summary = "Set assignment Status", description = "Set assignment Status.")
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Assignment.class))),
                    @ApiResponse(responseCode = "400", description = RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = RestConstants.HTTP_409, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    public Response createOrUpdateAssignment(@PathParam("assignmentId") String assignmentId,
                                             @QueryParam("status") Assignment.Status status) {
        AssignmentDao assignment = assignmentDaoFactory.assignmentDaoByNodeId(assignmentId);
        assignment.setStatus(status);
        return Response.ok().entity(assignment.getAssignment()).build();
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class AssignmentSearchResult extends SearchResult<Assignment> {
    }

    @POST
    @Path("/search")
    @Operation(summary = "Search assignments", description = "Searches for assignments.")
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = AssignmentSearchResult.class))),
                    @ApiResponse(responseCode = "400", description = RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = RestConstants.HTTP_409, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    public Response searchAssignments(
            @Parameter(description = "search parameters", required = false) SearchParameters parameters,
            @Parameter(description = RestConstants.MESSAGE_MAX_ITEMS, schema = @Schema(defaultValue = "25")) @QueryParam("maxItems") Integer maxItems,
            @Parameter(description = RestConstants.MESSAGE_SKIP_COUNT, schema = @Schema(defaultValue = "0")) @QueryParam("skipCount") Integer skipCount,
            @Parameter(description = RestConstants.MESSAGE_SORT_PROPERTIES) @QueryParam("sortProperties") List<String> sortProperties,
            @Parameter(description = RestConstants.MESSAGE_SORT_ASCENDING) @QueryParam("sortAscending") List<Boolean> sortAscending
            ) throws Throwable {
        SearchToken token = getSearchToken(SearchService.ContentType.ALL, maxItems, skipCount, sortProperties, sortAscending, parameters);
        SearchResult<AssignmentDao> assignmentDaoSearchResult = assignmentDaoFactory.searchAssignments(parameters.getCriteria(), token);
        AssignmentSearchResult result = assignmentDaoSearchResult.map(AssignmentDao::getAssignment, AssignmentSearchResult::new);
        return Response.ok().entity(result).build();
    }


    @GET
    @Path("/{assignmentId}")
    @Operation(summary = "get an assignment", description = "Retrieve an assignment based on its id.")
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Assignment.class))),
                    @ApiResponse(responseCode = "400", description = RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = RestConstants.HTTP_409, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    public Response getAssignment(@PathParam("assignmentId") String assignmentId) {
        AssignmentDao assignment = assignmentDaoFactory.assignmentDaoByNodeId(assignmentId);
        return Response.ok().entity(assignment.getAssignment()).build();
    }

    @DELETE
    @Path("/{assignmentId}")
    @Operation(summary = "Delete a assignment", description = "Delete a assignment an all its related documents and submissions")
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200),
                    @ApiResponse(responseCode = "400", description = RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = RestConstants.HTTP_409, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    public Response deleteAssignment(@PathParam("assignmentId") String assignmentId) {
        AssignmentDao assignment = assignmentDaoFactory.assignmentDaoByNodeId(assignmentId);
        assignment.delete();
        return Response.ok().build();
    }

    /***********************
     * Assignment File API *
     ***********************/

    @GET
    @Path("/{assignmentId}/files")
    @Operation(summary = "get assignment files", description = "get assignment files.")
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200, content = @Content(array = @ArraySchema(schema = @Schema(implementation = AssignmentFile.class)))),
                    @ApiResponse(responseCode = "400", description = RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = RestConstants.HTTP_409, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    public Response getAssignmentFiles(@PathParam("assignmentId") String assignmentId) {
        List<AssignmentFile> assignmentFiles = assignmentDaoFactory.assignmentDaoByNodeId(assignmentId)
                .getAssignmentFiles()
                .stream()
                .map(AssignmentFileDao::getAssignmentFile)
                .filter(Objects::nonNull)
                .toList();
        return Response.ok().entity(assignmentFiles).build();
    }

    /******************
     * Submission API *
     ******************/


    @GET
    @Path("/{assignmentId}/submissions")
    @Operation(summary = "get submissions", description = "get submissions (only available with observer/coordinator permissions) - also lists submissions not yet started")
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200, content = @Content(array = @ArraySchema(schema = @Schema(implementation = Submission.class)))),
                    @ApiResponse(responseCode = "400", description = RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = RestConstants.HTTP_409, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    public Response getSubmissions(@PathParam("assignmentId") String assignmentId) {
        AssignmentDao assignment = assignmentDaoFactory.assignmentDaoByNodeId(assignmentId);
        Collection<SubmissionDao> submissions = assignment.getSubmissions();
        return Response.ok().entity(submissions.stream().map(SubmissionDao::getSubmission).toList()).build();
    }

    @GET
    @Path("/{assignmentId}/submissions/{submissionId}")
    @Operation(summary = "get submissions", description = "get submissions (only available with observer/coordinator permissions) - also lists submissions not yet started")
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Submission.class))),
                    @ApiResponse(responseCode = "400", description = RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = RestConstants.HTTP_409, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    public Response getSubmission(@PathParam("assignmentId") String assignmentId,
                                  @Parameter(description = "id or -me- to get submission from current assignee")
                                  @PathParam("submissionId") String submissionId) {
        AssignmentDao assignment = assignmentDaoFactory.assignmentDaoByNodeId(assignmentId);
        SubmissionDao submission = assignment.getSubmission(submissionId);
        return Response.ok().entity(submission.getSubmission()).build();
    }

    @PUT
    @Path("/{assignmentId}/submissions/{submissionId}/validation")
    @Operation(summary = "edit submission", description = "edit submission (only as coordinator of the task)")
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Submission.class))),
                    @ApiResponse(responseCode = "400", description = RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = RestConstants.HTTP_409, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    public Response editSubmission(@PathParam("assignmentId") String assignmentId,
                                   @Parameter(description = "id or -me- to get submission from current assignee")
                                   @PathParam("submissionId") String submissionId,
                                   SubmissionValidationRequest request) {
        AssignmentDao assignment = assignmentDaoFactory.assignmentDaoByNodeId(assignmentId);
        SubmissionDao submission = assignment.getSubmission(submissionId);
        submission.updateValidationInfo(request);
        return Response.ok().entity(submission.getSubmission()).build();
    }

    @PUT
    @Path("/{assignmentId}/submissions/{submissionId}/submissionStatus")
    @Operation(summary = "edit submission status", description = "edit submission status")
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = Submission.class))),
                    @ApiResponse(responseCode = "400", description = RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = RestConstants.HTTP_409, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    public Response editSubmission(@PathParam("assignmentId") String assignmentId,
                                   @Parameter(description = "id or -me- to get submission from current assignee")
                                   @PathParam("submissionId") String submissionId,
                                   @QueryParam("status") Submission.Status status) {
        AssignmentDao assignment = assignmentDaoFactory.assignmentDaoByNodeId(assignmentId);
        SubmissionDao submission = assignment.getOrCreateSubmission(submissionId);
        submission.setStatus(status);
        return Response.ok().entity(submission.getSubmission()).build();
    }

    @DELETE
    @Path("/{assignmentId}/submissions/{submissionId}")
    @Operation(summary = "Delete a submission", description = "Delete a submission and all its related documents")
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200),
                    @ApiResponse(responseCode = "400", description = RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = RestConstants.HTTP_409, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    public Response deleteSubmission(@PathParam("assignmentId") String assignmentId,
                                     @Parameter(description = "id or -me- to get submission from current assignee")
                                     @PathParam("submissionId") String submissionId) {
        AssignmentDao assignment = assignmentDaoFactory.assignmentDaoByNodeId(assignmentId);
        SubmissionDao submission = assignment.getSubmission(submissionId);
        submission.delete();
        return Response.ok().build();
    }


    /***********************
     * Submission File API *
     ***********************/

    @GET
    @Path("/{assignmentId}/submissions/{submissionId}/files")
    @Operation(summary = "get submission files", description = "get submission files.")
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200, content = @Content(array = @ArraySchema(schema = @Schema(implementation = SubmissionFile.class)))),
                    @ApiResponse(responseCode = "400", description = RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = RestConstants.HTTP_409, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    public Response getSubmissionFiles(@PathParam("assignmentId") String assignmentId,
                                       @Parameter(description = "id or -me- to get submission from current assignee")
                                       @PathParam("submissionId") String submissionId) {
        AssignmentDao assignment = assignmentDaoFactory.assignmentDaoByNodeId(assignmentId);
        SubmissionDao submission = assignment.getSubmission(submissionId);
        List<SubmissionFileDao> submissionFiles = submission.getSubmissionFiles();
        return Response.ok().entity(submissionFiles.stream().map(SubmissionFileDao::getSubmissionFile).toList()).build();
    }

    /**
     * Only used for Swagger UI / OpenApi Specification.
     * To use this as a parameter, we need to register a MessageBodyReader for multipart/form-data.
     */
    @Schema(name = "SubmissionFileContentUpload", description = "Multipart upload for submission files")
    public static class SubmissionFileContentUpload {
        @Schema(description = "JSON-Metadaten")
        public SubmissionFileRequest metadata;

        @Schema(type = "string", format = "binary", description = "File content")
        public InputStream binary;
    }


    @POST
    @Path("/{assignmentId}/submissions/{submissionId}/files")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(
            summary = "Create a submission file",
            description = "Create a submission file",
            requestBody = @RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA,
                            schema = @Schema(implementation = SubmissionFileContentUpload.class),
                            encoding = {
                                    @Encoding(
                                            name = "metadata",
                                            contentType = "application/json"
                                    ),
                                    @Encoding(
                                            name = "binary",
                                            contentType = "application/octet-stream"
                                    )
                            }
                    )
            )
    )
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = SubmissionFile.class))),
                    @ApiResponse(responseCode = "400", description = RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = RestConstants.HTTP_409, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    public Response createSubmissionFile(@PathParam("assignmentId") String assignmentId,
                                         @Parameter(description = "id or -me- to get submission from current assignee")
                                         @PathParam("submissionId") String submissionId,
                                         @Schema(implementation = SubmissionFileRequest.class)
                                         @FormDataParam("metadata") FormDataBodyPart metadataPart,
                                         @FormDataParam("binary") InputStream fileInputStream,
                                         @FormDataParam("binary") FormDataContentDisposition fileMetaData) {

        metadataPart.setMediaType(MediaType.APPLICATION_JSON_TYPE);
        SubmissionFileRequest submissionFileRequest = metadataPart.getValueAs(SubmissionFileRequest.class);

        try (ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = validatorFactory.getValidator();
            Set<ConstraintViolation<SubmissionFileRequest>> violations = validator.validate(submissionFileRequest);
            if (!violations.isEmpty()) {
                throw new ConstraintViolationException(violations);
            }
        }

        log.debug("Received metadata: {}", submissionFileRequest);
        log.debug("Received file: {}", fileMetaData != null ? fileMetaData.getFileName() : null);

        AssignmentDao assignment = assignmentDaoFactory.assignmentDaoByNodeId(assignmentId);
        SubmissionDao submission = assignment.getOrCreateSubmission(submissionId);
        SubmissionFileDao submissionFile = submission.createSubmissionFile(submissionFileRequest, fileInputStream, fileMetaData);
        return Response.ok().entity(submissionFile.getSubmissionFile()).build();
    }

    /**
     * Only used for Swagger UI / OpenApi Specification.
     * To use this as a parameter, we need to register a MessageBodyReader for multipart/form-data.
     */
    @Schema(name = "SubmissionFileValidationUpload", description = "Multipart upload for submission file corrections and validation")
    public static class SubmissionFileValidationUpload {
        @Schema(description = "JSON-Metadaten")
        public SubmissionFileValidationRequest metadata;

        @Schema(type = "string", format = "binary", description = "File content")
        public InputStream binary;
    }

    @PUT
    @Path("/{assignmentId}/submissions/{submissionId}/files/{submissionFileId}/validation")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(
            summary = "Update correction file for submission file",
            description = "Update correction file for submission file",
            requestBody = @RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA,
                            schema = @Schema(implementation = SubmissionFileValidationUpload.class),
                            encoding = {
                                    @Encoding(
                                            name = "metadata",
                                            contentType = "application/json"
                                    ),
                                    @Encoding(
                                            name = "binary",
                                            contentType = "application/octet-stream"
                                    )
                            }
                    )
            )
    )
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = SubmissionFile.class))),
                    @ApiResponse(responseCode = "400", description = RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = RestConstants.HTTP_409, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    public Response updateSubmissionFileValidation(@PathParam("assignmentId") String assignmentId,
                                                   @Parameter(description = "id or -me- to get submission from current assignee")
                                                   @PathParam("submissionId") String submissionId,
                                                   @Parameter(description = "id of the submission file")
                                                   @PathParam("submissionFileId") String submissionFileId,
                                                   @Schema(implementation = SubmissionFileRequest.class)
                                                   @FormDataParam("metadata") FormDataBodyPart metadataPart,
                                                   @FormDataParam("binary") InputStream fileInputStream,
                                                   @FormDataParam("binary") FormDataContentDisposition fileMetaData) {

        metadataPart.setMediaType(MediaType.APPLICATION_JSON_TYPE);
        SubmissionFileValidationRequest submissionFileValidationRequest = metadataPart.getValueAs(SubmissionFileValidationRequest.class);

        try (ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = validatorFactory.getValidator();
            Set<ConstraintViolation<SubmissionFileValidationRequest>> violations = validator.validate(submissionFileValidationRequest);
            if (!violations.isEmpty()) {
                throw new ConstraintViolationException(violations);
            }
        }

        log.debug("Received metadata: {}", submissionFileValidationRequest);
        log.debug("Received file: {}", fileMetaData != null ? fileMetaData.getFileName() : null);

        AssignmentDao assignment = assignmentDaoFactory.assignmentDaoByNodeId(assignmentId);
        SubmissionDao submission = assignment.getSubmission(submissionId);
        SubmissionFileDao submissionFile = submission.getSubmissionFile(submissionFileId);
        if (fileInputStream != null) {
            submissionFile.updateCorrectionFile(fileInputStream);
        }

        if (submissionFileValidationRequest.validationStatus() != null) {
            submissionFile.setValidationStatus(submissionFileValidationRequest.validationStatus());
        }

        return Response.ok().entity(submissionFile.getSubmissionFile()).build();
    }

    @DELETE
    @Path("/{assignmentId}/submissions/{submissionId}/files/{submissionFileId}")
    @Operation(summary = "Delete a submission file", description = "Delete a submission file")
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200),
                    @ApiResponse(responseCode = "400", description = RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = RestConstants.HTTP_409, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    public Response deleteSubmissionFile(@PathParam("assignmentId") String assignmentId,
                                         @Parameter(description = "id or -me- to get submission from current assignee")
                                         @PathParam("submissionId") String submissionId,
                                         @Parameter(description = "id of the submission file")
                                         @PathParam("submissionFileId") String submissionFileId) {
        AssignmentDao assignment = assignmentDaoFactory.assignmentDaoByNodeId(assignmentId);
        SubmissionDao submission = assignment.getSubmission(submissionId);
        SubmissionFileDao submissionFile = submission.getSubmissionFile(submissionFileId);
        submissionFile.delete();
        return Response.ok().build();
    }

}
