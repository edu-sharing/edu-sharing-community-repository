package org.edu_sharing.service.contributor;

/**
 * Persistent identifier types of a contributor. Used by the management list to filter for entries
 * that carry a given id type. Each value maps to a fixed, whitelisted db column - never built from user input.
 */
public enum ContributorIdType {
    ORCID("orcid"),
    GND("gnduri"),
    ROR("ror"),
    WIKIDATA("wikidata"),
    EMAIL("email");

    private final String column;

    ContributorIdType(String column) {
        this.column = column;
    }

    /** whitelisted db column backing this id type */
    public String getColumn() {
        return column;
    }
}
