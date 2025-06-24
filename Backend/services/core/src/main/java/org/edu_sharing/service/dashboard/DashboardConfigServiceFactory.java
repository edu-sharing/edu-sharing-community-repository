package org.edu_sharing.service.dashboard;

import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.spring.ApplicationContextFactory;

public class DashboardConfigServiceFactory {
    public static DashboardConfigService getDashboardConfigService(String applicationId){

        if(!applicationId.equals(ApplicationInfoList.getHomeRepository().getAppId()) &&
                !ApplicationInfoList.getRepositoryInfoById(applicationId).getRepositoryType().equals(ApplicationInfo.REPOSITORY_TYPE_LOCAL)){
            throw new RuntimeException("no remote version of AuthorityService implemented yet");
        }

        return getLocalService();
    }
    public static DashboardConfigService getLocalService(){
        return ApplicationContextFactory.getApplicationContext().getBean(DashboardConfigService.class);
    }
}
