package org.edu_sharing.restservices.relation.v1.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import org.edu_sharing.restservices.shared.User;

import java.util.Date;

@Value
@Builder
public class NodeRelationDataEvaluation {
    @JsonProperty(required = true)
    boolean isApproved;
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    User approvedBy;
    Date approvedAt;
}
