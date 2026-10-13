package org.edu_sharing.service.scope;

import org.edu_sharing.repository.client.tools.CCConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ScopeNodeViewTest {

    static final String OWN = "GROUP_ORG_wlo";
    static final String OTHER = "GROUP_ORG_kunde_a";
    static final String TITLE = CCConstants.getValidGlobalName("cclom:title");

    static final ScopeDefinition DEFINITION = ScopeDefinition.builder().scopeId(OWN).displayName("WLO").fields(List.of(
            ScopeField.builder().id("ccm:editorial_inventory").visibility(ScopeFieldVisibility.INTERNAL).build(),
            ScopeField.builder().id("ccm:editorial_recommendation").visibility(ScopeFieldVisibility.PUBLIC).build(),
            ScopeField.builder().id("cclom:title").visibility(ScopeFieldVisibility.PUBLIC).replaces(true).build(),
            ScopeField.builder().id("ccm:curriculum").visibility(ScopeFieldVisibility.INTERNAL).replaces(true).build())).build();

    private ScopeNodeView underTest;
    private Map<String, Object> nodeProps;

    @BeforeEach
    void setUp() {
        ScopeDefinitionService definitionService = mock(ScopeDefinitionService.class);
        when(definitionService.getDefinition(anyString())).thenReturn(Optional.empty());
        when(definitionService.getDefinition(OTHER)).thenReturn(Optional.of(ScopeDefinition.builder().scopeId(OTHER).displayName("Kunde A").build()));
        underTest = new ScopeNodeView(new ScopesSettings(), mock(ScopeContextResolver.class), definitionService, mock(ScopeServiceFactory.class));
        nodeProps = new HashMap<>(Map.of(TITLE, "Originaltitel", CCConstants.NODETYPE, CCConstants.CCM_TYPE_IO));
    }

    private static Optional<ActiveScope> scope(ScopeRole role) {
        return Optional.of(ActiveScope.builder().definition(DEFINITION).role(role).build());
    }

    private static ScopeIndexData data(boolean ownPublished, boolean otherPublished) {
        return ScopeIndexData.builder().nodeId("n1").scopes(List.of(
                ScopeIndexData.Entry.builder().scopeId(OWN).published(ownPublished).properties(Map.of(
                        "ccm:editorial_inventory", List.of("accepted"),
                        "ccm:editorial_recommendation", List.of("highly_recommended"),
                        "cclom:title", List.of("Eigener Titel"),
                        "ccm:curriculum", List.of("c1", "c2"))).publicFields(List.of("ccm:editorial_recommendation")).build(),
                ScopeIndexData.Entry.builder().scopeId(OTHER).published(otherPublished).excluded(true).properties(Map.of(
                        "ccm:editorial_inventory", List.of("rejected"),
                        "ccm:editorial_recommendation", List.of("recommended"))).publicFields(List.of("ccm:editorial_recommendation")).build()
        )).build();
    }

    @Test
    void editorsSeeTheirScopeCompletelyWithEffectiveValuesAndOriginals() {
        ScopeNodeView.View view = underTest.view(scope(ScopeRole.EDITOR), data(false, false), nodeProps);

        assertEquals("Eigener Titel", view.properties().get(TITLE));
        assertEquals("c1[#]c2", view.properties().get(CCConstants.getValidGlobalName("ccm:curriculum")));
        // the given map is never modified (it may be cached)
        assertEquals("Originaltitel", nodeProps.get(TITLE));

        NodeScope own = view.scopes().get(0);
        assertEquals("WLO", own.getDisplayName());
        assertEquals(Set.of("ccm:editorial_inventory", "ccm:editorial_recommendation", "cclom:title", "ccm:curriculum"), own.getProperties().keySet());
        assertEquals(List.of("Originaltitel"), own.getOriginals().get("cclom:title"));
        // the other scope's entry is not published: invisible
        assertEquals(1, view.scopes().size());
    }

    @Test
    void guestsSeePublicPropertiesOfPublishedEntriesOnly() {
        assertTrue(underTest.view(scope(ScopeRole.GUEST), data(false, false), nodeProps).scopes().isEmpty());

        ScopeNodeView.View view = underTest.view(scope(ScopeRole.GUEST), data(true, false), nodeProps);
        NodeScope own = view.scopes().get(0);
        assertEquals(Set.of("ccm:editorial_recommendation", "cclom:title"), own.getProperties().keySet());
        assertNull(own.getOriginals());
        // public replacing field is applied for guests, the internal one is not
        assertEquals("Eigener Titel", view.properties().get(TITLE));
        assertNull(view.properties().get(CCConstants.getValidGlobalName("ccm:curriculum")));
    }

    @Test
    void otherScopesOnlyPublishedAndPublicNonReplacing() {
        ScopeNodeView.View view = underTest.view(Optional.empty(), data(true, true), nodeProps);

        // without an active scope nothing is replaced
        assertSame(nodeProps, view.properties());
        assertEquals(List.of(OWN, OTHER), view.scopes().stream().map(NodeScope::getScopeId).toList());
        for (NodeScope scope : view.scopes()) {
            assertEquals(Set.of("ccm:editorial_recommendation"), scope.getProperties().keySet(), scope.getScopeId());
            assertNull(scope.getOriginals());
        }
        assertEquals("Kunde A", view.scopes().get(1).getDisplayName());
    }

    @Test
    void editorOfOneScopeSeesOnlyPublicDataOfAnother() {
        ScopeNodeView.View view = underTest.view(scope(ScopeRole.EDITOR), data(false, true), nodeProps);

        NodeScope other = view.scopes().stream().filter(s -> s.getScopeId().equals(OTHER)).findFirst().orElseThrow();
        assertEquals(Map.of("ccm:editorial_recommendation", List.of("recommended")), other.getProperties());
    }
}
