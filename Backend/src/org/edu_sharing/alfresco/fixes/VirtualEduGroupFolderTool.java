package org.edu_sharing.alfresco.fixes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.OwnableService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.cache.EduGroupCache;

public class VirtualEduGroupFolderTool {

	
	Logger logger = Logger.getLogger(VirtualEduGroupFolderTool.class);
	
	ServiceRegistry serviceRegistry;
	NodeService nodeService;
	public VirtualEduGroupFolderTool(ServiceRegistry serviceRegistry, NodeService nodeService) {
		this.serviceRegistry = serviceRegistry;
		this.nodeService = nodeService;
	}
	
	public List<ChildAssociationRef> getGroupMapChildren(NodeRef nodeRef){
		if(QName.createQName(CCConstants.CCM_TYPE_MAP).equals(nodeService.getType(nodeRef))){
			String mapType = (String)nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_MAP_TYPE));
			if(mapType != null && mapType.equals(CCConstants.CCM_VALUE_MAP_TYPE_EDUGROUP)){
				logger.debug("its a map with typ edugroup");
				OwnableService ownableService = (OwnableService)AlfAppContextGate.getApplicationContext().getBean("ownableService");
				String user = ownableService.getOwner(nodeRef);

				Set<String> authorities = serviceRegistry.getAuthorityService().getContainingAuthorities(AuthorityType.GROUP, user, true);
				
				Collection<NodeRef> eduGroupNodeRefs = getEduGroupNodeRefs();
				
				List<ChildAssociationRef> children = new ArrayList<ChildAssociationRef>();
				for(NodeRef eduGroupNodeRef : eduGroupNodeRefs){
				
					NodeRef eduGroupHomeDir = (NodeRef)nodeService.getProperty(eduGroupNodeRef, QName.createQName(CCConstants.CCM_PROP_EDUGROUP_EDU_HOMEDIR));
					
					String groupName = (String)nodeService.getProperty(eduGroupNodeRef, ContentModel.PROP_AUTHORITY_NAME);
					if(eduGroupHomeDir != null && nodeService.exists(eduGroupHomeDir) && authorities.contains(groupName)){
						String eduGroupHomeDirName = (String)nodeService.getProperty(eduGroupHomeDir,ContentModel.PROP_NAME);
						children.add(new ChildAssociationRef(ContentModel.ASSOC_CONTAINS,nodeRef,QName.createQName(eduGroupHomeDirName),eduGroupHomeDir));
					}
				}
				return children;
			}
		}
		return null;
	}
	
	
	public Collection<NodeRef> getEduGroupNodeRefs(){
		return EduGroupCache.getKeys();	
	}
	
}
