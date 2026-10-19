package org.edu_sharing.service.scope;

import org.alfresco.service.cmr.dictionary.AspectDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.namespace.QName;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScopePropertiesTest {

    private ScopeProperties underTest;

    @BeforeEach
    void setUp() {
        DictionaryService dictionary = mock(DictionaryService.class);
        QName root = QName.createQName(CCConstants.CCM_ASPECT_EDITORIAL_SCOPE_PROPERTIES);
        QName custom = QName.createQName(CCConstants.NAMESPACE_CCM, "customEditorialScopeProperties");
        when(dictionary.getSubAspects(root, true)).thenReturn(List.of(custom));
        AspectDefinition rootAspect = aspect("editorial_inventory");
        AspectDefinition customAspect = aspect("editorial_release");
        when(dictionary.getAspect(root)).thenReturn(rootAspect);
        when(dictionary.getAspect(custom)).thenReturn(customAspect);
        underTest = new ScopeProperties(dictionary);
    }

    @Test
    void scopeOnlyPropertiesIncludeSubAspects() {
        assertEquals(Set.of("ccm:editorial_inventory", "ccm:editorial_release"), underTest.getScopeOnlyProperties());
    }

    @Test
    void replacesIsDerivedAndIgnoresTheRequest() {
        List<ScopeField> fields = List.of(
                ScopeField.builder().id("ccm:editorial_inventory").replaces(true).build(),
                ScopeField.builder().id("ccm:editorial_release").build(),
                ScopeField.builder().id("cclom:title").replaces(false).build());
        underTest.deriveReplaces(fields);
        assertFalse(fields.get(0).isReplaces());
        assertFalse(fields.get(1).isReplaces());
        assertTrue(fields.get(2).isReplaces());
    }

    private static AspectDefinition aspect(String property) {
        AspectDefinition aspect = mock(AspectDefinition.class);
        PropertyDefinition definition = mock(PropertyDefinition.class);
        when(aspect.getProperties()).thenReturn(Map.of(QName.createQName(CCConstants.NAMESPACE_CCM, property), definition));
        return aspect;
    }
}
