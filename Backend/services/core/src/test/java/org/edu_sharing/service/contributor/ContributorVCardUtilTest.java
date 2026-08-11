package org.edu_sharing.service.contributor;

import org.edu_sharing.service.search.SearchService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContributorVCardUtilTest {

    @Test
    void organizationIdsAloneYieldOrganizationKind() {
        ContributorEntry source = ContributorEntry.builder()
                .org("Example Org")
                .ror("https://ror.org/123")
                .wikidata("Q42")
                .build();
        ContributorEntry parsed = ContributorVCardUtil.fromVCardString(ContributorVCardUtil.toVCardString(source));
        assertNotNull(parsed);
        assertEquals(SearchService.ContributorKind.ORGANIZATION, parsed.getKind());
    }

    @Test
    void personIdTakesPrecedenceOverOrganizationId() {
        // an ORCID (person id) present alongside a ROR (org id) must still classify as PERSON
        ContributorEntry source = ContributorEntry.builder()
                .surname("Doe")
                .orcid("0000-0001")
                .ror("https://ror.org/123")
                .build();
        ContributorEntry parsed = ContributorVCardUtil.fromVCardString(ContributorVCardUtil.toVCardString(source));
        assertNotNull(parsed);
        assertEquals(SearchService.ContributorKind.PERSON, parsed.getKind());
    }

    @Test
    void vcardWithoutAnyIdIsNotManageable() {
        // no persistent id -> not part of the registry
        ContributorEntry source = ContributorEntry.builder()
                .givenname("Jane")
                .surname("Doe")
                .org("Some Org")
                .build();
        assertNull(ContributorVCardUtil.fromVCardString(ContributorVCardUtil.toVCardString(source)));
    }

    @Test
    void emailAloneIsNotEnoughToBeManageable() {
        // email must not trigger implicit creation of a registry entry - only ORCID/GND/ROR/Wikidata do
        ContributorEntry source = ContributorEntry.builder()
                .givenname("Jane")
                .surname("Doe")
                .email("jane@example.org")
                .build();
        assertNull(ContributorVCardUtil.fromVCardString(ContributorVCardUtil.toVCardString(source)));
    }

    @Test
    void blankAndNullInputReturnsNull() {
        assertNull(ContributorVCardUtil.fromVCardString(null));
        assertNull(ContributorVCardUtil.fromVCardString("   "));
        assertNull(ContributorVCardUtil.fromVCardString("not a vcard"));
    }

    @Test
    void roundTripPreservesPersonFields() {
        ContributorEntry source = ContributorEntry.builder()
                .givenname("Jane")
                .surname("Doe")
                .title("Prof. Dr.")
                .org("Example University")
                .email("jane.doe@example.org")
                .url("https://example.org/jane")
                .orcid("0000-0002-1825-0097")
                .gnduri("https://d-nb.info/gnd/123")
                .build();

        ContributorEntry parsed = ContributorVCardUtil.fromVCardString(ContributorVCardUtil.toVCardString(source));

        assertNotNull(parsed);
        assertEquals(SearchService.ContributorKind.PERSON, parsed.getKind());
        assertEquals("Jane", parsed.getGivenname());
        assertEquals("Doe", parsed.getSurname());
        assertEquals("Prof. Dr.", parsed.getTitle());
        assertEquals("Example University", parsed.getOrg());
        assertEquals("jane.doe@example.org", parsed.getEmail());
        assertEquals("https://example.org/jane", parsed.getUrl());
        assertEquals("0000-0002-1825-0097", parsed.getOrcid());
        assertEquals("https://d-nb.info/gnd/123", parsed.getGnduri());
    }

    @Test
    void roundTripPreservesOrganizationIds() {
        ContributorEntry source = ContributorEntry.builder()
                .org("Example Org")
                .url("https://example.org")
                .ror("https://ror.org/02mhbdp94")
                .build();

        ContributorEntry parsed = ContributorVCardUtil.fromVCardString(ContributorVCardUtil.toVCardString(source));

        assertNotNull(parsed);
        assertEquals(SearchService.ContributorKind.ORGANIZATION, parsed.getKind());
        assertEquals("Example Org", parsed.getOrg());
        assertEquals("https://ror.org/02mhbdp94", parsed.getRor());
    }

    /**
     * Documents a KNOWN BUG: a Wikidata id does not survive the vcard round-trip.
     * The vcard parser (cardme {@code VCardEngine}) uppercases extended-type names, so the parsed map
     * carries the id under {@code X-WIKIDATA}, while {@link org.edu_sharing.repository.client.tools.CCConstants#VCARD_T_X_WIKIDATA}
     * is the mixed-case {@code "X-Wikidata"} - the lookup misses and the id is silently dropped.
     * The other ids (ORCID / GND / ROR) are unaffected because their constants are already upper-case.
     * When the underlying bug is fixed this assertion will start failing and should be flipped to
     * {@code assertEquals("Q42", parsed.getWikidata())}.
     */
    @Test
    void wikidataIdIsDroppedOnReadDueToExtendedNameCaseMismatch() {
        ContributorEntry source = ContributorEntry.builder()
                .org("Example Org")
                .wikidata("Q42")
                .build();

        String vcard = ContributorVCardUtil.toVCardString(source);
        // the id IS written to the vcard ...
        assertTrue(vcard.contains("Q42"), "wikidata should be written to the vcard: " + vcard);
        // ... but it is lost again on read (current, buggy behaviour)
        ContributorEntry parsed = ContributorVCardUtil.fromVCardString(vcard);
        assertNull(parsed, "with only a (dropped) wikidata id the vcard parses to no manageable contributor");
    }

    @Test
    void toVCardStringEmbedsPersistentId() {
        String vcard = ContributorVCardUtil.toVCardString(ContributorEntry.builder().orcid("0000-0003").build());
        assertNotNull(vcard);
        assertTrue(vcard.contains("0000-0003"), "vcard should embed the orcid: " + vcard);
    }

    @Test
    void parsedEntryRetainsOriginalVcardString() {
        String vcard = ContributorVCardUtil.toVCardString(ContributorEntry.builder().orcid("0000-0004").build());
        ContributorEntry parsed = ContributorVCardUtil.fromVCardString(vcard);
        assertNotNull(parsed);
        assertEquals(vcard, parsed.getVcard());
    }

    @Test
    void toEntriesSplitsCombinedVcardIntoPersonAndOrganization() {
        // a vcard with both a person id (ORCID) and an org id (ROR) describes a person and an affiliated org
        String vcard = ContributorVCardUtil.toVCardString(ContributorEntry.builder()
                .givenname("Jane").surname("Doe").title("Prof. Dr.")
                .org("Example University")
                .email("jane@example.org").url("https://example.org/jane")
                .orcid("0000-0002-1825-0097")
                .ror("https://ror.org/02mhbdp94")
                .build());

        List<ContributorEntry> entries = ContributorVCardUtil.toEntries(vcard);

        assertEquals(2, entries.size());
        ContributorEntry person = entries.stream()
                .filter(e -> e.getKind() == SearchService.ContributorKind.PERSON).findFirst().orElseThrow();
        ContributorEntry org = entries.stream()
                .filter(e -> e.getKind() == SearchService.ContributorKind.ORGANIZATION).findFirst().orElseThrow();

        // person keeps the personal fields and the person id, but not the org fields
        assertEquals("Jane", person.getGivenname());
        assertEquals("Doe", person.getSurname());
        assertEquals("Prof. Dr.", person.getTitle());
        assertEquals("0000-0002-1825-0097", person.getOrcid());
        assertNull(person.getOrg());
        assertNull(person.getRor());
        // organization keeps the org fields and the org id, but not the person fields
        assertEquals("Example University", org.getOrg());
        assertEquals("https://ror.org/02mhbdp94", org.getRor());
        assertNull(org.getGivenname());
        assertNull(org.getSurname());
        assertNull(org.getOrcid());
        // email and url are copied into both records
        assertEquals("jane@example.org", person.getEmail());
        assertEquals("jane@example.org", org.getEmail());
        assertEquals("https://example.org/jane", person.getUrl());
        assertEquals("https://example.org/jane", org.getUrl());
    }

    @Test
    void toEntriesReturnsSinglePersonWhenOnlyPersonIdPresent() {
        String vcard = ContributorVCardUtil.toVCardString(ContributorEntry.builder()
                .surname("Doe").orcid("0000-1").build());
        List<ContributorEntry> entries = ContributorVCardUtil.toEntries(vcard);
        assertEquals(1, entries.size());
        assertEquals(SearchService.ContributorKind.PERSON, entries.get(0).getKind());
    }

    @Test
    void toEntriesReturnsSingleOrganizationWhenOnlyOrgIdPresent() {
        String vcard = ContributorVCardUtil.toVCardString(ContributorEntry.builder()
                .org("Example Org").ror("https://ror.org/1").build());
        List<ContributorEntry> entries = ContributorVCardUtil.toEntries(vcard);
        assertEquals(1, entries.size());
        assertEquals(SearchService.ContributorKind.ORGANIZATION, entries.get(0).getKind());
    }

    @Test
    void toEntriesReturnsEmptyForEmailOnly() {
        String vcard = ContributorVCardUtil.toVCardString(ContributorEntry.builder()
                .givenname("Jane").email("jane@example.org").build());
        assertTrue(ContributorVCardUtil.toEntries(vcard).isEmpty());
    }

    @Test
    void toEntriesReturnsEmptyWhenNoPersistentId() {
        String vcard = ContributorVCardUtil.toVCardString(ContributorEntry.builder()
                .givenname("Jane").surname("Doe").org("Some Org").build());
        assertTrue(ContributorVCardUtil.toEntries(vcard).isEmpty());
        assertTrue(ContributorVCardUtil.toEntries(null).isEmpty());
    }
}
