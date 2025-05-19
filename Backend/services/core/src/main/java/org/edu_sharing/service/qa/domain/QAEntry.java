package org.edu_sharing.service.qa.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QAEntry {
    String id;
    String nodeId;

    String question;
    String answer;

    String usedText;
    String educationalLevel;

    Date created;
    String createdBy;

    Date lastReviewed;
    String reviewedBy;
    boolean edited;
}
