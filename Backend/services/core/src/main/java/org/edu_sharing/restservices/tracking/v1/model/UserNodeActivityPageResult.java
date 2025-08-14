package org.edu_sharing.restservices.tracking.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.edu_sharing.restservices.shared.Pagination;
import org.edu_sharing.service.tracking.user_tracking.UserNodeActivity;

import java.util.List;


public record UserNodeActivityPageResult(
        @JsonProperty(required = true)
        List<UserNodeActivity> activities,
        @JsonProperty(required = true)
        Pagination pagination) {
}

