package org.edu_sharing.restservices;

import org.edu_sharing.restservices.contributor.v1.model.ContributorData;
import org.edu_sharing.restservices.contributor.v1.model.ContributorSearchResult;
import org.edu_sharing.restservices.contributor.v1.model.CreateContributorRequest;
import org.edu_sharing.restservices.contributor.v1.model.UpdateContributorRequest;
import org.edu_sharing.service.contributor.ContributorEntry;
import org.edu_sharing.service.contributor.ContributorPage;
import org.edu_sharing.service.contributor.ContributorService;
import org.edu_sharing.service.contributor.ContributorServiceFactory;
import org.edu_sharing.service.search.SearchService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContributorDaoTest {

    @Mock
    private ContributorService contributorService;
    @Mock
    private ContributorServiceFactory contributorServiceFactory;
    @Mock
    private RepositoryDao repoDao;

    private MockedStatic<ContributorServiceFactory> contributorServiceFactoryStatic;
    private ContributorDao underTest;

    @BeforeEach
    void setUp() {
        contributorServiceFactoryStatic = Mockito.mockStatic(ContributorServiceFactory.class);
        contributorServiceFactoryStatic.when(ContributorServiceFactory::getInstance).thenReturn(contributorServiceFactory);
        when(repoDao.getId()).thenReturn("home");
        when(contributorServiceFactory.getService("home")).thenReturn(contributorService);
        underTest = new ContributorDao(repoDao);
    }

    @AfterEach
    void tearDown() {
        contributorServiceFactoryStatic.close();
    }

    private static ContributorEntry fullEntry() {
        return ContributorEntry.builder()
                .id(7L)
                .kind(SearchService.ContributorKind.PERSON)
                .title("Prof.")
                .givenname("Jane")
                .surname("Doe")
                .org("Example University")
                .email("jane@example.org")
                .url("https://example.org")
                .uid("uid-1")
                .orcid("0000-1")
                .gnduri("gnd-1")
                .ror("ror-1")
                .wikidata("Q1")
                .vcard("BEGIN:VCARD")
                .created(new Date(1000L))
                .lastUpdated(new Date(2000L))
                .build();
    }

    @Test
    void getByIdMapsAllFields() throws Exception {
        when(contributorService.getById(7)).thenReturn(fullEntry());

        ContributorData data = underTest.getById(7);

        assertEquals(7L, data.getId());
        assertEquals(SearchService.ContributorKind.PERSON, data.getKind());
        assertEquals("Prof.", data.getTitle());
        assertEquals("Jane", data.getGivenname());
        assertEquals("Doe", data.getSurname());
        assertEquals("Example University", data.getOrg());
        assertEquals("jane@example.org", data.getEmail());
        assertEquals("https://example.org", data.getUrl());
        assertEquals("uid-1", data.getUid());
        assertEquals("0000-1", data.getOrcid());
        assertEquals("gnd-1", data.getGnduri());
        assertEquals("ror-1", data.getRor());
        assertEquals("Q1", data.getWikidata());
        assertEquals("BEGIN:VCARD", data.getVcard());
        assertEquals(new Date(1000L), data.getCreated());
        assertEquals(new Date(2000L), data.getLastUpdated());
    }

    @Test
    void getByIdThrowsWhenServiceReturnsNull() {
        when(contributorService.getById(8)).thenReturn(null);
        assertThrows(DAOMissingException.class, () -> underTest.getById(8));
    }

    @Test
    void createUnpacksRequestIntoEntryAndMapsResult() throws Exception {
        CreateContributorRequest request = new CreateContributorRequest(
                SearchService.ContributorKind.PERSON, "Prof.", "Jane", "Doe", "Org",
                "jane@example.org", "https://example.org", "uid-1", "0000-1", "gnd-1", "ror-1", "Q1");
        when(contributorService.create(any())).thenReturn(fullEntry());

        ContributorData data = underTest.create(request);

        ArgumentCaptor<ContributorEntry> captor = ArgumentCaptor.forClass(ContributorEntry.class);
        verify(contributorService).create(captor.capture());
        ContributorEntry sent = captor.getValue();
        assertEquals("Jane", sent.getGivenname());
        assertEquals("0000-1", sent.getOrcid());
        assertEquals("Q1", sent.getWikidata());
        assertEquals(7L, data.getId());
    }

    @Test
    void createWrapsValidationErrorAsDaoValidationException() {
        CreateContributorRequest request = new CreateContributorRequest(
                null, null, null, null, null, null, null, null, null, null, null, null);
        when(contributorService.create(any())).thenThrow(new IllegalArgumentException("no id"));
        assertThrows(DAOValidationException.class, () -> underTest.create(request));
    }

    @Test
    void updatePassesIdEntryAndApplyToExistingFlag() throws Exception {
        UpdateContributorRequest request = new UpdateContributorRequest(
                SearchService.ContributorKind.PERSON, null, "Jane", "Doe", null,
                null, null, null, "0000-1", null, null, null, true);
        when(contributorService.update(eq(5L), any(), anyBoolean())).thenReturn(fullEntry());

        underTest.update(5, request);

        ArgumentCaptor<ContributorEntry> captor = ArgumentCaptor.forClass(ContributorEntry.class);
        verify(contributorService).update(eq(5L), captor.capture(), eq(true));
        assertEquals("0000-1", captor.getValue().getOrcid());
    }

    @Test
    void searchManagedBuildsPaginationFromPage() {
        ContributorPage page = new ContributorPage(List.of(fullEntry()), 42L);
        when(contributorService.listManaged(any(), any(), any(), any(), anyBoolean(), eq(10L), eq(50)))
                .thenReturn(page);

        ContributorSearchResult result = underTest.searchManaged(null, null, null, null, true, 10, 50);

        assertEquals(1, result.getContributors().size());
        assertEquals(42, result.getPagination().getTotal());
        assertEquals(10, result.getPagination().getFrom());
        assertEquals(1, result.getPagination().getCount());
    }

    @Test
    void searchMapsEntriesToData() {
        when(contributorService.search(eq("doe"), eq(SearchService.ContributorKind.PERSON), eq(5)))
                .thenReturn(List.of(fullEntry()));

        List<ContributorData> result = underTest.search("doe", SearchService.ContributorKind.PERSON, 5);

        assertEquals(1, result.size());
        assertTrue(result.get(0).getId() == 7L);
    }
}
