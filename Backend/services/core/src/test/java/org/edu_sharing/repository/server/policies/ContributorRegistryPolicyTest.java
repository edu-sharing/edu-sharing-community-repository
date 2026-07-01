package org.edu_sharing.repository.server.policies;

import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.namespace.QName;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.contributor.ContributorService;
import org.edu_sharing.service.contributor.ContributorServiceFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContributorRegistryPolicyTest {

    @Mock
    private PolicyComponent policyComponent;
    @Mock
    private ContributorServiceFactory contributorServiceFactory;
    @Mock
    private ContributorService contributorService;
    @InjectMocks
    private ContributorRegistryPolicy underTest;

    private MockedStatic<AuthenticationUtil> authStatic;
    private MockedStatic<ContributorServiceFactory> factoryStatic;

    private static final QName AUTHOR = QName.createQName(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_AUTHOR);

    @BeforeEach
    void setUp() {
        authStatic = Mockito.mockStatic(AuthenticationUtil.class);
        factoryStatic = Mockito.mockStatic(ContributorServiceFactory.class);
    }

    @AfterEach
    void tearDown() {
        factoryStatic.close();
        authStatic.close();
    }

    /** wire the static factory -> local service, only needed for the tests that reach the delegation */
    private void stubService() {
        factoryStatic.when(ContributorServiceFactory::getInstance).thenReturn(contributorServiceFactory);
        when(contributorServiceFactory.getLocalService()).thenReturn(contributorService);
        authStatic.when(AuthenticationUtil::getFullyAuthenticatedUser).thenReturn("editor-user");
    }

    private static String vcard(String surname, String orcid) {
        return "BEGIN:VCARD\nN:" + surname + "\nX-ORCID:" + orcid + "\nEND:VCARD";
    }

    private static Serializable multi(String... vcards) {
        return new ArrayList<>(List.of(vcards));
    }

    @Test
    @SuppressWarnings("unchecked")
    void changedContributorPropertyDelegatesToServiceWithVCardsAndCreator() {
        stubService();
        String vcard = vcard("Doe", "0000-1");
        Map<QName, Serializable> after = Map.of(AUTHOR, multi(vcard));

        underTest.onUpdateProperties(null, Map.of(), after);

        ArgumentCaptor<Collection<String>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(contributorService).registerVCardsIfAbsent(captor.capture(), eq("editor-user"));
        assertTrue(captor.getValue().contains(vcard));
    }

    @Test
    void unchangedContributorPropertyIsIgnored() {
        Serializable value = multi(vcard("Doe", "0000-1"));
        // identical before/after -> no change -> no delegation
        underTest.onUpdateProperties(null, Map.of(AUTHOR, value), Map.of(AUTHOR, value));

        verify(contributorService, never()).registerVCardsIfAbsent(any(), any());
    }

    @Test
    void updateWithoutContributorPropertyDoesNothing() {
        Map<QName, Serializable> after = Map.of(QName.createQName(CCConstants.CM_NAME), "some-name.pdf");

        underTest.onUpdateProperties(null, Map.of(), after);

        verify(contributorService, never()).registerVCardsIfAbsent(any(), any());
    }
}
