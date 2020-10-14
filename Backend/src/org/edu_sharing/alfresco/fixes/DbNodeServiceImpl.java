package org.edu_sharing.alfresco.fixes;

import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.QNamePattern;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;

public class DbNodeServiceImpl extends org.alfresco.repo.node.db.DbNodeServiceImpl {

	
	
	
	ServiceRegistry serviceRegistry;
	
	Logger logger = Logger.getLogger(DbNodeServiceImpl.class);
	
	@Override
	public List<ChildAssociationRef> getChildAssocs(NodeRef nodeRef, QNamePattern typeQNamePattern, QNamePattern qnamePattern,
			boolean preload) {

		List<ChildAssociationRef> result = new VirtualEduGroupFolderTool(serviceRegistry,this).getGroupMapChildren(nodeRef);
		if(result != null){
			return result;
		}
		
		
		return super.getChildAssocs(nodeRef, typeQNamePattern, qnamePattern, preload);
	}
	
	@Override
	public List<ChildAssociationRef> getChildAssocs(NodeRef nodeRef, java.util.Set<QName> childNodeTypeQNames) {
		// check if the maps are allowed to be displayed
		if(childNodeTypeQNames==null || childNodeTypeQNames.contains(QName.createQName(CCConstants.CCM_TYPE_MAP))) {
			List<ChildAssociationRef> result = new VirtualEduGroupFolderTool(serviceRegistry, this).getGroupMapChildren(nodeRef);
			if (result != null) {
				return result;
			}
		}
		
		return super.getChildAssocs(nodeRef, childNodeTypeQNames);
	};

	@Override
	public NodeRef getChildByName(NodeRef nodeRef, QName assocTypeQName, String childName) {
		NodeRef result =  super.getChildByName(nodeRef, assocTypeQName, childName);
		if(result == null){
			if(ContentModel.ASSOC_CONTAINS.equals(assocTypeQName) && QName.createQName(CCConstants.CCM_TYPE_MAP).equals(this.getType(nodeRef))){
				String mapType = (String)this.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_MAP_TYPE));
				if(mapType != null && mapType.equals(CCConstants.CCM_VALUE_MAP_TYPE_EDUGROUP)){
					logger.info("an edugroup folder getting virtual children");
					List<ChildAssociationRef> eduGroupFolders = new VirtualEduGroupFolderTool(serviceRegistry,this).getGroupMapChildren(nodeRef);
					for(ChildAssociationRef childRef : eduGroupFolders){
						String name = (String)this.getProperty(childRef.getChildRef(), ContentModel.PROP_NAME);
						
						/**
						 * @Todo what is if there are two group folders with the same name
						 */
						if(childName != null && childName.equals(name)){
							return childRef.getChildRef();
						}
					}
				}
			}
		}
		
		return result;
	}



	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}
	
}
