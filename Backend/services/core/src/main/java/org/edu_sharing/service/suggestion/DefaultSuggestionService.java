package org.edu_sharing.service.suggestion;

import org.apache.commons.lang.NotImplementedException;
import org.edu_sharing.restservices.suggestions.v1.dto.CreateSuggestionRequestDTO;

import java.util.List;
import java.util.Map;

public class DefaultSuggestionService implements SuggestionService {

    @Override
    public Suggestion createSuggestion(String nodeId, String providerId, SuggestionType type, CreateSuggestionRequestDTO[] suggestions) {
        throw new NotImplementedException();
    }

    @Override
    public void deleteSuggestions(String nodeId, String providerId) {
        throw new NotImplementedException();
    }

    @Override
    public List<Suggestion> updateStatus(List<String> ids, SuggestionStatus status) {
        throw new NotImplementedException();
    }

    @Override
    public Map<String, List<Suggestion>> getSuggestionsByNodeId(String nodeId) {
        throw new NotImplementedException();
    }

}
