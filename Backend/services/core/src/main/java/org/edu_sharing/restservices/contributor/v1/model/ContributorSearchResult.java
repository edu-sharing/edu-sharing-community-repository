package org.edu_sharing.restservices.contributor.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import org.edu_sharing.restservices.shared.Pagination;

import java.util.List;

/**
 * Result of the contributor registry management list: the contributors of the requested page
 * plus pagination info (total match count, offset, page size).
 */
@Value
@Builder
public class ContributorSearchResult {
    @JsonProperty(required = true)
    List<ContributorData> contributors;
    @JsonProperty(required = true)
    Pagination pagination;
}
