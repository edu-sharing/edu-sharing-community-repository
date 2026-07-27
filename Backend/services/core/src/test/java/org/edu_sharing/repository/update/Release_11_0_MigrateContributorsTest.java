package org.edu_sharing.repository.update;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.edu_sharing.service.contributor.ContributorEntry;
import org.edu_sharing.service.contributor.ContributorService;
import org.edu_sharing.service.contributor.ContributorServiceFactory;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Release_11_0_MigrateContributorsTest {

    @Mock
    private SearchServiceFactory searchServiceFactory;
    @Mock
    private SearchService localSearchService;
    @Mock
    private ContributorServiceFactory contributorServiceFactory;
    @Mock
    private ContributorService contributorService;

    private final Release_11_0_MigrateContributors underTest = new Release_11_0_MigrateContributors();

    private MockedStatic<SearchServiceFactory> searchServiceFactoryStatic;
    private MockedStatic<ContributorServiceFactory> contributorServiceFactoryStatic;
    private MockedStatic<AuthenticationUtil> authStatic;

    @BeforeEach
    void setUp() {
        searchServiceFactoryStatic = Mockito.mockStatic(SearchServiceFactory.class);
        searchServiceFactoryStatic.when(SearchServiceFactory::getInstance).thenReturn(searchServiceFactory);
        when(searchServiceFactory.getLocalService()).thenReturn(localSearchService);

        contributorServiceFactoryStatic = Mockito.mockStatic(ContributorServiceFactory.class);
        contributorServiceFactoryStatic.when(ContributorServiceFactory::getInstance).thenReturn(contributorServiceFactory);
        when(contributorServiceFactory.getLocalService()).thenReturn(contributorService);

        authStatic = Mockito.mockStatic(AuthenticationUtil.class);
        authStatic.when(AuthenticationUtil::getFullyAuthenticatedUser).thenReturn("migration-user");
    }

    @AfterEach
    void tearDown() {
        authStatic.close();
        contributorServiceFactoryStatic.close();
        searchServiceFactoryStatic.close();
    }

    @Test
    void delegatesTheIndexedVCardsAndMigratingUserToTheRegistryService() throws Exception {
        Set<String> vcards = Set.of("vcard-a", "vcard-b");
        when(localSearchService.getAllContributorVCards()).thenReturn(vcards);
        when(contributorService.registerVCardsIfAbsent(vcards, "migration-user")).thenReturn(List.of());

        underTest.execute();

        // the migration only enumerates the vcards and delegates registration (dedup/parse/insert live in the service)
        verify(contributorService).registerVCardsIfAbsent(eq(vcards), eq("migration-user"));
    }
}
