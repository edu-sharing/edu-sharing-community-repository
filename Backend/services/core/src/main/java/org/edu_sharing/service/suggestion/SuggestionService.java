package org.edu_sharing.service.suggestion;

import org.edu_sharing.restservices.suggestions.v1.dto.CreateSuggestionRequestDTO;

import java.util.List;
import java.util.Map;

public interface SuggestionService {

    List<PropertySuggestion> createSuggestion(String nodeId, SuggestionType type, String version, List<CreateSuggestionRequestDTO> suggestions);

    void deleteSuggestions(String suggestionId, List<String> versions);

    List<PropertySuggestion> updateStatus(String nodeId, List<String> ids, SuggestionStatus status);

    Map<String, List<PropertySuggestion>> getSuggestionsByNodeId(String nodeId, List<SuggestionStatus> status);
}
