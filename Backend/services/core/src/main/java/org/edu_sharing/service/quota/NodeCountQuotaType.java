package org.edu_sharing.service.quota;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import lombok.Getter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.search.SearchService.ContentType;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Kinds of nodes which can be limited in their count per user (owner).
 * The person property holds the max. number of owned nodes, an unset property means unlimited.
 *
 * Existing nodes are counted via elastic using the regular {@link ContentType} filter
 * (see SearchServiceElastic.getContentTypeQuery) plus optional additional filters.
 * {@link #matches(NodeService, NodeRef)} is the equivalent check on a single (newly created, not yet indexed) node
 */
@Getter
public enum NodeCountQuotaType {
    /**
     * all learning objects (incl. collection references, child objects and published copies)
     */
    MATERIAL(CCConstants.CM_PROP_PERSON_MATERIAL_COUNT_QUOTA, ContentType.FILES) {
        @Override
        public boolean matches(NodeService nodeService, NodeRef nodeRef) {
            return isWorkspace(nodeRef) && isType(nodeService, nodeRef, CCConstants.CCM_TYPE_IO);
        }
    },
    /**
     * maps with the collection aspect
     */
    COLLECTION(CCConstants.CM_PROP_PERSON_COLLECTION_COUNT_QUOTA, ContentType.COLLECTIONS) {
        @Override
        public boolean matches(NodeService nodeService, NodeRef nodeRef) {
            return isWorkspace(nodeRef)
                    && isType(nodeService, nodeRef, CCConstants.CCM_TYPE_MAP)
                    && nodeService.hasAspect(nodeRef, QName.createQName(CCConstants.CCM_ASPECT_COLLECTION));
        }
    },
    /**
     * regular maps, i.e. no collections and no special folders (home, inbox, favorites, system folders, ...) which carry a map type
     */
    FOLDER(CCConstants.CM_PROP_PERSON_FOLDER_COUNT_QUOTA, ContentType.FOLDERS) {
        @Override
        public boolean matches(NodeService nodeService, NodeRef nodeRef) {
            if (!isWorkspace(nodeRef)
                    || !isType(nodeService, nodeRef, CCConstants.CCM_TYPE_MAP)
                    || nodeService.hasAspect(nodeRef, QName.createQName(CCConstants.CCM_ASPECT_COLLECTION))) {
                return false;
            }
            Serializable mapType = nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_MAP_TYPE));
            return mapType == null || StringUtils.isEmpty(mapType.toString());
        }

        @Override
        public List<Query> getAdditionalFilters() {
            return List.of(QueryBuilders.bool(b -> b.mustNot(m -> m.exists(
                    e -> e.field("properties." + CCConstants.getValidLocalName(CCConstants.CCM_PROP_MAP_TYPE))))));
        }
    };

    /**
     * the (long) name of the person property holding the limit
     */
    private final String personProperty;

    /**
     * the content type filter used to count the existing nodes
     */
    private final ContentType contentType;

    NodeCountQuotaType(String personProperty, ContentType contentType) {
        this.personProperty = personProperty;
        this.contentType = contentType;
    }

    /**
     * @return true if the given (existing) node is counted for this quota type
     */
    public abstract boolean matches(NodeService nodeService, NodeRef nodeRef);

    /**
     * @return elastic filters which are applied in addition to the {@link #getContentType()} filter
     */
    public List<Query> getAdditionalFilters() {
        return Collections.emptyList();
    }

    private static boolean isWorkspace(NodeRef nodeRef) {
        return StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.equals(nodeRef.getStoreRef());
    }

    private static boolean isType(NodeService nodeService, NodeRef nodeRef, String type) {
        return nodeService.getType(nodeRef).equals(QName.createQName(type));
    }
}
