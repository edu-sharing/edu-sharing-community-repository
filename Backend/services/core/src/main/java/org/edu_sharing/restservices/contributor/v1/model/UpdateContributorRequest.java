package org.edu_sharing.restservices.contributor.v1.model;

import org.edu_sharing.service.search.SearchService;
import org.springframework.validation.annotation.Validated;

/**
 * Request to update a contributor in the registry.
 * <p>
 * If {@code applyToExisting} is true the change is asynchronously propagated to all media nodes that
 * currently carry this contributor (matched against all field values before the change).
 */
@Validated
public record UpdateContributorRequest(
        SearchService.ContributorKind kind,
        String title,
        String givenname,
        String surname,
        String org,
        String email,
        String url,
        String uid,
        String orcid,
        String gnduri,
        String ror,
        String wikidata,
        boolean applyToExisting
) {
}
