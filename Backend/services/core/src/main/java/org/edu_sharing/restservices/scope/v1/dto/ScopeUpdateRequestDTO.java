package org.edu_sharing.restservices.scope.v1.dto;

import lombok.Data;
import org.edu_sharing.service.scope.ScopeField;

import java.util.List;

@Data
public class ScopeUpdateRequestDTO {
    private String mds;
    private List<ScopeField> fields;
}
