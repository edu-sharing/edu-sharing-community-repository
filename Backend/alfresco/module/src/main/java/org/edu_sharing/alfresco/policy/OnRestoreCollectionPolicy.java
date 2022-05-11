package org.edu_sharing.alfresco.policy;

import org.alfresco.repo.node.NodeServicePolicies.OnRestoreNodePolicy;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.tools.UsageTool;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;

public class OnRestoreCollectionPolicy implements OnRestoreNodePolicy {
	
	PolicyComponent policyComponent;
	
	NodeService nodeService;
	
	Logger logger = Logger.getLogger(OnRestoreCollectionPolicy.class);

	VersionService versionService = null;
	
	public void init() {
		policyComponent.bindClassBehaviour(OnRestoreNodePolicy.QNAME, QName.createQName(CCConstants.CCM_TYPE_MAP), new JavaBehaviour(this, "onRestoreNode"));
		policyComponent.bindClassBehaviour(OnRestoreNodePolicy.QNAME, QName.createQName(CCConstants.CCM_TYPE_IO), new JavaBehaviour(this, "onRestoreNode"));
	}

	@Override
	public void onRestoreNode(ChildAssociationRef childAssocRef) {
		handleRef(childAssocRef);
	}

	private void handleRef(ChildAssociationRef childAssocRef) {
		if(nodeService.hasAspect(childAssocRef.getChildRef(),QName.createQName(CCConstants.CCM_ASPECT_COLLECTION_IO_REFERENCE))){
			setCollectionRefUsage(childAssocRef.getChildRef());
		}
		// map
		if(nodeService.getType(childAssocRef.getChildRef()).equals(QName.createQName(CCConstants.CCM_TYPE_MAP)) && nodeService.hasAspect(childAssocRef.getChildRef(),QName.createQName(CCConstants.CCM_ASPECT_COLLECTION))){
			// recursive
			for(ChildAssociationRef child : nodeService.getChildAssocs(childAssocRef.getChildRef())){
				handleRef(child);
			}
		}
	}

	private void setCollectionRefUsage(NodeRef nodeRef) {
		logger.info("resetting usage of collection ref after restoring: "+nodeRef.getId());
		try {
			new UsageTool().createUsage(ApplicationInfoList.getHomeRepository().getAppId(),
					nodeService.getPrimaryParent(nodeRef).getParentRef().getId(),
					(String)nodeService.getProperty(nodeRef,QName.createQName(CCConstants.CCM_PROP_IO_ORIGINAL)),
					nodeRef.getId()
					);
		} catch (Exception e) {
			logger.warn("failed to set ref usage",e);
		}

	}


	public void setPolicyComponent(PolicyComponent policyComponent) {
		this.policyComponent = policyComponent;
	}
	
	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}
	
	public void setVersionService(VersionService versionService) {
		this.versionService = versionService;
	}
}
