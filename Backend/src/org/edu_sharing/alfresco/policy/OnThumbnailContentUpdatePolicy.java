package org.edu_sharing.alfresco.policy;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.ContentServicePolicies.OnContentUpdatePolicy;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.cache.RepositoryCache;

/**
 * Policy for removing IO from Cache when corresponding thumbnail changed(thumbnail generation is often called asynchron
 * @author rudi
 *
 */
public class OnThumbnailContentUpdatePolicy  implements OnContentUpdatePolicy {

	
	PolicyComponent policyComponent;

	NodeService nodeService;
	
	Logger logger = Logger.getLogger(OnThumbnailContentUpdatePolicy.class);

	public void init() {
		logger.info("called!");
		policyComponent.bindClassBehaviour(OnContentUpdatePolicy.QNAME, ContentModel.TYPE_THUMBNAIL, new JavaBehaviour(this,
				"onContentUpdate"));
	}
	
	@Override
	public void onContentUpdate(NodeRef nodeRef, boolean newContent) {
		logger.info("called");
		try{
			NodeRef parentRef = nodeService.getPrimaryParent(nodeRef).getParentRef();
			if(nodeService.getType(parentRef).equals(QName.createQName(CCConstants.CCM_TYPE_IO))){
				//remove from cache so that the gui gets the mimetyp and preview
				new RepositoryCache().remove(parentRef.getId());
			}
		}catch(Throwable e){
			logger.error(e.getMessage(),e);
		}
	}
	
	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}
	
	public void setPolicyComponent(PolicyComponent policyComponent) {
		this.policyComponent = policyComponent;
	}
}
