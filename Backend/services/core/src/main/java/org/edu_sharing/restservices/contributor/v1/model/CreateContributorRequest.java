package org.edu_sharing.restservices.contributor.v1.model;

import org.edu_sharing.service.search.SearchService;
import org.springframework.validation.annotation.Validated;

/**
 * Request to create a contributor in the registry. At least one persistent id
 * (orcid, gnduri, ror, wikidata or email) must be provided - enforced by the service.
 */
@Validated
public record CreateContributorRequest(
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
        String wikidata
) {
}
