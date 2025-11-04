package org.edu_sharing.service.assignment;

import org.edu_sharing.repository.server.tools.transaction.RetryingTransaction;
import org.edu_sharing.restservices.assignment.v1.model.Assignment;
import org.edu_sharing.restservices.assignment.v1.model.CreateAssignmentRequest;

import java.util.Date;
import java.util.List;

public interface AssignmentDao {
    void createOrUpdate(CreateAssignmentRequest request);

    void refresh();

    boolean exists();

    void delete();

    Assignment getAssignment();

    String getNodeId();


    String getCreator();

    Date getModifiedDate();

    Boolean getAllowAdditionalDocumentSubmissions();

    Assignment.Type getType();

    Assignment.Status getStatus();

    Date getEndDate();

    Date getStartDate();

    Date getCreateDate();

    String getSummary();

    String getTitle();

    List<AssignmentFileDao> getAssignmentFiles();
}
