package org.edu_sharing.repository.server;

import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;

/**
 * Binds content that was already written to the content store (via an unbound ContentWriter) to a node.
 * Alfresco's ContentPropertyRestrictionInterceptor blocks setting a content property with a new contentUrl
 * through the public NodeService; this class is whitelisted in contentPropertyRestrictions.whitelist
 * (alfresco-global.properties).
 * Only ever pass ContentData produced by our own writer - never data originating from a client request,
 * otherwise a caller could point a node to foreign content.
 */
final class ContentDataBinder {
    private ContentDataBinder() {
    }

    static void bind(NodeService nodeService, NodeRef nodeRef, QName property, ContentData contentData) {
        nodeService.setProperty(nodeRef, property, contentData);
    }
}
