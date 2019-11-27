package org.edu_sharing.service.repoproxy;

import java.util.HashMap;

import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;

public abstract class RepoProxyAbstract implements RepoProxy {

	public boolean myTurn(String repoId) {
		
		String homeRepo = ApplicationInfoList.getHomeRepository().getAppId();
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
	public HashMap<String, String> remoteAuth(ApplicationInfo repoInfo, boolean validate) throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}
}
