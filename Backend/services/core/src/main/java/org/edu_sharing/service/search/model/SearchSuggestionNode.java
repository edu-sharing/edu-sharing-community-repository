package org.edu_sharing.service.search.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.edu_sharing.service.model.NodeRef;
import org.edu_sharing.service.suggestion.SuggestionStatus;
import org.edu_sharing.service.suggestion.SuggestionType;

import java.util.Date;
import java.util.List;

@AllArgsConstructor
@Data
public class SearchSuggestionNode {
    NodeRef nodeRef;
    List<SuggestionNode> suggestions;

    @AllArgsConstructor
    @Data
    public static class SuggestionNode {
        String id;
        SuggestionType type;
        SuggestionStatus status;
        String propertyId;
        String value;
        String version;
        String description;
        String createdBy;
        Date created;
    }
}
