package org.edu_sharing.repository.update;

import org.alfresco.repo.model.Repository;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.UserEnvironmentTool;

import java.io.PrintWriter;
import java.util.List;

public class Release_8_1_SetCompanyHomePermissions extends UpdateAbstract {

	public static final String ID = "Release_8_1_SetCompanyHomePermissions";

	public static final String description = "remove GROUP_EVERYONE from the Company-Home folder if this is the first time edu-sharing install";

	NodeService nodeService = serviceRegistry.getNodeService();

	public Release_8_1_SetCompanyHomePermissions(PrintWriter out) {
		this.out = out;
		this.logger = Logger.getLogger(Release_8_1_SetCompanyHomePermissions.class);
	}

	@Override
	public void execute() {
		this.executeWithProtocolEntryNoGlobalTx();
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public void run() throws Throwable {

	}

	@Override
	public boolean runAndReport() {
		return doTask();
	}


	public boolean doTask(){
		try {
			String eduSystemFolderUpdate = new UserEnvironmentTool().getEdu_SharingSystemFolderUpdate();
			List<ChildAssociationRef> assoc = nodeService.getChildAssocs(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, eduSystemFolderUpdate));
			if(assoc.size() > 0) {
				logInfo("edu-sharing is already installed, will not update permissions. Please check the company home folder permissions manually");
			} else {
				logInfo("edu-sharing is not installed. Will remove GROUP_EVERYONE from the company home folder");
				Repository repositoryHelper = (Repository) applicationContext.getBean("repositoryHelper");
				serviceRegistry.getPermissionService().deletePermission(
						repositoryHelper.getCompanyHome(),
						CCConstants.AUTHORITY_GROUP_EVERYONE, CCConstants.PERMISSION_CONSUMER
				);
			}
			return true;
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void test() {
		logInfo("not implemented");

	}

}
