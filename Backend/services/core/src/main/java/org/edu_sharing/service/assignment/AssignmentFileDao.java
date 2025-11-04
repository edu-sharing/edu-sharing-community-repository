package org.edu_sharing.service.assignment;

import org.edu_sharing.restservices.assignment.v1.model.AssignmentFile;
import org.edu_sharing.restservices.assignment.v1.model.AssignmentFileRequest;

public interface AssignmentFileDao {
    void refresh();

    boolean exists();

    AssignmentFile getAssignmentFile();

    void delete();

    String getReferNodeId();

    void update(AssignmentFileRequest assignmentFileRequest);

    String getNodeId();

    Boolean isDone();

    AssignmentFile.Role getDocumentRole();
}

