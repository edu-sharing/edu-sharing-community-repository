package org.edu_sharing.service.scope;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.json.JsonpUtils;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ScopeCriteriaTest {

    static final String SCOPE = "GROUP_ORG_wlo";
    static final ScopeDefinition DEFINITION = ScopeDefinition.builder().scopeId(SCOPE).fields(List.of(
            ScopeField.builder().id("ccm:editorial_release").visibility(ScopeFieldVisibility.INTERNAL).build(),
            ScopeField.builder().id("ccm:editorial_recommendation").visibility(ScopeFieldVisibility.PUBLIC).build(),
            ScopeField.builder().id("cclom:title").visibility(ScopeFieldVisibility.PUBLIC).replaces(true).build(),
            ScopeField.builder().id("ccm:curriculum").visibility(ScopeFieldVisibility.INTERNAL).replaces(true).build())).build();

    private final ScopeCriteria underTest = new ScopeCriteria(new ScopesSettings(), mock(ScopeContextResolver.class), mock(ScopeProperties.class)) {
        @Override
        Set<String> getScopeOnlyProperties() {
            return Set.of("ccm:editorial_release", "ccm:editorial_recommendation", "ccm:editorial_inventory");
        }
    };

    private static Optional<ActiveScope> scope(String role) {
        return role.equals("-") ? Optional.empty()
                : Optional.of(ActiveScope.builder().definition(DEFINITION).role(ScopeRole.valueOf(role)).build());
    }

    @ParameterizedTest(name = "{0} / {1} -> {2}")
    @CsvSource({
            // own scope
            "EDITOR,           ccm:editorial_release,        OWN",
            "EDITOR,           ccm:editorial_recommendation, OWN",
            "EDITOR,           cclom:title,                  OWN_REPLACING",
            "EDITOR,           ccm:curriculum,               OWN_REPLACING",
            "GUEST,            ccm:editorial_release,        NONE",
            "GUEST,            ccm:editorial_recommendation, OWN",
            "GUEST,            cclom:title,                  OWN_REPLACING",
            "TEMPORARY_READER, ccm:curriculum,               NONE",
            // scope properties of other scopes: published and public only
            "EDITOR,           ccm:editorial_inventory,      FOREIGN",
            "-,                ccm:editorial_recommendation, FOREIGN",
            // regular properties: replaced values never outside the own scope
            "-,                cclom:title,                  ORIGINAL",
            "EDITOR,           cclom:general_keyword,        ORIGINAL",
            // editorial work sets
            "EDITOR,           virtual:scope_view,           ORIGINAL",
            "GUEST,            virtual:scope_view,           IGNORE",
            "-,                virtual:scope_view,           IGNORE",
    })
    void mode(String role, String criterion, ScopeCriteriaRewriter.Mode expected) {
        assertEquals(expected, underTest.getMode(scope(role), criterion));
    }

    private static String json(Query query) {
        String json = JsonpUtils.toJsonString(query, new JacksonJsonpMapper());
        // decode (nested) wrapper queries for assertions
        java.util.regex.Pattern wrapper = java.util.regex.Pattern.compile("\"query\":\"([A-Za-z0-9+/=]{8,})\"");
        for (java.util.regex.Matcher m = wrapper.matcher(json); m.find(); m = wrapper.matcher(json)) {
            StringBuilder sb = new StringBuilder();
            m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(new String(Base64.getDecoder().decode(m.group(1)), StandardCharsets.UTF_8)));
            m.appendTail(sb);
            json = sb.toString();
        }
        return json;
    }

    private static final Query TERM = Query.of(q -> q.term(t -> t.field("properties.ccm:editorial_release.keyword").value("released")));

    @Test
    void fieldsAreMovedIntoTheScopeEntries() {
        Query exists = Query.of(q -> q.exists(e -> e.field("properties.ccm:editorial_release")));
        Query other = Query.of(q -> q.term(t -> t.field("properties.ccm:editorial_release_x.keyword").value("x")));

        assertTrue(json(ScopeCriteriaRewriter.rewrite(TERM, "ccm:editorial_release")).contains("\"scopes.properties.ccm:editorial_release.keyword\""));
        assertTrue(json(ScopeCriteriaRewriter.rewrite(exists, "ccm:editorial_release")).contains("\"scopes.properties.ccm:editorial_release\""));
        // only the exact property, not one that merely starts with the same name
        assertTrue(json(ScopeCriteriaRewriter.rewrite(other, "ccm:editorial_release")).contains("\"properties.ccm:editorial_release_x.keyword\""));
    }

    /**
     * ngsearchword searches many fields: inside the scope it also matches the replaced values of the own entry
     */
    @Test
    void fullTextAlsoMatchesTheReplacedValuesOfTheOwnScope() {
        Query fullText = Query.of(q -> q.bool(b -> b
                .should(s -> s.wildcard(w -> w.field("properties.cclom:title.keyword").value("*test*")))
                .should(s -> s.wildcard(w -> w.field("properties.cm:name.keyword").value("*test*")))));

        assertEquals(List.of("cclom:title", "ccm:curriculum"), underTest.getVisibleReplacingFields(scope("EDITOR").orElseThrow()));
        // readers: public replacing fields only
        assertEquals(List.of("cclom:title"), underTest.getVisibleReplacingFields(scope("GUEST").orElseThrow()));

        String editor = json(ScopeCriteriaRewriter.fullText(SCOPE, List.of("cclom:title"), false, fullText));
        // the original query stays, the replaced title is searched in the own entry
        assertTrue(editor.contains("\"properties.cclom:title.keyword\""));
        assertTrue(editor.contains("\"scopes.properties.cclom:title.keyword\""));
        assertTrue(editor.contains("\"scopes.scopeId\""));
        assertFalse(editor.contains("\"scopes.published\""));
        // cm:name is not replaced: never moved
        assertFalse(editor.contains("scopes.properties.cm:name"));
        assertTrue(json(ScopeCriteriaRewriter.fullText(SCOPE, List.of("cclom:title"), true, fullText)).contains("\"scopes.published\""));
        // no replacing fields: unchanged
        assertSame(fullText, ScopeCriteriaRewriter.fullText(SCOPE, List.of(), false, fullText));
    }

    @Test
    void mdsStatementsInWrapperQueriesAreRewritten() {
        // like multiplejoin OR: a bool of wrapper queries built from the mds statement
        Query statement = Query.of(q -> q.bool(b -> b
                .should(s -> s.wrapper(new org.edu_sharing.service.search.ReadableWrapperQueryBuilder("{\"term\":{\"properties.ccm:editorial_release.keyword\":\"released\"}}").build()))
                .should(s -> s.wrapper(new org.edu_sharing.service.search.ReadableWrapperQueryBuilder("{\"term\":{\"properties.ccm:editorial_release.keyword\":\"withdrawn\"}}").build()))));

        String json = json(ScopeCriteriaRewriter.rewrite(statement, "ccm:editorial_release"));

        assertTrue(json.contains("\"scopes.properties.ccm:editorial_release.keyword\":\"released\""), json);
        assertTrue(json.contains("scopes.properties.ccm:editorial_release.keyword\":\"withdrawn\""), json);
        assertFalse(json.contains("\"properties.ccm:editorial_release.keyword\""), json);
    }

    @Test
    void ownScopeIsNestedAndFiltered() {
        String json = json(ScopeCriteriaRewriter.apply(ScopeCriteriaRewriter.Mode.OWN, SCOPE, "ccm:editorial_release", TERM));
        assertTrue(json.contains("\"path\":\"scopes\""), json);
        assertTrue(json.contains("\"scopes.scopeId\":{\"value\":\"" + SCOPE + "\"}"), json);
        assertTrue(json.contains("scopes.properties.ccm:editorial_release.keyword"), json);
    }

    @Test
    void replacingFieldsUseTheScopeValueOrTheOriginal() {
        Query title = Query.of(q -> q.match(m -> m.field("properties.cclom:title").query("Physik")));
        String json = json(ScopeCriteriaRewriter.apply(ScopeCriteriaRewriter.Mode.OWN_REPLACING, SCOPE, "cclom:title", title));
        assertTrue(json.contains("scopes.properties.cclom:title"), json);
        // original value only where not replaced in this scope
        assertTrue(json.contains("\"must_not\":[{\"term\":{\"scope_overrides\":{\"value\":\"" + SCOPE + "|cclom:title\"}}}]"), json);
        assertTrue(json.contains("\"properties.cclom:title\""), json);
    }

    @Test
    void foreignScopesOnlyPublishedAndPublic() {
        String json = json(ScopeCriteriaRewriter.apply(ScopeCriteriaRewriter.Mode.FOREIGN, null, "ccm:editorial_release", TERM));
        assertTrue(json.contains("\"scopes.published\":{\"value\":true}"), json);
        assertTrue(json.contains("\"scopes.publicFields\":{\"value\":\"ccm:editorial_release\"}"), json);
        assertFalse(json.contains("scopes.scopeId"), json);
    }

    @Test
    void noneAndIgnore() {
        assertTrue(ScopeCriteriaRewriter.apply(ScopeCriteriaRewriter.Mode.NONE, SCOPE, "x", TERM).isMatchNone());
        assertTrue(ScopeCriteriaRewriter.apply(ScopeCriteriaRewriter.Mode.IGNORE, SCOPE, "x", TERM).isMatchAll());
        assertSame(TERM, ScopeCriteriaRewriter.apply(ScopeCriteriaRewriter.Mode.ORIGINAL, SCOPE, "x", TERM));
    }
}
