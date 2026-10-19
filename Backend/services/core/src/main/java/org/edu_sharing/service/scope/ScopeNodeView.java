package org.edu_sharing.service.scope;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.util.ISO8601DateFormat;
import org.apache.commons.lang3.math.NumberUtils;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.metadata.ValueTool;
import org.edu_sharing.repository.server.tools.DateTool;
import org.edu_sharing.spring.ApplicationContextFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Scope data of a node for the node dto (F5, concept §5.4/§9):
 * <ul>
 *     <li>the properties replaced in the active scope get their scope values (effective values), so node api, detail
 *     page, rendering and metadata templates show them</li>
 *     <li>the visible scope entries: own scope all properties for editors, public properties of published entries for
 *     guests / temporary readers, other scopes only published entries with their public non-replacing properties</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScopeNodeView {

    public record View(Map<String, Object> properties, List<NodeScope> scopes) {
    }

    private final ScopesSettings scopesSettings;
    private final ScopeContextResolver scopeContextResolver;
    private final ScopeDefinitionService definitionService;
    private final ScopeServiceFactory scopeServiceFactory;

    public static Optional<ScopeNodeView> getInstance() {
        ApplicationContext context = ApplicationContextFactory.getApplicationContext();
        return context == null ? Optional.empty() : Optional.ofNullable(context.getBeanProvider(ScopeNodeView.class).getIfAvailable());
    }

    /**
     * @param nodeProps properties of the node (global names, multi values joined), not modified
     * @return the properties with effective values (a copy if anything was replaced) and the visible scope entries,
     * empty if scopes are disabled or the node has no entries
     */
    public Optional<View> apply(String nodeId, Map<String, Object> nodeProps) {
        if (!scopesSettings.isEnabled() || nodeId == null || nodeProps == null || !isIo(nodeProps)) {
            return Optional.empty();
        }
        // the entries themselves are filtered below, reading them needs no permission of the user
        ScopeIndexData data = AuthenticationUtil.runAsSystem(() -> scopeServiceFactory.getLocalService().getIndexData(nodeId));
        if (data == null || data.getScopes() == null || data.getScopes().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(view(scopeContextResolver.resolve(), data, nodeProps));
    }

    View view(Optional<ActiveScope> activeScope, ScopeIndexData data, Map<String, Object> nodeProps) {
        String activeScopeId = activeScope.map(ActiveScope::getScopeId).orElse(null);
        boolean editor = activeScope.map(s -> s.getRole() == ScopeRole.EDITOR).orElse(false);
        Map<String, Object> properties = nodeProps;
        List<NodeScope> scopes = new ArrayList<>();

        for (ScopeIndexData.Entry entry : data.getScopes()) {
            Map<String, List<String>> values = entry.getProperties() == null ? Map.of() : entry.getProperties();
            if (entry.getScopeId().equals(activeScopeId)) {
                if (!editor && !entry.isPublished()) {
                    continue;
                }
                ScopeDefinition definition = activeScope.get().getDefinition();
                Map<String, List<String>> visible = new LinkedHashMap<>();
                Map<String, List<String>> originals = new LinkedHashMap<>();
                for (ScopeField field : definition.getFields() == null ? List.<ScopeField>of() : definition.getFields()) {
                    List<String> value = values.get(field.getId());
                    if (value == null || (!editor && field.getVisibility() != ScopeFieldVisibility.PUBLIC)) {
                        continue;
                    }
                    visible.put(field.getId(), value);
                    if (field.isReplaces() && !value.isEmpty()) {
                        if (properties == nodeProps) {
                            // never modify the given (possibly cached) map
                            properties = new HashMap<>(nodeProps);
                        }
                        String global = CCConstants.getValidGlobalName(field.getId());
                        if (editor) {
                            originals.put(field.getId(), toList(nodeProps.get(global)));
                        }
                        properties.put(global, ValueTool.toMultivalue(value.toArray(new String[0])));
                    }
                }
                // an editorial change is a change of the content inside the scope: "modified" shows the later one
                if (entry.getModified() != null && entry.getModified().getTime() > getModified(nodeProps)) {
                    if (properties == nodeProps) {
                        properties = new HashMap<>(nodeProps);
                    }
                    setModified(properties, nodeProps, entry.getModified());
                }
                scopes.add(nodeScope(entry, definition.getDisplayName(), visible, editor ? originals : null));
            } else {
                if (!entry.isPublished() || entry.getPublicFields() == null) {
                    continue;
                }
                Map<String, List<String>> visible = new LinkedHashMap<>();
                for (String field : entry.getPublicFields()) {
                    if (values.containsKey(field)) {
                        visible.put(field, values.get(field));
                    }
                }
                if (visible.isEmpty()) {
                    continue;
                }
                String displayName = definitionService.getDefinition(entry.getScopeId()).map(ScopeDefinition::getDisplayName).orElse(entry.getScopeId());
                scopes.add(nodeScope(entry, displayName, visible, null));
            }
        }
        return new View(properties, scopes);
    }

    private static NodeScope nodeScope(ScopeIndexData.Entry entry, String displayName, Map<String, List<String>> visible, Map<String, List<String>> originals) {
        return NodeScope.builder()
                .scopeId(entry.getScopeId())
                .displayName(displayName)
                .published(entry.isPublished())
                .excluded(entry.isExcluded())
                .properties(visible)
                .originals(originals)
                .build();
    }

    /**
     * @return cm:modified of the node in milliseconds, 0 if unknown
     */
    static long getModified(Map<String, Object> nodeProps) {
        for (String key : List.of(CCConstants.CM_PROP_C_MODIFIED + CCConstants.LONG_DATE_SUFFIX, CCConstants.CM_PROP_C_MODIFIED)) {
            Object value = nodeProps.get(key);
            if (value instanceof Date date) {
                return date.getTime();
            }
            if (value != null && NumberUtils.isDigits(value.toString())) {
                return Long.parseLong(value.toString());
            }
        }
        return 0;
    }

    /**
     * sets cm:modified in every form of the node properties: milliseconds (or formatted like the original value), the
     * long and the iso variant
     */
    private static void setModified(Map<String, Object> properties, Map<String, Object> nodeProps, Date modified) {
        String millis = String.valueOf(modified.getTime());
        Object original = nodeProps.get(CCConstants.CM_PROP_C_MODIFIED);
        boolean formatted = original != null && !(original instanceof Date) && !NumberUtils.isDigits(original.toString());
        properties.put(CCConstants.CM_PROP_C_MODIFIED, formatted ? new DateTool().formatDate(modified.getTime()) : millis);
        properties.put(CCConstants.CM_PROP_C_MODIFIED + CCConstants.LONG_DATE_SUFFIX, millis);
        properties.put(CCConstants.CM_PROP_C_MODIFIED + CCConstants.ISODATE_SUFFIX, ISO8601DateFormat.format(modified));
    }

    private static List<String> toList(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(String::valueOf).toList();
        }
        return Arrays.asList(ValueTool.getMultivalue(value.toString()));
    }

    private static boolean isIo(Map<String, Object> nodeProps) {
        Object type = nodeProps.get(CCConstants.NODETYPE);
        return type == null || CCConstants.CCM_TYPE_IO.equals(type) || CCConstants.getValidLocalName(CCConstants.CCM_TYPE_IO).equals(type);
    }
}
