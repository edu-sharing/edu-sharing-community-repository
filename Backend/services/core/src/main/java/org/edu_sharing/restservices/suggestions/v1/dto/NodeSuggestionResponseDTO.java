package org.edu_sharing.restservices.suggestions.v1.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NodeSuggestionResponseDTO {
    @JsonProperty(required = true)
    private String nodeId;
    @JsonProperty(required = true)
    private Map<String, List<SuggestionResponseDTO>> suggestions;
}
