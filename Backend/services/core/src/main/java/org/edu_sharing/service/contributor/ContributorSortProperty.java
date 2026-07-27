package org.edu_sharing.service.contributor;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Sortable columns of the contributor registry management list.
 * Each value carries a fixed, whitelisted list of ORDER BY terms - never built from user input.
 */
public enum ContributorSortProperty {
    NAME("surname", "org", "givenname"),
    KIND("kind"),
    CREATED("created"),
    LAST_UPDATED("last_updated"),
    /** primary persistent identifier - deterministic order over the present ids */
    IDS("COALESCE(orcid, gnduri, ror, wikidata, email)");

    private final String[] terms;

    ContributorSortProperty(String... terms) {
        this.terms = terms;
    }

    /**
     * Whitelisted ORDER BY expression, applying the direction to every sort term
     * (so a DESC sort affects all columns, not just the last one).
     */
    public String toOrderBy(boolean ascending) {
        String direction = ascending ? " ASC" : " DESC";
        return Arrays.stream(terms).map(term -> term + direction).collect(Collectors.joining(", "));
    }
}
