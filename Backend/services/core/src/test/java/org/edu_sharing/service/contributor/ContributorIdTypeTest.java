package org.edu_sharing.service.contributor;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContributorIdTypeTest {

    @Test
    void getColumnMapsToWhitelistedColumns() {
        assertEquals("orcid", ContributorIdType.ORCID.getColumn());
        assertEquals("gnduri", ContributorIdType.GND.getColumn());
        assertEquals("ror", ContributorIdType.ROR.getColumn());
        assertEquals("wikidata", ContributorIdType.WIKIDATA.getColumn());
        assertEquals("email", ContributorIdType.EMAIL.getColumn());
    }

    @ParameterizedTest
    @EnumSource(ContributorIdType.class)
    void everyColumnIsNonBlank(ContributorIdType type) {
        // the column is interpolated into SQL, so every enum value must back a fixed, non-empty column name
        Assertions.assertNotNull(type.getColumn());
        Assertions.assertFalse(type.getColumn().isBlank());
    }
}
