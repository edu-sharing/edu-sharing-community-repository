package org.edu_sharing.service.scope;

import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.QName;
import org.edu_sharing.alfresco.service.OrganisationService;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.InsufficientPermissionException;
import org.edu_sharing.service.permission.annotation.Permission;
import org.edu_sharing.service.authority.AuthorityService;
import org.edu_sharing.service.search.SearchServiceElastic;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;

/**
 * Reads and writes the scope definition stored at the scope group (groupType = SCOPE, aspect ccm:scopeGroup).
 */
@Slf4j
@Service
@ScopesFeature
@RequiredArgsConstructor
public class ScopeDefinitionServiceImpl implements ScopeDefinitionService {

    private static final int MAX_SCOPES = 10000;
    private static final String VIRTUAL_PREFIX = "virtual:";

    private final org.alfresco.service.cmr.security.AuthorityService alfAuthorityService;
    private final NodeService nodeService;
    private final AuthorityService authorityService;
    private final RetryingTransactionHelper retryingTransactionHelper;
    // lazy: search will depend on the active scope (ScopeContextResolver -> this service)
    private final ObjectProvider<SearchServiceElastic> searchServiceElastic;
    private final OrganisationService organisationService;
    private final DictionaryService dictionaryService;
    private final PermissionService permissionService;
    private final ScopeProperties scopeProperties;
    // lazy: the scope service depends on this service
    private final ObjectProvider<ScopeServiceFactory> scopeServiceFactory;

    @Override
    public Optional<ScopeDefinition> getDefinition(String scopeId) {
        // group nodes are not readable for every user, the definition itself is not confidential
        return Optional.ofNullable(AuthenticationUtil.runAsSystem(() -> readDefinition(scopeId)));
    }

    private ScopeDefinition readDefinition(String scopeId) {
        if (scopeId == null || !scopeId.startsWith(org.alfresco.service.cmr.security.PermissionService.GROUP_PREFIX)) {
            return null;
        }
        NodeRef nodeRef = alfAuthorityService.getAuthorityNodeRef(scopeId);
        if (nodeRef == null) {
            return null;
        }
        Map<QName, Serializable> properties = nodeService.getProperties(nodeRef);
        if (!CCConstants.GROUP_TYPE_SCOPE.equals(properties.get(QName.createQName(CCConstants.CCM_PROP_GROUPEXTENSION_GROUPTYPE)))) {
            return null;
        }
        NodeRef folder = (NodeRef) properties.get(QName.createQName(CCConstants.CCM_PROP_SCOPE_FOLDER));
        return ScopeDefinition.builder()
                .scopeId(scopeId)
                .displayName(alfAuthorityService.getAuthorityDisplayName(scopeId))
                .mds((String) properties.get(QName.createQName(CCConstants.CCM_PROP_SCOPE_MDS)))
                .fields(scopeProperties.deriveReplaces(ScopeDefinitionJson.parseFields((String) properties.get(QName.createQName(CCConstants.CCM_PROP_SCOPE_FIELDS)))))
                .presentation(ScopeDefinitionJson.parsePresentation((String) properties.get(QName.createQName(CCConstants.CCM_PROP_SCOPE_SETTINGS))))
                .baseQuery(ScopeDefinitionJson.parseBaseQuery(toStringList(properties.get(QName.createQName(CCConstants.CCM_PROP_SCOPE_BASE_QUERY))),
                        (Long) properties.get(QName.createQName(CCConstants.CCM_PROP_SCOPE_BASE_QUERY_VERSION))))
                .folderId(folder == null ? null : folder.getId())
                .build();
    }

    @SuppressWarnings("unchecked")
    private static List<String> toStringList(Serializable value) {
        if (value == null) {
            return null;
        }
        if (value instanceof List<?> list) {
            return (List<String>) list;
        }
        // single value, e.g. written directly via the node api
        return List.of(value.toString());
    }

