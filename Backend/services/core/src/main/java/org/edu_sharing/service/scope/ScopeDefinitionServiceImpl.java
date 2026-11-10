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
    /** the relevance as sort column */
    static final String SORT_SCORE = "score";

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
                .fields(scopeProperties.applyFixedSettings(ScopeDefinitionJson.parseFields((String) properties.get(QName.createQName(CCConstants.CCM_PROP_SCOPE_FIELDS)))))
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
            markAsScope(scopeId, groupRef, folder, mds, fields, presentation);
            return group;
        }));
        String scopeId = org.alfresco.service.cmr.security.PermissionService.GROUP_PREFIX + groupName;
        log.info("scope group {} created", scopeId);
        return getDefinition(scopeId).orElseThrow();
    }

    @Override
    @Permission(requiresGlobalAdmin = true)
    public ScopeDefinition convertToScope(String scopeId, String mds, List<ScopeField> fields, ScopePresentation presentation) {
        validateFields(fields);
        validatePresentation(presentation);
        AuthenticationUtil.runAsSystem(() -> retryingTransactionHelper.doInTransaction(() -> {
            NodeRef groupRef = scopeId == null || !scopeId.startsWith(org.alfresco.service.cmr.security.PermissionService.GROUP_PREFIX)
                    ? null : alfAuthorityService.getAuthorityNodeRef(scopeId);
            if (groupRef == null) {
                throw new IllegalArgumentException("unknown group " + scopeId);
            }
            Map<QName, Serializable> properties = nodeService.getProperties(groupRef);
            NodeRef folder = (NodeRef) properties.get(OrganisationService.PROP_EDUGROUP_EDU_HOMEDIR);
            if (folder == null || !nodeService.exists(folder)) {
                throw new IllegalArgumentException(scopeId + " is no organization (no shared folder)");
            }
            String groupType = (String) properties.get(QName.createQName(CCConstants.CCM_PROP_GROUPEXTENSION_GROUPTYPE));
            if (CCConstants.GROUP_TYPE_SCOPE.equals(groupType)) {
                throw new IllegalArgumentException(scopeId + " is already a scope group");
            }
            if (org.edu_sharing.alfresco.service.AuthorityService.MEDIA_CENTER_GROUP_TYPE.equals(groupType)) {
                throw new IllegalArgumentException(scopeId + " is a media center");
            }
            if (properties.get(QName.createQName(CCConstants.CCM_PROP_EDUSCOPE_NAME)) != null) {
                throw new IllegalArgumentException(scopeId + " belongs to an edu scope");
            }
            String scopeMds = mds != null ? mds : (String) nodeService.getProperty(folder, QName.createQName(CCConstants.CM_PROP_METADATASET_EDU_METADATASET));
            markAsScope(scopeId, groupRef, folder, scopeMds, fields, presentation);
            return null;
        }));
        log.info("organization {} converted to a scope group", scopeId);
        // after the commit: the job reads the definition
        String jobId = scopeServiceFactory.getObject().getLocalService().startFolderImport(scopeId);
        log.info("import of the shared folder of scope {}: bulk job {}", scopeId, jobId);
        return getDefinition(scopeId).orElseThrow();
    }

    /**
     * marks the group as scope group: definition (aspect ccm:scopeGroup), group type and Collaborator for the members
     * on the shared folder (concept §4.7: members can create and edit, not only read). Runs inside the transaction
     */
    private void markAsScope(String scopeId, NodeRef groupRef, NodeRef folder, String mds, List<ScopeField> fields, ScopePresentation presentation) {
        Map<QName, Serializable> properties = new HashMap<>();
        properties.put(QName.createQName(CCConstants.CCM_PROP_SCOPE_MDS), mds);
        properties.put(QName.createQName(CCConstants.CCM_PROP_SCOPE_FIELDS), ScopeDefinitionJson.write(fields == null ? List.of() : fields));
        properties.put(QName.createQName(CCConstants.CCM_PROP_SCOPE_FOLDER), folder);
        properties.put(QName.createQName(CCConstants.CCM_PROP_SCOPE_SETTINGS), writePresentation(presentation));
        nodeService.addAspect(groupRef, QName.createQName(CCConstants.CCM_ASPECT_SCOPE_GROUP), properties);
        nodeService.setProperty(groupRef, QName.createQName(CCConstants.CCM_PROP_GROUPEXTENSION_GROUPTYPE), CCConstants.GROUP_TYPE_SCOPE);

        permissionService.deletePermission(folder, scopeId, PermissionService.CONSUMER);
        permissionService.setPermission(folder, scopeId, CCConstants.PERMISSION_COLLABORATOR, true);
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
     * card columns: properties of the dictionary or virtual properties of the mds, each once.
     * sort columns: properties of the dictionary or the relevance ("score"), each once. Scope only properties are
     * not sortable: their values only exist in the nested scope entries, ScopeSort only sorts the replacing fields
     */
    private void validatePresentation(ScopePresentation presentation) {
        if (presentation == null) {
            return;
        }
        validateColumns("card column", presentation.getCardColumns(), column -> column.startsWith(VIRTUAL_PREFIX));
        validateColumns("sort column", presentation.getSortColumns(), SORT_SCORE::equals);
        if (presentation.getSortColumns() != null) {
            Set<String> scopeOnly = scopeProperties.getScopeOnlyProperties();
            presentation.getSortColumns().stream().filter(scopeOnly::contains).findFirst().ifPresent(column -> {
                throw new IllegalArgumentException("sort column " + column + " is a scope only property and can not be sorted");
            });
        }
    }

    private void validateColumns(String kind, List<String> columns, java.util.function.Predicate<String> additionallyAllowed) {
        if (columns == null) {
            return;
        }
        Set<String> ids = new HashSet<>();
        for (String column : columns) {
            boolean known = column != null && (additionallyAllowed.test(column)
                    || dictionaryService.getProperty(QName.createQName(CCConstants.getValidGlobalName(column))) != null);
            if (!known) {
                throw new IllegalArgumentException(kind + " " + column + " is not known by the dictionary");
            }
            if (!ids.add(column)) {
                throw new IllegalArgumentException(kind + " " + column + " is configured twice");
            }
        }
    }

    /**
     * @return the json of the settings, null if nothing is set (removes the property)
     */
    private static String writePresentation(ScopePresentation presentation) {
        if (presentation == null) {
            return null;
        }
        // an empty list is the mds default: not stored. All other (future) fields are kept
        ScopePresentation stored = presentation.toBuilder()
                .cardColumns(emptyToNull(presentation.getCardColumns()))
                .sortColumns(emptyToNull(presentation.getSortColumns()))
                .build();
        return stored.equals(new ScopePresentation()) ? null : ScopeDefinitionJson.write(stored);
    }

    private static List<String> emptyToNull(List<String> columns) {
        return columns == null || columns.isEmpty() ? null : columns;
    }

    /**
     * only properties of the dictionary are allowed (es field limit, validation of values)
     */
    private void validateFields(List<ScopeField> fields) {
        if (fields == null) {
            return;
        }
        // replaces and the visibility of the inventory are not configurable
        scopeProperties.applyFixedSettings(fields);
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
