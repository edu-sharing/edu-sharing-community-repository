package org.edu_sharing.service.scope;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.json.JsonpUtils;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScopeRelevanceTest {

    private final ScopesSettings settings = new ScopesSettings();
    private final ScopeContextResolver resolver = mock(ScopeContextResolver.class);
    private final ScopeRelevance underTest = new ScopeRelevance(settings, resolver);

    @BeforeEach
    void enable() {
        settings.setEnabled(true);
        ScopesSettings.Relevance relevance = new ScopesSettings.Relevance();
        relevance.setRecommendedBoost(3);
        settings.setRelevance(relevance);
    }

    private void activeScope(ScopeRole role) {
        when(resolver.resolve()).thenReturn(Optional.of(ActiveScope.builder()
                .definition(ScopeCriteriaTest.DEFINITION).role(role).build()));
    }

    private static String json(Query query) {
        return JsonpUtils.toJsonString(query, new JacksonJsonpMapper());
    }

    @Test
    void recommendedEntriesOfTheActiveScopeAreBoosted() {
        activeScope(ScopeRole.EDITOR);
        String json = json(underTest.boostInternal().orElseThrow());
        assertTrue(json.contains("\"path\":\"scopes\""), json);
        assertTrue(json.contains("\"boost\":3.0"), json);
        assertTrue(json.contains(ScopeCriteriaTest.SCOPE), json);
        assertTrue(json.contains("\"" + ScopeRelevance.INVENTORY_FIELD + "\":{\"value\":\"recommended\"}"), json);
        assertFalse(json.contains("scopes.published"), json);
    }

    @Test
    void readersOnlyForPublishedEntries() {
        activeScope(ScopeRole.TEMPORARY_READER);
        assertTrue(json(underTest.boostInternal().orElseThrow()).contains("scopes.published"));
    }

    @Test
    void noBoostWithoutScopeOrBoost() {
        when(resolver.resolve()).thenReturn(Optional.empty());
        assertTrue(underTest.boostInternal().isEmpty());

        activeScope(ScopeRole.EDITOR);
        settings.getRelevance().setRecommendedBoost(0);
        assertTrue(underTest.boostInternal().isEmpty());
    }
}
