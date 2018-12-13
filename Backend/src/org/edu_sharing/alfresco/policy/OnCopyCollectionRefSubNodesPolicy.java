package org.edu_sharing.alfresco.policy;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.copy.CopyBehaviourCallback;
import org.alfresco.repo.copy.CopyDetails;
import org.alfresco.repo.copy.DefaultCopyBehaviourCallback;
import org.alfresco.repo.copy.CopyBehaviourCallback.ChildAssocCopyAction;
import org.alfresco.repo.copy.CopyBehaviourCallback.ChildAssocRecurseAction;
import org.alfresco.repo.copy.CopyBehaviourCallback.CopyChildAssociationDetails;
import org.alfresco.repo.copy.CopyServicePolicies.OnCopyNodePolicy;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;

public class OnCopyCollectionRefSubNodesPolicy  implements OnCopyNodePolicy{

	
	PolicyComponent policyComponent;
	
	NodeService nodeService;
	
	Logger logger = Logger.getLogger(OnCopyCollectionRefSubNodesPolicy.class);
	
	public void init(){
		this.policyComponent.bindClassBehaviour(QName.createQName(NamespaceService.ALFRESCO_URI, "getCopyCallback"),
               QName.createQName(CCConstants.CCM_TYPE_USAGE), new JavaBehaviour(this, "getCopyCallback"));

		this.policyComponent.bindClassBehaviour(QName.createQName(NamespaceService.ALFRESCO_URI, "getCopyCallback"),
				QName.createQName(CCConstants.CCM_TYPE_SHARE), new JavaBehaviour(this, "getCopyCallback"));

		this.policyComponent.bindClassBehaviour(QName.createQName(NamespaceService.ALFRESCO_URI, "getCopyCallback"),
	               QName.createQName(CCConstants.CCM_TYPE_ASSIGNED_LICENSE), new JavaBehaviour(this, "getCopyCallback"));
	}
	
	@Override
	public CopyBehaviourCallback getCopyCallback(QName classRef, CopyDetails copyDetails) {

		logger.info("classRef:"+classRef+ " "+copyDetails.getSourceNodeRef()+" "+nodeService.getAspects(copyDetails.getTargetParentNodeRef()).contains(QName.createQName(CCConstants.CCM_ASPECT_COLLECTION)));
		
		//check if the IO's container is an Collection
		
			return new DefaultCopyBehaviourCallback(){	
				@Override
				public boolean getMustCopy(QName classQName, CopyDetails copyDetails) {

					if(nodeService.getAspects(nodeService.getPrimaryParent(copyDetails.getTargetParentNodeRef()).getParentRef()).contains(QName.createQName(CCConstants.CCM_ASPECT_COLLECTION))){
							logger.info("getMustCopy returns false");
							return false;
					}
			
					return super.getMustCopy(classQName, copyDetails);
				}
			};
		
	}
	
	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}
	
	public void setPolicyComponent(PolicyComponent policyComponent) {
		this.policyComponent = policyComponent;
	}
}
