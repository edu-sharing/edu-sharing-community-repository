package org.edu_sharing.alfresco.policy;

import java.io.Serializable;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.model.ForumModel;
import org.alfresco.repo.action.ActionModel;
import org.alfresco.repo.copy.CopyBehaviourCallback;
import org.alfresco.repo.copy.CopyDetails;
import org.alfresco.repo.copy.CopyServicePolicies.OnCopyNodePolicy;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.copy.DefaultCopyBehaviourCallback;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;

public class OnCopyCollectionRefPolicy implements OnCopyNodePolicy{
	
	
	PolicyComponent policyComponent;
	
	NodeService nodeService;
	
	Logger logger = Logger.getLogger(OnCopyCollectionRefPolicy.class);
	
	public void init(){
		this.policyComponent.bindClassBehaviour(QName.createQName(NamespaceService.ALFRESCO_URI, "getCopyCallback"),
               QName.createQName(CCConstants.CCM_TYPE_IO), new JavaBehaviour(this, "getCopyCallback"));
	}
	
	@Override
	public CopyBehaviourCallback getCopyCallback(QName classRef, CopyDetails copyDetails) {
		
		
		
		logger.info("classRef:"+classRef+ " "+copyDetails.getSourceNodeRef()+" "+nodeService.getAspects(copyDetails.getTargetParentNodeRef()).contains(QName.createQName(CCConstants.CCM_ASPECT_COLLECTION)));
		
		
		
		if(nodeService.getAspects(copyDetails.getTargetParentNodeRef()).contains(QName.createQName(CCConstants.CCM_ASPECT_COLLECTION))){
			return new DefaultCopyBehaviourCallback(){
				@Override
				public Map<QName, Serializable> getCopyProperties(QName classQName, CopyDetails copyDetails,
						Map<QName, Serializable> properties) {
	
					logger.info("will remove content from properties");
					if(properties.containsKey(ContentModel.PROP_CONTENT)){
						properties.remove(ContentModel.PROP_CONTENT);
					}
					
					
					return super.getCopyProperties(classQName, copyDetails, properties);
				}
				
				
			};
		}
		
		return new DefaultCopyBehaviourCallback();
	}
	
	
	public void setPolicyComponent(PolicyComponent policyComponent) {
		this.policyComponent = policyComponent;
	}
	
	
	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}
	

}
