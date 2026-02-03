package org.edu_sharing.service.relations;

import java.util.Date;

public interface Evaluation {
   boolean isApproved();
   Date getApprovedAt();
   String getApprovedBy();
}
