package org.edu_sharing.service.authentication.totp;

import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;

public class OneTimeTokenServiceFactory {

    public static OneTimeTokenService getOneTimeTokenServiceService(String applicationId) {

        if (!applicationId.equals(ApplicationInfoList.getHomeRepository().getAppId()) && !ApplicationInfoList.getRepositoryInfoById(applicationId).getRepositoryType().equals(ApplicationInfo.REPOSITORY_TYPE_LOCAL)) {
            throw new RuntimeException("no remote version of OneTimeTokenService implemented yet");
        }

        return getLocalService();
    }

    public static OneTimeTokenService getLocalService() {
        return org.edu_sharing.spring.ApplicationContextFactory.getApplicationContext().getBean(OneTimeTokenService.class);
    }
}
