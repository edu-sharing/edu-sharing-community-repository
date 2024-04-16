package org.edu_sharing.repository.update;

import java.util.List;


import lombok.extern.slf4j.Slf4j;
import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.QName;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.repository.server.update.UpdateRoutine;
import org.edu_sharing.repository.server.update.UpdateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

@Slf4j
@UpdateService
public class FolderToMap {

	private final NodeService nodeService;
	private final PermissionService permissionService;

	int counter = 0;


	//very important: use the alfrescoDefaultDbNodeService defined in custom-core-services-context.xml
	//cause of overwriten getChild... methods in org.edu_sharing.alfresco.fixes.DbNodeServiceImpl
	//this can lead to a problem, that every edugroupfolder is processed for all members of the edugroup again
	@Autowired
	public FolderToMap(@Qualifier("alfrescoDefaultDbNodeService") NodeService nodeService, PermissionService permissionService) {
		this.nodeService = nodeService;
		this.permissionService = permissionService;
	}

	@UpdateRoutine(
			id = "Release_1_8_FolderToMapBugfix",
			description = "folder created over webdav where not transformed to map. this is fixed. use this updater to transform folders starting from a specified root folder.",
			order = 1802
	)
	public void execute(boolean test) {
		counter = 0;
		String rootNode = Context.getCurrentInstance().getRequest().getParameter("root");

		if (rootNode == null || rootNode.trim().equals("")) {
			String message = "Missing parameter \"root\"";
			RuntimeException e = new RuntimeException(message);
			log.error(message, e);
			return;
		}

		NodeRef nodeRef = new NodeRef(MCAlfrescoAPIClient.storeRef, rootNode);

		if (!nodeService.exists(nodeRef)) {
			String message = "node with id " + rootNode + " does not exsist!";
			RuntimeException e = new RuntimeException(message);
			log.error(message, e);
			return;
		}

		transformLevel(nodeRef, test);
		log.info("finished. transformed folders:" + counter);
	}

	private void transformLevel(NodeRef parent,boolean test){

		List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(parent);
		for(ChildAssociationRef childAssoc : childAssocs){

			//transform current folder
			if(nodeService.getType(childAssoc.getChildRef()).equals(ContentModel.TYPE_FOLDER)){

				String currentName = (String)nodeService.getProperty(childAssoc.getChildRef(), ContentModel.PROP_NAME);

				log.info("transform folder to map. Path:"+nodeService.getPath(childAssoc.getChildRef()).toDisplayPath(nodeService, permissionService) +" n:"+currentName);
				if(!test){
					nodeService.setType(childAssoc.getChildRef(), QName.createQName(CCConstants.CCM_TYPE_MAP));
					nodeService.setProperty(childAssoc.getChildRef(), ContentModel.PROP_TITLE, currentName);
				}
				counter++;
			}

			//transform current children
			if(nodeService.getType(childAssoc.getChildRef()).equals(QName.createQName(CCConstants.CCM_TYPE_MAP)) ){
				transformLevel(childAssoc.getChildRef(), test);
			}

		}
	}
}
