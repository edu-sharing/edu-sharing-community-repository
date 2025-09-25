package org.edu_sharing.repository.server.appcontext;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SimpleAppContextServiceFactoryImpl<T> implements AppContextServiceFactory<T> {

    private final AppContextServiceLocator locator;
    private final Class<T> type;

    @Override
    public T getService(String appId) {
        return locator.get(type, appId);
    }

    @Override
    public T getLocalService() {
        return locator.getLocal(type);
    }

    @Override
    public T getService() {
        return locator.get(type);
    }
}
