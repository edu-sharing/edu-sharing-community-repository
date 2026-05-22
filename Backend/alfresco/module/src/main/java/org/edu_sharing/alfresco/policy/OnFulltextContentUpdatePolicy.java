package org.edu_sharing.alfresco.policy;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.ContentServicePolicies.OnContentPropertyUpdatePolicy;
import org.alfresco.repo.node.NodeServicePolicies.OnUpdatePropertiesPolicy;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

/**
 * Clears cached fulltext (ccm:fulltext_content + ccm:fulltext_status) on a ccm:io node when
 * either the primary content (cm:content) or the link URL (ccm:wwwurl) is changed, so the
 * next getTextContent call triggers a fresh extraction.
 *
 * Two policies are bound:
 * - OnContentPropertyUpdatePolicy filtered to cm:content, so writing ccm:fulltext_content
 *   itself does not re-trigger the clear.
 * - OnUpdatePropertiesPolicy filtered to ccm:wwwurl, since wwwurl is d:text and would not
 *   surface through a content-update policy.
 */
public class OnFulltextContentUpdatePolicy implements OnContentPropertyUpdatePolicy, OnUpdatePropertiesPolicy {

    private PolicyComponent policyComponent;
    private NodeService nodeService;

    private static final Logger logger = Logger.getLogger(OnFulltextContentUpdatePolicy.class);

    private static final QName PROP_WWWURL = QName.createQName(CCConstants.CCM_PROP_IO_WWWURL);
    private static final QName PROP_FULLTEXT_STATUS = QName.createQName(CCConstants.CCM_PROP_IO_FULLTEXT_STATUS);
    private static final QName PROP_FULLTEXT_CONTENT = QName.createQName(CCConstants.CCM_PROP_IO_FULLTEXT_CONTENT);

    public void init() {
        QName ccmIoType = QName.createQName(CCConstants.CCM_TYPE_IO);
        policyComponent.bindClassBehaviour(
                OnContentPropertyUpdatePolicy.QNAME,
                ccmIoType,
                new JavaBehaviour(this, "onContentPropertyUpdate")
        );
        policyComponent.bindClassBehaviour(
                OnUpdatePropertiesPolicy.QNAME,
                ccmIoType,
                new JavaBehaviour(this, "onUpdateProperties")
        );
    }

    @Override
    public void onContentPropertyUpdate(NodeRef nodeRef, QName propertyQName, ContentData beforeValue, ContentData afterValue) {
        if (ContentModel.PROP_CONTENT.equals(propertyQName)) {
            clearFulltext(nodeRef);
        }
    }

    @Override
    public void onUpdateProperties(NodeRef nodeRef, Map<QName, Serializable> before, Map<QName, Serializable> after) {
        if (!Objects.equals(before.get(PROP_WWWURL), after.get(PROP_WWWURL))) {
            clearFulltext(nodeRef);
        }
    }

    private void clearFulltext(NodeRef nodeRef) {
        try {
            nodeService.removeProperty(nodeRef, PROP_FULLTEXT_STATUS);
            nodeService.removeProperty(nodeRef, PROP_FULLTEXT_CONTENT);
        } catch (Throwable e) {
            logger.error("Failed to clear fulltext properties on update for node " + nodeRef.getId(), e);
        }
    }

    public void setPolicyComponent(PolicyComponent policyComponent) {
        this.policyComponent = policyComponent;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }
}