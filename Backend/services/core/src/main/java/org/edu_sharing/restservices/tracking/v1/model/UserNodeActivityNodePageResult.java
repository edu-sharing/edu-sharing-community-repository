package org.edu_sharing.restservices.tracking.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.edu_sharing.restservices.shared.Pagination;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserNodeActivityNodePageResult {
    @JsonProperty(required = true)
    List<UserNodeActivityNode> activities;
    @JsonProperty(required = true)
    private Pagination pagination;
}

