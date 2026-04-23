package org.edu_sharing.service.assignment.dao;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.edu_sharing.restservices.assignment.v1.model.Submission;
import org.edu_sharing.restservices.assignment.v1.model.SubmissionFileRequest;
import org.edu_sharing.restservices.assignment.v1.model.SubmissionValidationRequest;
import org.edu_sharing.restservices.shared.NodeRef;
import org.edu_sharing.restservices.shared.UserSimple;
import org.edu_sharing.service.assignment.SubmissionDao;
import org.edu_sharing.service.assignment.SubmissionFileDao;
import org.edu_sharing.service.authority.AuthorityService;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.InputStream;
import java.util.Date;
import java.util.List;

@RequiredArgsConstructor
public class EmptySubmissionAssignmentDao implements SubmissionDao {
    private final String creator;

    @Setter(onMethod_ = @Autowired)
    private AuthorityService authorityService;

    @Override
    public void refresh() {
    }

    @Override
    public Submission getSubmission() {
        return new Submission(
                null,
                UserSimple.create(authorityService.getUser(creator), creator),
                getValidationNotes(),
                getFeedback(),
                getStatus(),
                getValidationStatus(),
                getSubmissionDate(),
                getReturnDate(),
                getUserNotes()
        );
    }

    @Override
    public Date getReturnDate() {
        return null;
    }

    @Override
    public Date getSubmissionDate() {
        return null;
    }

    @Override
    public boolean isReturned() {
        return false;
    }

    @Override
    public void updateValidationInfo(SubmissionValidationRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setStatus(Submission.Status status) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean exists() {
        return false;
    }

    @Override
    public void delete() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getNodeId() {
        return null;
    }

    @Override
    public String getCreator() {
        return creator;
    }

    @Override
    public Date getModifiedDate() {
        return null;
    }

    @Override
    public Date getCreateDate() {
        return null;
    }

    @NotNull
    @Override
    public NodeRef getNodeRef() {
        return new NodeRef();
    }

    @Override
    public org.alfresco.service.cmr.repository.NodeRef getAlfrescoNodeRef() {
        return null;
    }

    @Override
    public List<SubmissionFileDao> getSubmissionFiles() {
        return List.of();
    }

    @Override
    public SubmissionFileDao getSubmissionFile(String submissionFileId) {
        return null;
    }

    @Override
    public SubmissionFileDao createSubmissionFile(SubmissionFileRequest submissionFileRequest, InputStream fileInputStream, FormDataContentDisposition fileMetaData) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void create() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Submission.Status getStatus() {
        return Submission.Status.NOT_STARTED;
    }

    @Override
    public Submission.Status getValidationStatus() {
        return Submission.Status.NOT_STARTED;
    }

    @Override
    public String getFeedback() {
        return "";
    }

    @Override
    public String getValidationNotes() {
        return "";
    }

    @Override
    public String getUserNotes() {
        return "";
    }

    @Override
    public void setUserNotes(String userNotes) {
        throw new UnsupportedOperationException();
    }
}
