package org.edu_sharing.service.contributor;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.edu_sharing.alfresco.service.search.cmis.QueryBuilder;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.search.SearchService.ContributorKind;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContributorPropagationServiceTest {

    @Mock
    private QueryBuilder queryBuilder;
    @Mock
    private SearchService searchService;
    @Mock
    private NodeService nodeService;
    @InjectMocks
    private ContributorPropagationService underTest;

    private MockedStatic<AuthenticationUtil> authenticationUtil;

    private final NodeRef nodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, "node1");
    // an arbitrary, real contributor property qname the production code iterates over
    private final String prop = CCConstants.getLifecycleContributerPropsMap().values().iterator().next();
    private final QName propQName = QName.createQName(prop);

    @BeforeEach
    void setUp() {
        authenticationUtil = Mockito.mockStatic(AuthenticationUtil.class);
        // run the privileged work synchronously so we can assert on its effects
        authenticationUtil.when(() -> AuthenticationUtil.runAsSystem(any()))
                .thenAnswer(inv -> ((AuthenticationUtil.RunAsWork<?>) inv.getArgument(0)).doWork());
    }

    @AfterEach
    void tearDown() {
        authenticationUtil.close();
    }

    /** stub a single-page result containing exactly our nodeRef */
    private void stubSinglePageResult() {
        when(queryBuilder.build(any())).thenReturn("SELECT ...");
        ResultSet resultSet = Mockito.mock(ResultSet.class);
        ResultSetRow row = Mockito.mock(ResultSetRow.class);
        when(row.getNodeRef()).thenReturn(nodeRef);
        when(resultSet.iterator()).thenReturn(List.of(row).iterator());
        when(resultSet.length()).thenReturn(1); // < PAGE_SIZE -> loop ends after one page
        when(searchService.query(any(SearchParameters.class))).thenReturn(resultSet);
    }

    @Test
    @SuppressWarnings("unchecked")
    void fullFieldMatchRewritesTheStoredVcard() {
        ContributorEntry before = ContributorEntry.builder().givenname("Jane").surname("Doe").orcid("0000-1").build();
        ContributorEntry after = ContributorEntry.builder().givenname("Jane").surname("Doe-Smith").orcid("0000-1").build();
        String afterVcard = ContributorVCardUtil.toVCardString(after);

        stubSinglePageResult();
        when(nodeService.getProperty(nodeRef, propQName))
                .thenReturn(ContributorVCardUtil.toVCardString(before));

        underTest.applyContributorChange(before, after);

        ArgumentCaptor<Serializable> captor = ArgumentCaptor.forClass(Serializable.class);
        verify(nodeService).setProperty(eq(nodeRef), eq(propQName), captor.capture());
        List<String> written = (List<String>) captor.getValue();
        assertEquals(List.of(afterVcard), written);
    }

    @Test
    void homonymWithSameIdButDifferentFieldsIsLeftUntouched() {
        ContributorEntry before = ContributorEntry.builder().givenname("Jane").surname("Doe").orcid("0000-1").build();
        ContributorEntry after = ContributorEntry.builder().givenname("Jane").surname("Doe-Smith").orcid("0000-1").build();
        // stored carries the same orcid (so it survives the coarse prefilter) but a different name
        ContributorEntry stored = ContributorEntry.builder().givenname("John").surname("Smith").orcid("0000-1").build();

        stubSinglePageResult();
        when(nodeService.getProperty(nodeRef, propQName))
                .thenReturn(ContributorVCardUtil.toVCardString(stored));

        underTest.applyContributorChange(before, after);

        verify(nodeService, never()).setProperty(any(), any(), any());
    }

    @Test
    void contributorWithoutPersistentIdSkipsPropagation() {
        // no orcid/gnd/ror/wikidata/email -> no distinctive token -> nothing to query
        ContributorEntry before = ContributorEntry.builder().givenname("Jane").surname("Doe").build();
        ContributorEntry after = ContributorEntry.builder().givenname("Jane").surname("Doe-Smith").build();

        underTest.applyContributorChange(before, after);

        verify(searchService, never()).query(any(SearchParameters.class));
        verify(nodeService, never()).setProperty(any(), any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void onlyTheMatchingEntryOfAMultiValuePropertyIsReplaced() {
        ContributorEntry before = ContributorEntry.builder().givenname("Jane").surname("Doe").orcid("0000-1").build();
        ContributorEntry after = ContributorEntry.builder().givenname("Jane").surname("Doe-Smith").orcid("0000-1").build();
        ContributorEntry other = ContributorEntry.builder().givenname("Other").surname("Person").orcid("9999").build();

        String matchVcard = ContributorVCardUtil.toVCardString(before);
        String otherVcard = ContributorVCardUtil.toVCardString(other);
        String afterVcard = ContributorVCardUtil.toVCardString(after);

        stubSinglePageResult();
        ArrayList<String> stored = new ArrayList<>(List.of(matchVcard, otherVcard));
        when(nodeService.getProperty(nodeRef, propQName)).thenReturn(stored);

        underTest.applyContributorChange(before, after);

        ArgumentCaptor<Serializable> captor = ArgumentCaptor.forClass(Serializable.class);
        verify(nodeService).setProperty(eq(nodeRef), eq(propQName), captor.capture());
        List<String> written = (List<String>) captor.getValue();
        assertEquals(2, written.size());
        assertTrue(written.contains(afterVcard), "matching entry replaced by the after-vcard");
        assertTrue(written.contains(otherVcard), "unrelated entry kept unchanged");
        assertTrue(!written.contains(matchVcard), "old matching vcard removed");
    }

    /** a combined media vcard carrying both a person (ORCID) and an organization (ROR) component */
    private static String combinedVcard() {
        return ContributorVCardUtil.toVCardString(ContributorEntry.builder()
                .givenname("Jane").surname("Doe").orcid("0000-1")
                .org("Example Org").ror("https://ror.org/1").build());
    }

    @Test
    @SuppressWarnings("unchecked")
    void personEditRewritesOnlyThePersonComponentOfACombinedVcard() {
        ContributorEntry before = ContributorEntry.builder()
                .kind(ContributorKind.PERSON).givenname("Jane").surname("Doe").orcid("0000-1").build();
        ContributorEntry after = ContributorEntry.builder()
                .kind(ContributorKind.PERSON).givenname("Jane").surname("Doe-Smith").orcid("0000-1").build();

        stubSinglePageResult();
        when(nodeService.getProperty(nodeRef, propQName)).thenReturn(combinedVcard());

        underTest.applyContributorChange(before, after);

        ArgumentCaptor<Serializable> captor = ArgumentCaptor.forClass(Serializable.class);
        verify(nodeService).setProperty(eq(nodeRef), eq(propQName), captor.capture());
        ContributorEntry result = ContributorVCardUtil.fromVCardString(((List<String>) captor.getValue()).get(0));
        // person component updated ...
        assertEquals("Doe-Smith", result.getSurname());
        // ... organization component preserved
        assertEquals("Example Org", result.getOrg());
        assertEquals("https://ror.org/1", result.getRor());
    }

    @Test
    @SuppressWarnings("unchecked")
    void organizationEditRewritesOnlyTheOrganizationComponentOfACombinedVcard() {
        ContributorEntry before = ContributorEntry.builder()
                .kind(ContributorKind.ORGANIZATION).org("Example Org").ror("https://ror.org/1").build();
        ContributorEntry after = ContributorEntry.builder()
                .kind(ContributorKind.ORGANIZATION).org("Example Organization e.V.").ror("https://ror.org/1").build();

        stubSinglePageResult();
        when(nodeService.getProperty(nodeRef, propQName)).thenReturn(combinedVcard());

        underTest.applyContributorChange(before, after);

        ArgumentCaptor<Serializable> captor = ArgumentCaptor.forClass(Serializable.class);
        verify(nodeService).setProperty(eq(nodeRef), eq(propQName), captor.capture());
        ContributorEntry result = ContributorVCardUtil.fromVCardString(((List<String>) captor.getValue()).get(0));
        // organization component updated ...
        assertEquals("Example Organization e.V.", result.getOrg());
        // ... person component preserved
        assertEquals("Doe", result.getSurname());
        assertEquals("0000-1", result.getOrcid());
    }

    @Test
    @SuppressWarnings("unchecked")
    void organizationEditRewritesTheSharedEmailAndUrlOfACombinedVcard() {
        // email and url are shared by both components -> an organization edit rewrites them, too
        String combined = ContributorVCardUtil.toVCardString(ContributorEntry.builder()
                .givenname("Jane").surname("Doe").orcid("0000-1")
                .org("Example Org").ror("https://ror.org/1")
                .email("info@example.org").url("https://example.org").build());
        ContributorEntry before = ContributorEntry.builder()
                .kind(ContributorKind.ORGANIZATION).org("Example Org").ror("https://ror.org/1")
                .email("info@example.org").url("https://example.org").build();
        ContributorEntry after = ContributorEntry.builder()
                .kind(ContributorKind.ORGANIZATION).org("Example Org").ror("https://ror.org/1")
                .email("contact@example.org").url("https://example.org/new").build();

        stubSinglePageResult();
        when(nodeService.getProperty(nodeRef, propQName)).thenReturn(combined);

        underTest.applyContributorChange(before, after);

        ArgumentCaptor<Serializable> captor = ArgumentCaptor.forClass(Serializable.class);
        verify(nodeService).setProperty(eq(nodeRef), eq(propQName), captor.capture());
        ContributorEntry result = ContributorVCardUtil.fromVCardString(((List<String>) captor.getValue()).get(0));
        // shared fields updated ...
        assertEquals("contact@example.org", result.getEmail());
        assertEquals("https://example.org/new", result.getUrl());
        // ... person component preserved
        assertEquals("0000-1", result.getOrcid());
        assertEquals("Doe", result.getSurname());
    }
}
