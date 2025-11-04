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
import jakarta.validation.Valid;
import jakarta.ws.rs.*;

import java.io.InputStream;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.edu_sharing.restservices.ApiService;
import org.edu_sharing.restservices.RestConstants;
import org.edu_sharing.restservices.assignment.v1.model.*;
import org.edu_sharing.restservices.shared.ErrorResponse;
import org.edu_sharing.service.assignment.AssignmentDao;
import org.edu_sharing.service.assignment.AssignmentFileDao;
import org.edu_sharing.service.assignment.AssignmentDaoFactory;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Objects;

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
        AssignmentDao assignment = assignmentDaoFactory.getAssignment(request.id());
        assignment.createOrUpdate(request);
        return Response.ok().entity(assignment.getAssignment()).build();
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
        AssignmentDao assignment = assignmentDaoFactory.getAssignment(assignmentId);
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
        AssignmentDao assignment = assignmentDaoFactory.getAssignment(assignmentId);
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
        List<AssignmentFile> assignmentFiles = assignmentDaoFactory.getAssignment(assignmentId)
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
        List<Submission> submissions = null;
        return Response.ok().entity(submissions).build();
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
        List<Submission> submissions = null;
        return Response.ok().entity(submissions).build();
    }

    @PUT
    @Path("/{assignmentId}/submissions/{submissionId}")
    @Operation(summary = "edut submission", description = "edit submission (only as coordinator of the task)")
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
                                   EditSubmissionRequest request) {
        Submission submission = null;
        return Response.ok().entity(submission).build();
    }

    @PUT
    @Path("/{assignmentId}/submissions/{submissionId}/submissionStatus")
    @Operation(summary = "edut submission status", description = "edut submission status")
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
        Submission submission = null;
        return Response.ok().entity(submission).build();
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
        List<SubmissionFile> submissionFiles = null;
        return Response.ok().entity(submissionFiles).build();
    }

    /**
     * Only used for Swagger UI / OpenApi Specification.
     * To use this as a parameter, we need to register a MessageBodyReader for multipart/form-data.
     */
    @Schema(name = "SubmissionFileUpload", description = "Multipart upload for submission files")
    public static class SubmissionFileUpload {
        @Schema(description = "JSON-Metadaten")
        public AssignmentFileRequest metadata;

        @Schema(type = "string", format = "binary", description = "File content")
        public InputStream binary;
    }


    @PUT
    @Path("/{assignmentId}/submissions/{submissionId}/files")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(
            summary = "Create or edit submission file",
            description = "Create or edit submission file",
            requestBody = @RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA,
                            schema = @Schema(implementation = SubmissionFileUpload.class),
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
                    @ApiResponse(responseCode = "200", description = RestConstants.HTTP_200, content = @Content(schema = @Schema(implementation = AssignmentFile.class))),
                    @ApiResponse(responseCode = "400", description = RestConstants.HTTP_400, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = RestConstants.HTTP_401, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = RestConstants.HTTP_403, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = RestConstants.HTTP_404, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = RestConstants.HTTP_409, content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = RestConstants.HTTP_500, content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    public Response createOrUpdateSubmissionFile(@PathParam("assignmentId") String assignmentId,
                                                 @Parameter(description = "id or -me- to get submission from current assignee")
                                                 @PathParam("submissionId") String submissionId,
                                                 @Parameter(description = "id or null if a new submission file shall be created")
                                                 @QueryParam("submissionFileId") String submissionFileId,
                                                 @Schema(implementation = SubmissionFileRequest.class)
                                                 @FormDataParam("metadata") FormDataBodyPart metadataPart,
                                                 @FormDataParam("binary") InputStream fileInputStream,
                                                 @FormDataParam("binary") FormDataContentDisposition fileMetaData) {

        metadataPart.setMediaType(MediaType.APPLICATION_JSON_TYPE);
        SubmissionFileRequest submissionFileRequest = metadataPart.getValueAs(SubmissionFileRequest.class);
        log.debug("Received metadata: {}", submissionFileRequest);
        log.debug("Received file: {}", fileMetaData.getFileName());

        SubmissionFile submissionFile = null;
        return Response.ok().entity(submissionFile).build();
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
        return Response.ok().build();
    }

}
