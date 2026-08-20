package org.edu_sharing.repository.server.appcontext;

import lombok.RequiredArgsConstructor;
import org.edu_sharing.repository.server.tools.ApplicationInfo;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default {@link AppContextServiceFactory} implementation.
 * <p>
 * Resolving the implementation for an app is only half of the job: the resolved beans are
 * shared singletons which read the app they are working for from the
 * {@link ApplicationInfoContextHolder} (api key, remote url, app properties, ...).
 * Therefore {@link #getService(String)} and {@link #getLocalService()} do not hand out the
 * bean itself but a proxy which binds the requested {@code ApplicationInfo} to the current
 * thread for the duration of every single method call. Without that binding the service
 * falls back to whatever is left in the thread local - in most cases the home repository -
 * which is wrong for every remote repository.
 * <p>
 * {@link #getService()} intentionally stays unbound: it means "the service of the app which
 * is currently in context" and therefore must not override that context.
 */
@RequiredArgsConstructor
public class SimpleAppContextServiceFactoryImpl<T> implements AppContextServiceFactory<T> {

    private final AppContextServiceLocator locator;
    private final Class<T> type;

    /**
     * cache of the bound proxies by app id. The {@code ApplicationInfo} itself is resolved on
     * every invocation, so the cached proxies stay valid when the app info cache is refreshed.
     */
    private final Map<String, T> boundServices = new ConcurrentHashMap<>();

    @Override
    public T getService(String appId) {
        // fail fast on unknown app ids, as the locator did before
        ApplicationInfo resolved = locator.resolveApplicationInfo(appId);
        if(resolved.ishomeNode()) {
            // skip invocation for local service and directly resolve the local app id
            return getLocalService();
        }
        return boundServices.computeIfAbsent(appId, this::bind);
    }

    @Override
    public T getLocalService() {
        return locator.getLocal(type);
    }

    @Override
    public T getService() {
        return locator.get(type);
    }

    /**
     * creates a proxy which runs every call of the resolved service within the
     * {@code ApplicationInfo} of the given app id
     */
    private T bind(String appId) {
        if (!type.isInterface()) {
            // no proxy possible, the caller has to take care of the context on its own
            return locator.get(type, appId);
        }

        InvocationHandler handler = (p, method, args) -> {
            ApplicationInfo appInfo = locator.resolveApplicationInfo(appId);
            T target = locator.get(type, appInfo);
            try (AppContextScope.UseApplicationInfo ignored = AppContextScope.useApplicationInfo(appInfo)) {
                return method.invoke(target, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        };

        //noinspection unchecked
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }
}
