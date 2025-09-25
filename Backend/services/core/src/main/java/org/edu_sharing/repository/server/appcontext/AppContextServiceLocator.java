package org.edu_sharing.repository.server.appcontext;

import com.drew.lang.annotations.NotNull;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.spring.ApplicationContextFactory;
import org.edu_sharing.spring.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service locator responsible for resolving and retrieving beans based on application context.
 * <p>
 * The {@code AppContextServiceLocator} class allows for dynamic resolution of beans based on their
 * type and the application context ID. This enables context-aware dependency injection for
 * multi-application or multi-tenant setups.
 * <p>
 * Key responsibilities:
 * - Caches resolved beans to optimize lookup performance.
 * - Supports resolving beans using different priority strategies such as:
 * 1. AppContextRegistry overrides by bean names.
 * 2. LocalContextRegistry overrides by bean names.
 * 3. Annotations such as {@code @AppContext} and {@code @LocalAppContext}.
 * - Ensures that the appropriate bean is selected based on the current application context.
 * <p>
 * Primary methods include:
 * - {@link #get(Class)}: Resolves a bean for the current application context.
 * - {@link #get(Class, String)}: Resolves a bean for a specific application context ID.
 * <p>
 * This class integrates with the {@link ApplicationInfoContextHolder} to determine the current
 * application context and collaborates with {@link AppContextRegistry} for context definitions and overrides.
 * <p>
 * Dependencies:
 * - {@link ListableBeanFactory} for retrieving Spring beans.
 * - {@link AppContextRegistry} for managing context definitions and overrides.
 */
@Component
@RequiredArgsConstructor
public class AppContextServiceLocator {

    private final ListableBeanFactory beanFactory;
    private final AppContextRegistry appContextRegistry;

    private final Map<Key, Object> cache = new ConcurrentHashMap<>();
    private final ApplicationInfo localApp = ApplicationInfoList.getHomeRepository();

    private final Map<ApplicationInfo, String> remoteProviderCache = new ConcurrentHashMap<>();


    @EventListener(RefreshScopeRefreshedEvent.class)
    public void onRefreshScopeRefreshed() {
        remoteProviderCache.clear();
    }

    /**
     * Retrieves the single instance of {@code AppContextServiceLocator}.
     * <p>
     * This method is deprecated and relies on the {@code ApplicationContextFactory} to fetch the bean of type
     * {@code AppContextServiceLocator} using Spring's application context.
     *
     * @return the singleton instance of {@code AppContextServiceLocator} from the application context
     * @deprecated This method is deprecated. Use alternative mechanisms to access service locators or consider
     * dependency injection (DI) for better maintainability and testability.
     */
    @Deprecated
    public static AppContextServiceLocator getInstance() {
        return ApplicationContextFactory.getApplicationContext().getBean(AppContextServiceLocator.class);
    }

    /**
     * Retrieves a bean instance of the specified type from the application context associated
     * with the current {@code ApplicationInfo}.
     *
     * @param type the class type of the bean to be retrieved; must not be null
     * @param <T>  the generic type of the bean to be retrieved
     * @return the instance of the specified type retrieved from the application context; never null
     * @throws IllegalArgumentException if no {@code ApplicationInfo} is present in the context
     */
    @NotNull
    public <T> T get(@NotNull Class<T> type) {
        String providerName = ProviderNameContextHolder.getProviderName();
        if (providerName != null) {
            return getByProvider(type, providerName);
        }

        ApplicationInfo currentAppInfo = ApplicationInfoContextHolder.getCurrentApplicationInfo();
        Assert.notNull(currentAppInfo, "No contextName found in ApplicationInfoContext");
        return get(type, currentAppInfo);
    }

    /**
     * Retrieves an instance of the specified type from the local application context.
     * The method uses the provided type to resolve and return a corresponding
     * component or bean from the local context identified by `localAppId`.
     *
     * @param type the class type of the desired component; must not be null
     * @param <T>  the generic type of the component to be retrieved
     * @return the instance of the specified type from the local application context; never null
     * @throws IllegalArgumentException if the component of the specified type cannot be resolved
     */
    @NotNull
    public <T> T getLocal(@NotNull Class<T> type) {
        return get(type, localApp);
    }

    /**
     * Retrieves an instance of the specified type from the application context associated with the given application ID.
     * The method resolves the {@code ApplicationInfo} for the provided {@code appId} and uses it to fetch the desired
     * bean or component of the specified type.
     *
     * @param type  the class type of the desired component; must not be null
     * @param appId the application ID used to resolve the application context; must not be null
     * @param <T>   the generic type of the component to be retrieved
     * @return the instance of the specified type from the resolved application context; never null
     * @throws IllegalArgumentException if no {@code ApplicationInfo} is found for the specified {@code appId}
     */
    @NotNull
    public <T> T get(@NotNull @NonNull Class<T> type, @NotNull @NonNull String appId) {
        ApplicationInfo repositoryInfoById = "-home-".equals(appId)
                ? ApplicationInfoList.getHomeRepository()
                : ApplicationInfoList.getRepositoryInfoById(appId);

        if (repositoryInfoById == null) {
            throw new IllegalArgumentException("No repository found for contextName " + appId);
        }

        return get(type, repositoryInfoById);
    }

    /**
     * Retrieves an instance of the specified type from the application context associated
     * with the provided {@code ApplicationInfo}.
     * <p>
     * The method uses the application context identified by {@code appInfo} to resolve
     * and return a corresponding component or bean of the specified type. If the requested
     * component is not found in the cache, it will be resolved and cached for future use.
     *
     * @param type    the class type of the component to be retrieved; must not be null
     * @param appInfo the application information used to resolve the application context; must not be null
     * @param <T>     the generic type of the component to be retrieved
     * @return the instance of the specified type retrieved from the application context; never null
     * @throws IllegalArgumentException if the specified {@code ApplicationInfo} is invalid or the component cannot be resolved
     */
    @NotNull
    public <T> T get(@NotNull @NonNull Class<T> type, @NotNull @NonNull ApplicationInfo appInfo) {
        String providerName = resolveProviderName(appInfo);
        return getByProvider(type, providerName);
    }

    @SuppressWarnings("unchecked")
    private <T> T getByProvider(@NotNull @NonNull Class<T> type, @NotNull @NonNull String providerName) {
        Key key = new Key(type, providerName);
        return (T) cache.computeIfAbsent(key, k -> resolve(type, providerName));
    }

    private String resolveProviderName(ApplicationInfo appInfo) {
        return remoteProviderCache.computeIfAbsent(appInfo, k -> {
            if(!appInfo.getType().equals(ApplicationInfo.TYPE_REPOSITORY)){
                return "local";
            }

            String remoteProvider = appInfo.getString(ApplicationInfo.KEY_REMOTE_PROVIDER, null);
            if (!StringUtils.isBlank(remoteProvider)) {
                return remoteProvider;
            }

            if (appInfo.ishomeNode()) {
                return "local";
            }

            throw new IllegalStateException("No provider name found for contextName " + appInfo.getAppId() + ". Please make sure that the remote class is configured in the key" + ApplicationInfo.KEY_REMOTE_PROVIDER);
        });
    }

    private <T> T resolve(Class<T> type, String contextName) {

        // 1) registry override by annoation @AppContext(contextName)
        Map<String, T> candidates = beanFactory.getBeansOfType(type);
        for (T bean : candidates.values()) {
            AppContext ann = getAppContextAnnotation(bean, AppContext.class);
            if (ann != null && Arrays.asList(ann.value()).contains(contextName)) {
                return bean;
            }
        }

        // 2) local override by annoation @LocalAppContext
        for (T bean : candidates.values()) {
            LocalAppContext ann = getAppContextAnnotation(bean, LocalAppContext.class);
            if (ann != null) {
                return bean;
            }
        }

        // 3) local override by annoation @LocalAppContext
        for (T bean : candidates.values()) {
            FallbackAppContext ann = getAppContextAnnotation(bean, FallbackAppContext.class);
            if (ann != null && Arrays.asList(ann.value()).contains(contextName)) {
                return bean;
            }
        }


        // 4) registry override by bean name
        AppContextRegistry.ContextDefinition contextDef = appContextRegistry.getContexts(contextName);
        AppContextRegistry.BeanOverride<T> beanOverride = contextDef.resolveOverrideBean(type);
        if (beanOverride != null) {
            return beanOverride.getBean(beanFactory);
        }

        // 5) register override by bean name in fallback context
        AppContextRegistry.ContextDefinition fallbackContextDef = appContextRegistry.getFallbackContext();
        AppContextRegistry.BeanOverride<T> fallbackBeanOverride = fallbackContextDef.resolveOverrideBean(type);
        if (fallbackBeanOverride != null) {
            return fallbackBeanOverride.getBean(beanFactory);
        }


        // 6) fallback to local context if only one candidate is found in local context
        if (contextName.equals("local") && candidates.size() == 1) {
            return candidates.values().stream().findFirst().get();
        }

        throw new IllegalStateException("No bean found for type " + type + " and contextName " + contextName);
    }

    private <T extends java.lang.annotation.Annotation> T getAppContextAnnotation(Object bean, Class<T> annotationType) {
        Class<?> userClass = AopUtils.getTargetClass(bean);
        return userClass.getAnnotation(annotationType);
    }

    private record Key(Class<?> type, String providerName) {
    }

}
