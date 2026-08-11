package org.edu_sharing.service.contributor;

import org.edu_sharing.service.contributor.ibatis.ContributorMapper;
import org.edu_sharing.service.search.SearchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContributorServiceImplTest {

    @Mock
    private ContributorMapper contributorMapper;
    @Mock
    private ContributorPropagationService propagationService;
    @InjectMocks
    private ContributorServiceImpl underTest;

    @Test
    void searchTrimsWordAndDefaultsLimit() {
        underTest.search("  ", SearchService.ContributorKind.PERSON, 0);
        // blank word -> null, non-positive limit -> 50
        verify(contributorMapper).search(isNull(), eq(SearchService.ContributorKind.PERSON), eq(50));
    }

    @Test
    void searchKeepsGivenWordAndLimit() {
        underTest.search("doe", null, 10);
        verify(contributorMapper).search(eq("doe"), isNull(), eq(10));
    }

    @Test
    void getAllDefaultsLimitToHundred() {
        underTest.getAll(5, 0);
        verify(contributorMapper).getAll(eq(5L), eq(100));
    }

    @Test
    void getAllKeepsPositiveLimit() {
        underTest.getAll(5, 20);
        verify(contributorMapper).getAll(eq(5L), eq(20));
    }

    @Test
    void countDelegatesToMapper() {
        when(contributorMapper.count()).thenReturn(7L);
        assertEquals(7L, underTest.count());
    }

    @Test
    @SuppressWarnings("unchecked")
    void listManagedAppliesDefaultsAndClampsSkip() {
        underTest.listManaged("  ", null, null, null, true, -5, 0);

        // null sortBy -> NAME default; null hasIds -> empty column list; skip clamped to >= 0; limit default 50
        verify(contributorMapper).countManaged(isNull(), isNull(), eq(List.of()));
        verify(contributorMapper).listManaged(
                isNull(), isNull(), eq(List.of()),
                eq("surname ASC, org ASC, givenname ASC"), eq(0L), eq(50));
    }

    @Test
    @SuppressWarnings("unchecked")
    void listManagedMapsIdTypesToWhitelistedColumnsAndBuildsOrderBy() {
        ArgumentCaptor<List<String>> columns = ArgumentCaptor.forClass(List.class);

        underTest.listManaged("doe", SearchService.ContributorKind.ORGANIZATION,
                List.of(ContributorIdType.ORCID, ContributorIdType.EMAIL),
                ContributorSortProperty.CREATED, false, 0, 25);

        verify(contributorMapper).listManaged(
                eq("doe"), eq(SearchService.ContributorKind.ORGANIZATION), columns.capture(),
                eq("created DESC"), eq(0L), eq(25));
        assertEquals(List.of("orcid", "email"), columns.getValue());
    }

    @Test
    void listManagedReturnsEntriesWithTotal() {
        List<ContributorEntry> entries = List.of(ContributorEntry.builder().orcid("x").build());
        when(contributorMapper.countManaged(any(), any(), any())).thenReturn(42L);
        when(contributorMapper.listManaged(any(), any(), any(), any(), anyLong(), anyInt())).thenReturn(entries);

        ContributorPage page = underTest.listManaged(null, null, null, ContributorSortProperty.NAME, true, 0, 50);

        assertEquals(42L, page.total());
        assertSame(entries, page.entries());
    }

    @Test
    void createRejectsNullEntry() {
        assertThrows(IllegalArgumentException.class, () -> underTest.create(null));
    }

    @Test
    void createRejectsEntryWithoutAnyId() {
        ContributorEntry entry = ContributorEntry.builder().givenname("Jane").surname("Doe").build();
        assertThrows(IllegalArgumentException.class, () -> underTest.create(entry));
    }

    @Test
    void createRejectsEntryWithOnlyEmail() {
        // email alone must not qualify - only ORCID/GND/ROR/Wikidata make an entry manageable
        ContributorEntry entry = ContributorEntry.builder().givenname("Jane").surname("Doe").email("jane@example.org").build();
        assertThrows(IllegalArgumentException.class, () -> underTest.create(entry));
    }

    @Test
    void createDerivesOrganizationKindAndStampsTimestampsAndVcard() {
        ContributorEntry entry = ContributorEntry.builder()
                .org("Example Org").ror("https://ror.org/1").wikidata("Q1").build();

        ContributorEntry result = underTest.create(entry);

        // org-only ids -> ORGANIZATION
        assertEquals(SearchService.ContributorKind.ORGANIZATION, result.getKind());
        assertNull(result.getId(), "id must be cleared so the db generates it");
        assertNotNull(result.getCreated());
        assertNotNull(result.getLastUpdated());
        assertNotNull(result.getVcard());
        verify(contributorMapper).create(entry);
    }

    @Test
    void createDerivesPersonKindWhenPersonIdPresent() {
        ContributorEntry entry = ContributorEntry.builder().surname("Doe").orcid("0000-1").ror("https://ror.org/1").build();
        ContributorEntry result = underTest.create(entry);
        assertEquals(SearchService.ContributorKind.PERSON, result.getKind());
    }

    @Test
    void createKeepsExplicitKind() {
        ContributorEntry entry = ContributorEntry.builder()
                .kind(SearchService.ContributorKind.ORGANIZATION).orcid("0000-1").build();
        ContributorEntry result = underTest.create(entry);
        assertEquals(SearchService.ContributorKind.ORGANIZATION, result.getKind());
    }

    @Test
    void updateRejectsUnknownId() {
        when(contributorMapper.getById(99)).thenReturn(null);
        ContributorEntry entry = ContributorEntry.builder().orcid("0000-1").build();
        assertThrows(IllegalArgumentException.class, () -> underTest.update(99, entry, false));
    }

    @Test
    void updateKeepsCreationDateAndPropagatesWhenRequested() {
        Date created = new Date(1000L);
        ContributorEntry before = ContributorEntry.builder().id(1L).orcid("0000-old").created(created).build();
        when(contributorMapper.getById(1)).thenReturn(before);

        ContributorEntry entry = ContributorEntry.builder().surname("Doe").orcid("0000-new").build();
        ContributorEntry result = underTest.update(1, entry, true);

        assertEquals(1L, result.getId());
        assertEquals(created, result.getCreated(), "created is carried over from the stored entry");
        assertNotNull(result.getLastUpdated());
        verify(contributorMapper).update(entry);
        verify(propagationService).applyContributorChange(before, entry);
    }

    @Test
    void updateDoesNotPropagateWhenFlagFalse() {
        ContributorEntry before = ContributorEntry.builder().id(2L).orcid("0000-old").created(new Date()).build();
        when(contributorMapper.getById(2)).thenReturn(before);

        underTest.update(2, ContributorEntry.builder().orcid("0000-new").build(), false);

        verify(contributorMapper).update(any());
        verifyNoInteractions(propagationService);
    }

    @Test
    void deleteDelegatesToMapper() {
        underTest.delete(5);
        verify(contributorMapper).delete(5);
    }

    private static String vcard(String surname, String orcid) {
        return ContributorVCardUtil.toVCardString(ContributorEntry.builder().surname(surname).orcid(orcid).build());
    }

    @Test
    void registerVCardsIfAbsentSkipsVCardsWithoutPersistentId() {
        // vcard carries only a name, no X- id -> not manageable
        List<ContributorEntry> created = underTest.registerVCardsIfAbsent(List.of(vcard("Doe", null)), "editor");

        assertTrue(created.isEmpty());
        verify(contributorMapper, never()).findByAnyId(any(), any(), any(), any(), any());
        verify(contributorMapper, never()).create(any());
    }

    @Test
    void registerVCardsIfAbsentDeduplicatesSameIdWithinCall() {
        when(contributorMapper.findByAnyId(any(), any(), any(), any(), any())).thenReturn(List.of());
        // same orcid twice (different formatting) -> only one insert
        List<ContributorEntry> created = underTest.registerVCardsIfAbsent(
                List.of(vcard("Doe", "0000-1"), vcard("Doe-typo", "0000-1")), "editor");

        assertEquals(1, created.size());
        verify(contributorMapper, times(1)).create(any());
    }

    @Test
    void registerVCardsIfAbsentSkipsWhenAlreadyInRegistry() {
        when(contributorMapper.findByAnyId(any(), any(), any(), any(), any()))
                .thenReturn(List.of(ContributorEntry.builder().id(1L).orcid("0000-1").build()));

        List<ContributorEntry> created = underTest.registerVCardsIfAbsent(List.of(vcard("Doe", "0000-1")), "editor");

        assertTrue(created.isEmpty());
        verify(contributorMapper, never()).create(any());
    }

    @Test
    void registerVCardsIfAbsentCreatesWithCreatorAndTimestamps() {
        when(contributorMapper.findByAnyId(any(), any(), any(), any(), any())).thenReturn(List.of());
        ArgumentCaptor<ContributorEntry> captor = ArgumentCaptor.forClass(ContributorEntry.class);

        List<ContributorEntry> created = underTest.registerVCardsIfAbsent(List.of(vcard("Doe", "0000-1")), "editor");

        assertEquals(1, created.size());
        verify(contributorMapper).create(captor.capture());
        ContributorEntry entry = captor.getValue();
        assertEquals("0000-1", entry.getOrcid());
        assertEquals("editor", entry.getCreator());
        assertNotNull(entry.getCreated());
        assertNotNull(entry.getLastUpdated());
    }

    @Test
    void registerVCardsIfAbsentHandlesNullInput() {
        assertTrue(underTest.registerVCardsIfAbsent(null, "editor").isEmpty());
        verifyNoInteractions(contributorMapper);
    }

    /** a combined vcard (person id + org id) is registered as two independent entries */
    private static String combinedVcard() {
        return ContributorVCardUtil.toVCardString(ContributorEntry.builder()
                .givenname("Jane").surname("Doe").orcid("0000-1")
                .org("Example Org").ror("https://ror.org/1")
                .email("jane@example.org").build());
    }

    @Test
    void registerVCardsIfAbsentSplitsCombinedVcardIntoPersonAndOrganization() {
        when(contributorMapper.findByAnyId(any(), any(), any(), any(), any())).thenReturn(List.of());

        List<ContributorEntry> created = underTest.registerVCardsIfAbsent(List.of(combinedVcard()), "editor");

        assertEquals(2, created.size());
        assertTrue(created.stream().anyMatch(e -> e.getKind() == SearchService.ContributorKind.PERSON
                && "0000-1".equals(e.getOrcid())), "a person entry is created for the ORCID");
        assertTrue(created.stream().anyMatch(e -> e.getKind() == SearchService.ContributorKind.ORGANIZATION
                && "https://ror.org/1".equals(e.getRor())), "an organization entry is created for the ROR");
        verify(contributorMapper, times(2)).create(any());
    }

    @Test
    void registerVCardsIfAbsentExcludesEmailFromOrganizationLookup() {
        when(contributorMapper.findByAnyId(any(), any(), any(), any(), any())).thenReturn(List.of());

        underTest.registerVCardsIfAbsent(List.of(combinedVcard()), "editor");

        // the person is looked up including its email ...
        verify(contributorMapper).findByAnyId(eq("0000-1"), isNull(), isNull(), isNull(), eq("jane@example.org"));
        // ... but the organization lookup drops the email so the just-inserted person cannot mask it
        verify(contributorMapper).findByAnyId(isNull(), isNull(), eq("https://ror.org/1"), isNull(), isNull());
    }
}
