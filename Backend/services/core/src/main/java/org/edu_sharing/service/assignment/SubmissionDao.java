package org.edu_sharing.service.assignment;

import org.edu_sharing.restservices.assignment.v1.model.EditSubmissionRequest;
import org.edu_sharing.restservices.assignment.v1.model.Submission;
import org.edu_sharing.restservices.assignment.v1.model.SubmissionFileRequest;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;

import java.io.InputStream;
import java.util.List;

public interface SubmissionDao extends BasicNodeDao {
    void refresh();

    Submission getSubmission();

    boolean isReturned();

    void updateValidationInfo(EditSubmissionRequest request);

    void setStatus(Submission.Status status);

    void delete();

    List<SubmissionFileDao> getSubmissionFiles();

    SubmissionFileDao getSubmissionFile(String submissionFileId);

    SubmissionFileDao createOrUpdateSubmissionFile(String submissionFileId, SubmissionFileRequest submissionFileRequest, InputStream fileInputStream, FormDataContentDisposition fileMetaData);

    void create();

    Submission.Status getStatus();

    Submission.Status getValidationStatus();

    String getFeedback();

    String getValidationNotes();
}
