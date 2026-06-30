package org.edu_sharing.repository.update;

import org.edu_sharing.service.contributor.ContributorEntry;
import org.edu_sharing.service.contributor.ContributorVCardUtil;
import org.edu_sharing.service.contributor.ibatis.ContributorMapper;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Release_11_0_MigrateContributorsTest {

    @Mock
    private ContributorMapper contributorMapper;
    @Mock
    private SearchServiceFactory searchServiceFactory;
    @Mock
    private SearchService localSearchService;
    @InjectMocks
    private Release_11_0_MigrateContributors underTest;

    private MockedStatic<SearchServiceFactory> searchServiceFactoryStatic;

    @BeforeEach
    void setUp() {
        searchServiceFactoryStatic = Mockito.mockStatic(SearchServiceFactory.class);
        searchServiceFactoryStatic.when(SearchServiceFactory::getInstance).thenReturn(searchServiceFactory);
        when(searchServiceFactory.getLocalService()).thenReturn(localSearchService);
    }

    @AfterEach
    void tearDown() {
        searchServiceFactoryStatic.close();
    }

    private static String vcard(String surname, String orcid) {
        return ContributorVCardUtil.toVCardString(ContributorEntry.builder().surname(surname).orcid(orcid).build());
    }

    @Test
    void duplicateIdsWithinTheRunAreInsertedOnlyOnce() throws Exception {
        // same orcid (-> same id key) but different vcard formatting/name; only the first must be persisted
        Set<String> vcards = new LinkedHashSet<>(List.of(vcard("Doe", "0000-1"), vcard("Doe-typo", "0000-1")));
        when(localSearchService.getAllContributorVCards()).thenReturn(vcards);
        when(contributorMapper.findByAnyId(any(), any(), any(), any(), any())).thenReturn(List.of());

        underTest.execute();

        verify(contributorMapper, times(1)).create(any());
    }

    @Test
    void contributorAlreadyInRegistryIsSkipped() throws Exception {
        when(localSearchService.getAllContributorVCards()).thenReturn(Set.of(vcard("Doe", "0000-1")));
        // db already contains an entry with one of the ids
        when(contributorMapper.findByAnyId(any(), any(), any(), any(), any()))
                .thenReturn(List.of(ContributorEntry.builder().id(1L).orcid("0000-1").build()));

        underTest.execute();

        verify(contributorMapper, never()).create(any());
    }

    @Test
    void unparseableVcardIsSkipped() throws Exception {
        when(localSearchService.getAllContributorVCards()).thenReturn(Set.of("this is not a vcard"));

        underTest.execute();

        // unparseable -> never even consulted for an existing db entry, never created
        verify(contributorMapper, never()).findByAnyId(any(), any(), any(), any(), any());
        verify(contributorMapper, never()).create(any());
    }
}
