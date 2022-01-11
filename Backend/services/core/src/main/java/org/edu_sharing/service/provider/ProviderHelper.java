package org.edu_sharing.service.provider;

import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.monitoring.Application;
import org.edu_sharing.repository.server.tools.ApplicationInfo;

public class ProviderHelper {
    private static Logger logger = Logger.getLogger(ProviderHelper.class);
    public static boolean hasProvider(ApplicationInfo appInfo) {
        if(appInfo == null){
            logger.warn("hasProvider was called without an app info, remote repo might be removed");
            return false;
        }
        if(appInfo.ishomeNode()
                && (appInfo.getString(ApplicationInfo.KEY_REMOTE_PROVIDER,null) == null
                    || appInfo.getString(ApplicationInfo.KEY_REMOTE_PROVIDER,null).isEmpty())
                || appInfo.getRepositoryType().equals(ApplicationInfo.REPOSITORY_TYPE_LOCAL)) {
            return false;
        }
        return appInfo.getType().equals(ApplicationInfo.TYPE_REPOSITORY);
    }
    public static Provider getProviderByApp(ApplicationInfo appInfo){
        Class clazz = null;
        try {
            clazz = Class.forName(appInfo.getString(ApplicationInfo.KEY_REMOTE_PROVIDER,null));
            return (Provider) clazz.getConstructor(new Class[] { String.class}).newInstance(new Object[] { appInfo.getAppId() });
        } catch (Throwable t) {
            throw new RuntimeException("Error while loading the provider for appId "+appInfo.getAppId()+". Please make sure that the remote class is configured in the key "+ApplicationInfo.KEY_REMOTE_PROVIDER, t);
        }
    }
}
