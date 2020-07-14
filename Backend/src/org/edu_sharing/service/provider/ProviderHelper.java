package org.edu_sharing.service.provider;

import org.edu_sharing.alfresco.monitoring.Application;
import org.edu_sharing.repository.server.tools.ApplicationInfo;

public class ProviderHelper {
    public static boolean hasProvider(ApplicationInfo appInfo) {
        if(appInfo.ishomeNode()
                && appInfo.getString(ApplicationInfo.KEY_REMOTE_PROVIDER,null) == null) {
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
