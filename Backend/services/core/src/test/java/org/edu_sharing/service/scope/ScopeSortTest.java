package org.edu_sharing.service.scope;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.mapping.RuntimeField;
import co.elastic.clients.elasticsearch._types.mapping.RuntimeFieldType;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScopeSortTest {

    private final ScopesSettings settings = new ScopesSettings();
    private final ScopeContextResolver resolver = mock(ScopeContextResolver.class);
    private final ScopeSort underTest = new ScopeSort(settings, resolver,
            new ScopeCriteria(settings, resolver, mock(ScopeProperties.class)));

    @BeforeEach
    void enable() {
        settings.setEnabled(true);
        ScopesSettings.Sort sort = new ScopesSettings.Sort();
        sort.setStrategy("runtime");
        settings.setSort(sort);
    }

    private void activeScope(ScopeRole role) {
        when(resolver.resolve()).thenReturn(Optional.of(ActiveScope.builder()
                .definition(ScopeCriteriaTest.DEFINITION).role(role).build()));
    }

    @Test
    void replacedFieldsSortByTheScopeValue() {
        activeScope(ScopeRole.EDITOR);
        SearchRequest.Builder builder = new SearchRequest.Builder().index("workspace");
        assertTrue(underTest.applyInternal(builder, "cclom:title", "properties.cclom:title.keyword", SortOrder.Asc));
        SearchRequest request = builder.build();

        RuntimeField field = request.runtimeMappings().get("scope_sort.cclom:title");
        assertEquals(RuntimeFieldType.Keyword, field.type());
        assertEquals(ScopeCriteriaTest.SCOPE, field.script().params().get("scopeId").to(String.class));
        assertFalse(field.script().params().get("publishedOnly").to(Boolean.class));
        assertFalse(field.script().params().get("lowercase").to(Boolean.class));
        assertEquals("scope_sort.cclom:title", request.sort().get(0).field().field());
    }

    @Test
    void readersSortByPublicFieldsOfPublishedEntries() {
        activeScope(ScopeRole.TEMPORARY_READER);
        assertTrue(underTest.createRuntimeField(resolver.resolve().get(), "cclom:title", "properties.cclom:title.sort")
                .map(field -> field.script().params().get("publishedOnly").to(Boolean.class)
                        && field.script().params().get("lowercase").to(Boolean.class))
                .orElse(false));
        // internal replaced field: original order
        assertTrue(underTest.createRuntimeField(resolver.resolve().get(), "ccm:curriculum", "properties.ccm:curriculum.sort").isEmpty());
    }

    @Test
    void modifiedSortsByTheNewerDate() {
        activeScope(ScopeRole.EDITOR);
        assertEquals(RuntimeFieldType.Long, underTest.createRuntimeField(resolver.resolve().get(), "cm:modified", "properties.cm:modified.number")
                .map(RuntimeField::type).orElse(null));
    }

    @Test
    void otherwiseTheRegularSortApplies() {
        activeScope(ScopeRole.EDITOR);
        // not part of the scope / not replacing
        assertTrue(underTest.createRuntimeField(resolver.resolve().get(), "cclom:general_keyword", "properties.cclom:general_keyword.sort").isEmpty());
        assertTrue(underTest.createRuntimeField(resolver.resolve().get(), "ccm:editorial_release", "properties.ccm:editorial_release.sort").isEmpty());

        when(resolver.resolve()).thenReturn(Optional.empty());
        assertFalse(underTest.applyInternal(new SearchRequest.Builder(), "cclom:title", "properties.cclom:title.keyword", SortOrder.Asc));

        activeScope(ScopeRole.EDITOR);
        settings.getSort().setStrategy("merge");
        assertFalse(underTest.applyInternal(new SearchRequest.Builder(), "cclom:title", "properties.cclom:title.keyword", SortOrder.Asc));
    }
}
