package org.edu_sharing.service.assignment;

import org.alfresco.service.cmr.repository.NodeRef;
import org.edu_sharing.restservices.assignment.v1.model.Submission;
import org.edu_sharing.restservices.assignment.v1.model.SubmissionFile;
import org.edu_sharing.restservices.assignment.v1.model.SubmissionFileRequest;

import java.io.InputStream;
import java.util.Optional;

public interface SubmissionFileDao extends BasicNodeDao {
    SubmissionFile getSubmissionFile();

    void delete();

    void create(SubmissionFileRequest submissionFileRequest, InputStream fileInputStream);

    Submission.Status getValidationStatus();

    Optional<AssignmentFileDao> getReferToAssigmentFile();

    String getContentNodeId();

    void updateCorrectionFile(InputStream fileInputStream);

    void setValidationStatus(Submission.Status validationStatus);

    void refresh();

    String getCorrectionNodeId();

    NodeRef getAlfrescoContentNodeRef();

    NodeRef getAlfrescoCorrectionNodeRef();
}