    @Override
    @Permission(requiresGlobalAdmin = true)
    public List<ScopeDefinition> getAllScopes() {
        try {
            SearchResponse<Map> response = searchServiceElastic.getObject().searchNative(SearchRequest.of(req -> req
                    .index(SearchServiceElastic.AUTHORITIES_INDEX)
                    .size(MAX_SCOPES)
                    .source(src -> src.filter(f -> f.includes("properties.cm:authorityName")))
                    .query(q -> q.bool(b -> b
                            .filter(f -> f.term(t -> t.field("properties.ccm:groupType.keyword").value(CCConstants.GROUP_TYPE_SCOPE)))
                            .filter(f -> f.term(t -> t.field("nodeRef.storeRef.protocol").value("workspace")))))));
            List<ScopeDefinition> result = new ArrayList<>();
            for (Hit<Map> hit : response.hits().hits()) {
                Object authorityName = ((Map<?, ?>) hit.source().get("properties")).get("cm:authorityName");
                getDefinition(String.valueOf(authorityName)).ifPresent(result::add);
            }
            result.sort(Comparator.comparing(ScopeDefinition::getDisplayName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
            return result;
        } catch (IOException e) {
            throw new RuntimeException("could not search scope groups", e);
        }
    }

    @Override
    @Permission(requiresGlobalAdmin = true)
    public List<String> getScopeOnlyProperties() {
        return scopeProperties.getScopeOnlyProperties().stream().sorted().toList();
    }

    @Override
    public List<ScopeDefinition> getScopesOfCurrentUser() {
        String user = AuthenticationUtil.getFullyAuthenticatedUser();
        List<ScopeDefinition> result = new ArrayList<>();
        for (String authority : authorityService.getMemberships(user)) {
            getDefinition(authority).ifPresent(result::add);
        }
        result.sort(Comparator.comparing(ScopeDefinition::getDisplayName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        return result;
    }

    @Override
    public boolean isMember(String scopeId) {
        return authorityService.getMemberships(AuthenticationUtil.getFullyAuthenticatedUser()).contains(scopeId);
    }

    @Override
    @Permission(requiresGlobalAdmin = true)
    public ScopeDefinition createScope(String name, String displayName, String mds, List<ScopeField> fields, ScopePresentation presentation) {
        validateFields(fields);
        validatePresentation(presentation);
        String groupName = AuthenticationUtil.runAsSystem(() -> retryingTransactionHelper.doInTransaction(() -> {
            String group = organisationService.createOrganization(name, displayName, mds, null);
            String scopeId = org.alfresco.service.cmr.security.PermissionService.GROUP_PREFIX + group;
            NodeRef groupRef = alfAuthorityService.getAuthorityNodeRef(scopeId);
            NodeRef folder = (NodeRef) nodeService.getProperty(groupRef, OrganisationService.PROP_EDUGROUP_EDU_HOMEDIR);

            Map<QName, Serializable> properties = new HashMap<>();
            properties.put(QName.createQName(CCConstants.CCM_PROP_SCOPE_MDS), mds);
            properties.put(QName.createQName(CCConstants.CCM_PROP_SCOPE_FIELDS), ScopeDefinitionJson.write(fields == null ? List.of() : fields));
            properties.put(QName.createQName(CCConstants.CCM_PROP_SCOPE_FOLDER), folder);
            properties.put(QName.createQName(CCConstants.CCM_PROP_SCOPE_SETTINGS), writePresentation(presentation));
            nodeService.addAspect(groupRef, QName.createQName(CCConstants.CCM_ASPECT_SCOPE_GROUP), properties);
            nodeService.setProperty(groupRef, QName.createQName(CCConstants.CCM_PROP_GROUPEXTENSION_GROUPTYPE), CCConstants.GROUP_TYPE_SCOPE);

            // shared folder: members can create and edit, not only read (concept §4.7)
            permissionService.deletePermission(folder, scopeId, PermissionService.CONSUMER);
            permissionService.setPermission(folder, scopeId, CCConstants.PERMISSION_COLLABORATOR, true);
            return group;
        }));
        String scopeId = org.alfresco.service.cmr.security.PermissionService.GROUP_PREFIX + groupName;
        log.info("scope group {} created", scopeId);
        return getDefinition(scopeId).orElseThrow();
    }

    @Override
    @Permission(requiresGlobalAdmin = true)
    public ScopeDefinition updateScope(String scopeId, String mds, List<ScopeField> fields, ScopePresentation presentation) {
        getDefinition(scopeId).orElseThrow(() -> new ScopeNotFoundException("unknown scope " + scopeId));
        validateFields(fields);
        validatePresentation(presentation);
        AuthenticationUtil.runAsSystem(() -> retryingTransactionHelper.doInTransaction(() -> {
            NodeRef groupRef = alfAuthorityService.getAuthorityNodeRef(scopeId);
            nodeService.setProperty(groupRef, QName.createQName(CCConstants.CCM_PROP_SCOPE_MDS), mds);
            nodeService.setProperty(groupRef, QName.createQName(CCConstants.CCM_PROP_SCOPE_FIELDS), ScopeDefinitionJson.write(fields == null ? List.of() : fields));
            nodeService.setProperty(groupRef, QName.createQName(CCConstants.CCM_PROP_SCOPE_SETTINGS), writePresentation(presentation));
            return null;
        }));
        // published / excluded of the entries depend on the field list
        scopeServiceFactory.getObject().getLocalService().onDefinitionChanged(scopeId);
        return getDefinition(scopeId).orElseThrow();
    }

    /**
     * card columns: properties of the dictionary or virtual properties of the mds, each once
     */
    private void validatePresentation(ScopePresentation presentation) {
        if (presentation == null || presentation.getCardColumns() == null) {
            return;
        }
        Set<String> ids = new HashSet<>();
        for (String column : presentation.getCardColumns()) {
            boolean known = column != null && (column.startsWith(VIRTUAL_PREFIX)
                    || dictionaryService.getProperty(QName.createQName(CCConstants.getValidGlobalName(column))) != null);
            if (!known) {
                throw new IllegalArgumentException("card column " + column + " is not known by the dictionary");
            }
            if (!ids.add(column)) {
                throw new IllegalArgumentException("card column " + column + " is configured twice");
            }
        }
    }

    /**
     * @return the json of the settings, null if nothing is set (removes the property)
     */
    private static String writePresentation(ScopePresentation presentation) {
        boolean empty = presentation == null || presentation.getCardColumns() == null || presentation.getCardColumns().isEmpty();
        return empty ? null : ScopeDefinitionJson.write(presentation);
    }

    /**
     * only properties of the dictionary are allowed (es field limit, validation of values)
     */
    private void validateFields(List<ScopeField> fields) {
        if (fields == null) {
            return;
        }
        // replaces is not configurable: regular properties replace, scope only properties don't
        scopeProperties.deriveReplaces(fields);
        Set<String> ids = new HashSet<>();
        for (ScopeField field : fields) {
            if (field.getId() == null || dictionaryService.getProperty(QName.createQName(CCConstants.getValidGlobalName(field.getId()))) == null) {
                throw new IllegalArgumentException("scope field " + field.getId() + " is not known by the dictionary");
            }
            if (!ids.add(field.getId())) {
                throw new IllegalArgumentException("scope field " + field.getId() + " is configured twice");
            }
        }
    }

    @Override
    public ScopeBaseQuery setBaseQuery(String scopeId, ScopeBaseQuery query) {
        ScopeDefinition definition = getDefinition(scopeId)
                .orElseThrow(() -> new ScopeNotFoundException("unknown scope " + scopeId));
        // org admins of the scope group only (no toolpermission, see concept §8)
        if (!authorityService.hasAdminAccessToGroup(scopeId)) {
            throw new InsufficientPermissionException("only org admins of the scope group can change its base query");
        }
        if (query == null || query.getQueries() == null) {
            throw new IllegalArgumentException("queries must be set");
        }
        long previousVersion = definition.getBaseQuery() == null ? 0 : definition.getBaseQuery().getVersion();
        ScopeBaseQuery stored = ScopeBaseQuery.builder()
                .version(previousVersion + 1)
                .queries(query.getQueries())
                .build();
        ArrayList<String> values = ScopeDefinitionJson.writeBaseQueries(stored.getQueries());
        AuthenticationUtil.runAsSystem(() -> retryingTransactionHelper.doInTransaction(() -> {
            NodeRef groupRef = alfAuthorityService.getAuthorityNodeRef(scopeId);
            nodeService.setProperty(groupRef, QName.createQName(CCConstants.CCM_PROP_SCOPE_BASE_QUERY), values);
            nodeService.setProperty(groupRef, QName.createQName(CCConstants.CCM_PROP_SCOPE_BASE_QUERY_VERSION), stored.getVersion());
            return null;
        }));
        log.info("base query of scope {} changed to version {}", scopeId, stored.getVersion());
        return stored;
    }
}
