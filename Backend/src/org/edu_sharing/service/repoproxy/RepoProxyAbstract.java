package org.edu_sharing.service.repoproxy;

import java.util.HashMap;

import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.restservices.RepositoryDao;
import org.edu_sharing.restservices.RestConstants;
import org.edu_sharing.service.toolpermission.ToolPermissionHelper;
import org.edu_sharing.service.toolpermission.ToolPermissionService;

public abstract class RepoProxyAbstract implements RepoProxy {

	public boolean myTurn(String repoId) {
		String homeRepo = ApplicationInfoList.getHomeRepository().getAppId();
		if(repoId.equals(RepositoryDao.HOME)) {
			repoId = homeRepo;
		}
		// validate if user is allowed to access data from this repo
		ToolPermissionHelper.throwIfToolpermissionMissing(CCConstants.CCM_VALUE_TOOLPERMISSION_REPOSITORY_PREFIX+repoId);

		if (homeRepo.equals(repoId)) {
			return false;
		}
		ApplicationInfo repoInfo = ApplicationInfoList.getRepositoryInfoById(repoId);

		if (repoInfo != null && repoInfo.getRepositoryType().equals(ApplicationInfo.REPOSITORY_TYPE_ALFRESCO)) {
			return true;
		}
		
		return false;
	}
	
	@Override
	public HashMap<String, String> remoteAuth(ApplicationInfo repoInfo, String username, boolean validate) throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}
}
