package org.edu_sharing.service.suggestion;

import org.edu_sharing.restservices.suggestions.v1.dto.CreateSuggestionRequestDTO;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Limit;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface SuggestionService {

    /**
     * Retrieves a list of tracked suggestion data based on the specified date range and limit.
     * This method is restricted to administrators.
     *
     * @param from The start date of the range (inclusive). Must not be null.
     * @param to   The end date of the range (inclusive). Can be null to specify an open-ended range.
     * @param limit The limit for pagination or number of results. Can be null for no limit.
     * @return A list of {@code Suggestion} objects that match the specified criteria.
     */
    @PreAuthorize("T(org.edu_sharing.service.authority.AuthorityServiceHelper).isAdmin()")
    List<PropertySuggestion> getTrackedData(@NotNull Date from, Date to, Limit limit);

    /**
     * Retrieves a list of deleted tracked suggestion data based on the specified date range and limit.
     * This method is restricted to administrators.
     *
     * @param from The start date of the range (inclusive). Must not be null.
     * @param to   The end date of the range (inclusive). Can be null to specify an open-ended range.
     * @param limit The limit for pagination or number of results. Can be null for no limit.
     * @return A list of {@code Suggestion} objects that were deleted and match the specified criteria.
     */
    @PreAuthorize("T(org.edu_sharing.service.authority.AuthorityServiceHelper).isAdmin()")
    List<PropertySuggestion> getDeletedTrackedData(@NotNull Date from, Date to, Limit limit);

    List<PropertySuggestion> createSuggestion(String nodeId, SuggestionType type, String version, List<CreateSuggestionRequestDTO> suggestions);

    void deleteSuggestions(String suggestionId, List<String> versions);

    List<PropertySuggestion> updateStatus(String nodeId, List<String> ids, SuggestionStatus status);

    Map<String, List<PropertySuggestion>> getSuggestionsByNodeId(String nodeId, List<SuggestionStatus> status);
}
