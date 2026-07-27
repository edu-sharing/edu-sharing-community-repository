package org.edu_sharing.service.contributor;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContributorSortPropertyTest {

    @Test
    void nameAscendingAppliesDirectionToEveryTerm() {
        assertEquals("surname ASC, org ASC, givenname ASC", ContributorSortProperty.NAME.toOrderBy(true));
    }

    @Test
    void nameDescendingAppliesDirectionToEveryTerm() {
        // a DESC sort must affect all columns, not just the last one
        assertEquals("surname DESC, org DESC, givenname DESC", ContributorSortProperty.NAME.toOrderBy(false));
    }

    @Test
    void idsKeepsCoalesceExpressionIntact() {
        assertEquals("COALESCE(orcid, gnduri, ror, wikidata, email) ASC", ContributorSortProperty.IDS.toOrderBy(true));
        assertEquals("COALESCE(orcid, gnduri, ror, wikidata, email) DESC", ContributorSortProperty.IDS.toOrderBy(false));
    }

    @ParameterizedTest
    @EnumSource(ContributorSortProperty.class)
    void orderByIsNonBlankAndCarriesDirection(ContributorSortProperty sort) {
        // the ORDER BY expression is built from whitelisted terms only - never from user input
        String asc = sort.toOrderBy(true);
        String desc = sort.toOrderBy(false);
        Assertions.assertFalse(asc.isBlank());
        assertTrue(asc.endsWith(" ASC"));
        assertTrue(desc.endsWith(" DESC"));
    }
}
