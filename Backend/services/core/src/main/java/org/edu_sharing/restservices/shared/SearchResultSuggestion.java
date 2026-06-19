package org.edu_sharing.restservices.shared;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.edu_sharing.restservices.SuggestionDao;


@Data
@EqualsAndHashCode(callSuper = true)
public class SearchResultSuggestion extends SearchResult<SuggestionDao.NodeSuggestionEntry> {
}
