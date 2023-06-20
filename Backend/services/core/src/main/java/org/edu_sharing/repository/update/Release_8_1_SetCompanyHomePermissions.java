package org.edu_sharing.repository.update;

import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.model.Repository;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.PermissionService;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.UserEnvironmentToolFactory;
import org.edu_sharing.repository.server.update.UpdateRoutine;
import org.edu_sharing.repository.server.update.UpdateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.List;

@Slf4j
@UpdateService
public class Release_8_1_SetCompanyHomePermissions {
	private final NodeService nodeService;
	private final Repository repositoryHelper;
	private final PermissionService permissionService;
	private final UserEnvironmentToolFactory userEnvironmentToolFactory;


	@Autowired
	public Release_8_1_SetCompanyHomePermissions(NodeService nodeService, @Qualifier("repositoryHelper") Repository repositoryHelper, PermissionService permissionService, UserEnvironmentToolFactory userEnvironmentToolFactory) {
		this.nodeService = nodeService;
		this.repositoryHelper = repositoryHelper;
		this.permissionService = permissionService;
		this.userEnvironmentToolFactory = userEnvironmentToolFactory;
	}

	@UpdateRoutine(
			id = "Release_8_1_SetCompanyHomePermissions",
			description = "remove GROUP_EVERYONE from the Company-Home folder if this is the first time edu-sharing install",
			order = 0,
			auto = true)
	public void execute() {
		try {
			// TODO
			String eduSystemFolderUpdate = userEnvironmentToolFactory.createUserEnvironmentTool().getEdu_SharingSystemFolderUpdate();
			List<ChildAssociationRef> assoc = nodeService.getChildAssocs(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, eduSystemFolderUpdate));
			if(assoc.size() > 0) {
				log.info("edu-sharing is already installed, will not update permissions. Please check the company home folder permissions manually");
			} else {
				log.info("edu-sharing is not installed. Will remove GROUP_EVERYONE from the company home folder");
				permissionService.deletePermission(
						repositoryHelper.getCompanyHome(),
						CCConstants.AUTHORITY_GROUP_EVERYONE, CCConstants.PERMISSION_READ
				);
			}
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}
}
