package org.edu_sharing.service.scope;

import lombok.RequiredArgsConstructor;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.namespace.QName;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Kinds of scope properties, derived from the dictionary:
 * <ul>
 *     <li>scope only: aspect ccm:editorialScopeProperties and all its sub aspects. They only exist in scope entries,
 *     custom models add their editorial properties via an aspect with parent ccm:editorialScopeProperties</li>
 *     <li>every other property is a regular node property: inside the own scope its value replaces the original</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class ScopeProperties {

    private final DictionaryService dictionaryService;
    private volatile Set<String> scopeOnlyProperties;

    /**
     * @return properties that only exist in scope entries (local names)
     */
    public Set<String> getScopeOnlyProperties() {
        if (scopeOnlyProperties == null) {
            QName root = QName.createQName(CCConstants.CCM_ASPECT_EDITORIAL_SCOPE_PROPERTIES);
            scopeOnlyProperties = Stream.concat(Stream.of(root), dictionaryService.getSubAspects(root, true).stream())
                    .distinct()
                    .map(dictionaryService::getAspect)
                    .filter(Objects::nonNull)
                    .flatMap(aspect -> aspect.getProperties().keySet().stream())
                    .map(q -> CCConstants.getValidLocalName(q.toString()))
                    .collect(Collectors.toUnmodifiableSet());
        }
        return scopeOnlyProperties;
    }

    /**
     * @return true if a value of the property replaces the original value of the node (every regular property)
     */
    public boolean replaces(String property) {
        return !getScopeOnlyProperties().contains(property);
    }

    /**
     * sets {@link ScopeField#isReplaces()} of every field from the dictionary, a stored or requested value is ignored
     */
    public List<ScopeField> deriveReplaces(List<ScopeField> fields) {
        if (fields != null) {
            fields.forEach(field -> field.setReplaces(field.getId() != null && replaces(field.getId())));
        }
        return fields;
    }
}
