package org.edu_sharing.service.relations;

import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.provider.ProviderHelper;
import org.edu_sharing.service.rating.RatingService;
import org.edu_sharing.spring.ApplicationContextFactory;

public class RelationServiceFactory {
    public static RelationService getRelationService(String appId) {
        ApplicationInfo appInfo = (appId == null) ? ApplicationInfoList.getHomeRepository() : ApplicationInfoList.getRepositoryInfoById(appId);
        if(!ProviderHelper.hasProvider(appInfo)){
            return getLocalService();

        }else{
            return ProviderHelper.getProviderByApp(appInfo).getRelationService();
        }
    }

    public static RelationService getLocalService(){
        return ApplicationContextFactory.getApplicationContext().getBean(RelationService.class);
    }
}
