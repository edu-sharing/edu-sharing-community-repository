package org.edu_sharing.repository.server.appcontext;

import lombok.Getter;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;

import java.util.function.Supplier;


/**
 * The AppContextScope class provides utility methods for managing the application context
 * and application information context within a scoped lifecycle, enabling temporary changes
 * to context values that are automatically reverted upon completion.
 */
public class AppContextScope {

    /**
     * Creates a new scoped application context using the provided context name.
     * The application context will temporarily switch to the specified context
     * name for the duration of the usage. The previous provider name is restored
     * automatically when the context is closed.
     *
     * @param contextName the name of the context to set for the application context
     * @return a {@code UseAppContext} instance representing the scoped application
     * context, which must be closed to restore the previous provider name
     */
    public static UseAppContext useAppContext(String contextName) {
        return new UseAppContext(contextName);
    }

    /**
     * Executes the provided supplier within the scope of a specific application context.
     * Temporarily switches the application context to the specified {@code contextName} and
     * restores the previous context after execution.
     *
     * @param <T>         The return type of the supplier.
     * @param contextName The name of the application context to activate during execution.
     * @param supplier    A supplier whose logic will be executed within the specified application context.
     * @return The result of the supplier's computation.
     */
    public static <T> T useAppContext(String contextName, Supplier<T> supplier) {
        try (UseAppContext ignored = AppContextScope.useAppContext(contextName)) {
            return supplier.get();
        }
    }


    /**
     * Creates a new scoped application context with the local context name.
     * The application context will temporarily switch to the local context for the duration of its usage.
     * The previous provider name is restored automatically when the context is closed.
     *
     * @return a {@code UseAppContext} instance representing the scoped application context
     * specific to the local context, which must be closed to restore the previous provider name.
     */
    public static UseAppContext useLocalAppContext() {
        return new UseAppContext("local");
    }

    /**
     * Executes the provided supplier within the scope of a local application context.
     * Temporarily switches the application context to the local context and restores
     * the previous provider name after execution.
     *
     * @param <T>      The return type of the supplier.
     * @param supplier A supplier whose logic will be executed within the scope of
     *                 the local application context.
     * @return The result of the supplier's computation.
     */
    public static <T> T useLocalAppContext(Supplier<T> supplier) {
        try (UseAppContext ignored = AppContextScope.useLocalAppContext()) {
            return supplier.get();
        }
    }

    /**
     * Creates a new scoped application context using the provided application ID.
     * The application context temporarily switches to the specified application
     * during its usage. The previous application context is restored automatically
     * when the context is closed.
     * <p>
     * This method will override the ApplicationInfo in the ApplicationInfoContextHolder.
     * Wherefore it has a deeper impact on the application context as the {@code useAppContext} or {@code useLocalAppContext} methods.
     *
     * @param appId the identifier of the application to set for the application context
     * @return a {@code UseApplicationInfo} instance representing the scoped application
     * context, which must be closed to restore the previous application context
     */
    public static UseApplicationInfo useApplicationInfo(String appId) {
        return new UseApplicationInfo(appId);
    }

    /**
     * Executes the provided supplier within the scope of a specific application context
     * identified by the given application ID. The application context temporarily switches
     * to the specified application during execution, and the previous application context
     * is restored automatically after completion.
     *
     * @param <T>      The return type of the supplier's operation.
     * @param appId    The identifier of the application to set for the application context.
     * @param supplier A supplier whose logic will be executed within the context of
     *                 the specified application.
     * @return The result of the computation provided by the supplier executed in the
     * specified application context.
     */
    public static <T> T useApplicationInfo(String appId, Supplier<T> supplier) {
        try (UseApplicationInfo ignored = AppContextScope.useApplicationInfo(appId)) {
            return supplier.get();
        }
    }

    /**
     * Creates a new scoped application context using the provided {@code ApplicationInfo}.
     * The application context will temporarily switch to the specified {@code ApplicationInfo}
     * for the duration of its usage. The previous application context is restored automatically
     * when the context is closed.
     * <p>
     * This method will override the ApplicationInfo in the ApplicationInfoContextHolder.
     * Wherefore it has a deeper impact on the application context as the {@code useAppContext} or {@code useLocalAppContext} methods.
     *
     * @param appInfo the {@code ApplicationInfo} to set for the application context
     * @return a {@code UseApplicationInfo} instance representing the scoped application context,
     * which must be closed to restore the previous application context
     */
    public static UseApplicationInfo useApplicationInfo(ApplicationInfo appInfo) {
        return new UseApplicationInfo(appInfo);
    }


    /**
     * Executes the provided supplier within the scope of a specific application context
     * defined by the given {@code ApplicationInfo}. The application context temporarily
     * switches to the specified {@code ApplicationInfo} during execution, and the previous
     * application context is restored automatically afterward.
     *
     * @param <T>      The return type of the supplier's computation.
     * @param appInfo  The {@code ApplicationInfo} to set for the application context.
     * @param supplier A supplier whose logic will be executed within the context of the
     *                 specified {@code ApplicationInfo}.
     * @return The result of the computation provided by the supplier executed in the
     * specified application context.
     */
    public static <T> T useApplicationInfo(ApplicationInfo appInfo, Supplier<T> supplier) {
        try (UseApplicationInfo ignored = AppContextScope.useApplicationInfo(appInfo)) {
            return supplier.get();
        }
    }


    public static class UseAppContext implements AutoCloseable {
        private final String previousProviderName;

        private UseAppContext(String providerName) {
            previousProviderName = ProviderNameContextHolder.getProviderName();
            ProviderNameContextHolder.setProviderName(providerName);
        }

        @Override
        public void close() {
            if (previousProviderName == null) {
                ProviderNameContextHolder.clear();
            } else {
                ProviderNameContextHolder.setProviderName(previousProviderName);
            }
        }
    }

    public static class UseApplicationInfo implements AutoCloseable {

        private final ApplicationInfo prevAppInfo;

        @Getter
        private final ApplicationInfo appInfo;

        private UseApplicationInfo(ApplicationInfo appInfo) {
            prevAppInfo = ApplicationInfoContextHolder.getCurrentApplicationInfo();
            this.appInfo = appInfo;
            ApplicationInfoContextHolder.setCurrentApplicationInfo(appInfo);
        }

        private UseApplicationInfo(String appId) {
            if ("-home-".equals(appId)) {
                this.appInfo = ApplicationInfoList.getHomeRepository();
            } else {
                this.appInfo = ApplicationInfoList.getRepositoryInfoById(appId);
            }

            this.prevAppInfo = ApplicationInfoContextHolder.getCurrentApplicationInfo();
            ApplicationInfoContextHolder.setCurrentApplicationInfo(this.appInfo);
        }

        @Override
        public void close() {
            ApplicationInfoContextHolder.setCurrentApplicationInfo(prevAppInfo);
        }
    }
}
