package org.edu_sharing.alfresco.policy;

import java.io.Serializable;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.model.ForumModel;
import org.alfresco.repo.action.ActionModel;
import org.alfresco.repo.copy.CopyBehaviourCallback;
import org.alfresco.repo.copy.CopyDetails;
import org.alfresco.repo.copy.CopyServicePolicies;
import org.alfresco.repo.copy.CopyServicePolicies.OnCopyNodePolicy;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.copy.DefaultCopyBehaviourCallback;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;

public class OnCopyCollectionRefPolicy implements OnCopyNodePolicy,CopyServicePolicies.OnCopyCompletePolicy {
	
	
	PolicyComponent policyComponent;
	
	NodeService nodeService;
	
	Logger logger = Logger.getLogger(OnCopyCollectionRefPolicy.class);
	
	public void init(){
		this.policyComponent.bindClassBehaviour(QName.createQName(NamespaceService.ALFRESCO_URI, "getCopyCallback"),
               QName.createQName(CCConstants.CCM_TYPE_IO), new JavaBehaviour(this, "getCopyCallback"));
		this.policyComponent.bindClassBehaviour(QName.createQName(NamespaceService.ALFRESCO_URI, "onCopyComplete"),
				QName.createQName(CCConstants.CCM_TYPE_IO), new JavaBehaviour(this, "onCopyComplete"));
	}

	@Override
	public void onCopyComplete(QName classRef, NodeRef sourceNodeRef, NodeRef targetNodeRef, boolean copyToNewNode, Map<NodeRef,NodeRef> copyMap){
		if(isCollectionReference(targetNodeRef,nodeService.getPrimaryParent(targetNodeRef).getParentRef())){
			nodeService.addAspect(targetNodeRef,QName.createQName(CCConstants.CCM_ASPECT_COLLECTION_IO_REFERENCE),null);
			logger.info("will add property link to original io");
			nodeService.setProperty(targetNodeRef,QName.createQName(CCConstants.CCM_PROP_IO_ORIGINAL),sourceNodeRef.getId());
		}else{
			logger.info("setting ccm:original for copied node to itself:"+targetNodeRef.getId());
			nodeService.setProperty(targetNodeRef,QName.createQName(CCConstants.CCM_PROP_IO_ORIGINAL),targetNodeRef.getId());
		}
	}

	@Override
	public CopyBehaviourCallback getCopyCallback(QName classRef, CopyDetails copyDetails) {

		if(isCollectionReference(copyDetails.getSourceNodeRef(), copyDetails.getTargetParentNodeRef())){
			return new DefaultCopyBehaviourCallback(){


				@Override
				public Map<QName, Serializable> getCopyProperties(QName classQName, CopyDetails copyDetails,
						Map<QName, Serializable> properties) {
					properties=super.getCopyProperties(classQName, copyDetails, properties);
					logger.info("will remove content from properties");
					properties.remove(ContentModel.PROP_CONTENT);
					return properties;
				}
				
				
			};
		}
		
		return new DefaultCopyBehaviourCallback();
	}

	private boolean isCollectionReference(NodeRef sourceRef,NodeRef targetParentRef) {
		boolean isCollectionReference=nodeService.hasAspect(targetParentRef,QName.createQName(CCConstants.CCM_ASPECT_COLLECTION));
		if(!isCollectionReference) {
			try {
				isCollectionReference = nodeService.hasAspect(sourceRef,QName.createQName(CCConstants.CCM_ASPECT_IO_CHILDOBJECT))
						&& nodeService.getAspects(nodeService.getPrimaryParent(targetParentRef).getParentRef()).contains(QName.createQName(CCConstants.CCM_ASPECT_COLLECTION));
			}
			catch(Throwable t){
				logger.info(t);
			}
		}
		return isCollectionReference;
	}


	public void setPolicyComponent(PolicyComponent policyComponent) {
		this.policyComponent = policyComponent;
	}
	
	
	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}
	

}
