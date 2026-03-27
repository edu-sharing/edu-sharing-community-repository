package org.edu_sharing.service.suggestion;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.NotImplementedException;
import org.edu_sharing.restservices.suggestions.v1.dto.CreateSuggestionRequestDTO;

import java.util.List;
import java.util.Map;

@Slf4j
public class DefaultSuggestionService implements SuggestionService {

    @Override
    public List<PropertySuggestion> createSuggestion(String nodeId, SuggestionType type, String version, List<CreateSuggestionRequestDTO> suggestions) {
        throw new NotImplementedException();
    }

    @Override
    public void deleteSuggestions(String suggestionId, List<String> versions) {
        throw new NotImplementedException();
    }

    @Override
    public List<PropertySuggestion> updateStatus(String nodeId, List<String> ids, SuggestionStatus status) {
        throw new NotImplementedException();
    }

    @Override
    public Map<String, List<PropertySuggestion>> getSuggestionsByNodeId(String nodeId, List<SuggestionStatus> status) {
        throw new NotImplementedException();
    }

}
