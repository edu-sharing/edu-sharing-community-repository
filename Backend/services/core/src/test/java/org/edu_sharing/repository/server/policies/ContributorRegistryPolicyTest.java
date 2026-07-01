package org.edu_sharing.repository.server.policies;

import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.namespace.QName;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.contributor.ContributorEntry;
import org.edu_sharing.service.contributor.ContributorVCardUtil;
import org.edu_sharing.service.contributor.ibatis.ContributorMapper;
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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContributorRegistryPolicyTest {

    @Mock
    private PolicyComponent policyComponent;
    @Mock
    private ContributorMapper contributorMapper;
    @InjectMocks
    private ContributorRegistryPolicy underTest;

    private MockedStatic<AuthenticationUtil> authStatic;

    private static final QName AUTHOR = QName.createQName(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_AUTHOR);

    @BeforeEach
    void setUp() {
        // opened for every test; only stubbed in the tests that actually reach the create path
        authStatic = Mockito.mockStatic(AuthenticationUtil.class);
    }

    @AfterEach
    void tearDown() {
        authStatic.close();
    }

    private static String vcard(String surname, String orcid) {
        return ContributorVCardUtil.toVCardString(ContributorEntry.builder().surname(surname).orcid(orcid).build());
    }

    private static Serializable multi(String... vcards) {
        return new ArrayList<>(List.of(vcards));
    }

    @Test
    void changedContributorWithValidIdIsRegisteredWithCreator() {
        authStatic.when(AuthenticationUtil::getFullyAuthenticatedUser).thenReturn("editor-user");
        when(contributorMapper.findByAnyId(any(), any(), any(), any(), any())).thenReturn(List.of());
        Map<QName, Serializable> after = Map.of(AUTHOR, multi(vcard("Doe", "0000-1")));

        underTest.onUpdateProperties(null, Map.of(), after);

        ArgumentCaptor<ContributorEntry> captor = ArgumentCaptor.forClass(ContributorEntry.class);
        verify(contributorMapper).create(captor.capture());
        ContributorEntry created = captor.getValue();
        assertEquals("0000-1", created.getOrcid());
        assertEquals("editor-user", created.getCreator());
        assertNotNull(created.getCreated());
        assertNotNull(created.getLastUpdated());
    }

    @Test
    void contributorWithoutPersistentIdIsNotRegistered() {
        // vcard carries only a name, no X- id -> fromVCardString returns null
        Map<QName, Serializable> after = Map.of(AUTHOR, multi(vcard("Doe", null)));

        underTest.onUpdateProperties(null, Map.of(), after);

        verify(contributorMapper, never()).findByAnyId(any(), any(), any(), any(), any());
        verify(contributorMapper, never()).create(any());
    }

    @Test
    void contributorAlreadyInRegistryIsNotDuplicated() {
        when(contributorMapper.findByAnyId(any(), any(), any(), any(), any()))
                .thenReturn(List.of(ContributorEntry.builder().id(1L).orcid("0000-1").build()));
        Map<QName, Serializable> after = Map.of(AUTHOR, multi(vcard("Doe", "0000-1")));

        underTest.onUpdateProperties(null, Map.of(), after);

        verify(contributorMapper, never()).create(any());
    }

    @Test
    void sameIdInMultipleValuesIsRegisteredOnce() {
        authStatic.when(AuthenticationUtil::getFullyAuthenticatedUser).thenReturn("editor-user");
        when(contributorMapper.findByAnyId(any(), any(), any(), any(), any())).thenReturn(List.of());
        // same orcid twice (e.g. duplicated) -> only one insert
        Map<QName, Serializable> after = Map.of(AUTHOR, multi(vcard("Doe", "0000-1"), vcard("Doe-typo", "0000-1")));

        underTest.onUpdateProperties(null, Map.of(), after);

        verify(contributorMapper, times(1)).create(any());
    }

    @Test
    void unchangedContributorPropertyIsIgnored() {
        Serializable value = multi(vcard("Doe", "0000-1"));
        // identical before/after -> no change -> nothing happens
        underTest.onUpdateProperties(null, Map.of(AUTHOR, value), Map.of(AUTHOR, value));

        verifyNoInteractions(contributorMapper);
    }

    @Test
    void updateWithoutContributorPropertyDoesNothing() {
        Map<QName, Serializable> after = Map.of(QName.createQName(CCConstants.CM_NAME), "some-name.pdf");

        underTest.onUpdateProperties(null, Map.of(), after);

        verifyNoInteractions(contributorMapper);
    }
}
