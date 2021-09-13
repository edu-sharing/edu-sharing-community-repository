package org.edu_sharing.repository.server.tools.cache;

import org.alfresco.repo.cache.SimpleCache;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.rpc.User;

public class UserCache {
    static final SimpleCache<String, User> userCache =
            (SimpleCache<String, User>) AlfAppContextGate.getApplicationContext().getBean("eduSharingUserCache");

    public static final User get(String username){
        return userCache.get(username);
    }

    public static final void put(String username, User user){
        userCache.put(username,user);
    }

    public static final void refresh(String username){
        userCache.remove(username);
    }
}
