package org.edu_sharing.restservices.suggestions.v1.dto;

import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
public class NodeSuggestionResponseDTO {
    String nodeId;
    Map<String, List<SuggestionResponseDTO>> suggestions;
}
