package org.edu_sharing.service.suggestion;

import org.edu_sharing.restservices.suggestions.v1.dto.CreateSuggestionRequestDTO;

import java.util.List;
import java.util.Map;

public interface SuggestionService {
    Suggestion createSuggestion(String nodeId, String providerId, SuggestionType type, CreateSuggestionRequestDTO[] suggestions);

    void deleteSuggestions(String nodeId, String providerId);

    List<Suggestion> updateStatus(String nodeId, List<String> ids, SuggestionStatus status);

    Map<String, List<Suggestion>> getSuggestionsByNodeId(String nodeId);
}
