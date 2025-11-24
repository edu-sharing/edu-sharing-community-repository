package org.edu_sharing.service.assignment;

import org.edu_sharing.repository.server.tools.security.RunAsSystem;
import org.edu_sharing.repository.server.tools.transaction.RetryingTransaction;
import org.edu_sharing.restservices.assignment.v1.model.Submission;
import org.edu_sharing.restservices.assignment.v1.model.SubmissionFile;
import org.edu_sharing.restservices.assignment.v1.model.SubmissionFileRequest;

import java.io.InputStream;
import java.util.Optional;

public interface SubmissionFileDao extends BasicNodeDao {
    SubmissionFile getSubmissionFile();

    void delete();

    @RunAsSystem
    @RetryingTransaction
    void create(SubmissionFileRequest submissionFileRequest, InputStream fileInputStream);

    Submission.Status getValidationStatus();

    Optional<AssignmentFileDao> getReferToAssigmentFile();

    String getContentNodeId();

    @RunAsSystem
    @RetryingTransaction
    void update(SubmissionFileRequest request, InputStream fileInputStream);

    void setValidationStatus(Submission.Status validationStatus);

    void refresh();
}
