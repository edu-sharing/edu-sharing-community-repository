package org.edu_sharing.repository.server.appcontext;

import lombok.extern.slf4j.Slf4j;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;

/**
 * A utility class that provides a thread-local context holder for managing
 * {@link ApplicationInfo} instances. It allows setting and retrieving the
 * current {@code ApplicationInfo} associated with the current thread.
 *
 * This class uses a {@link ThreadLocal} to associate a specific
 * {@code ApplicationInfo} instance with the execution context of the thread.
 * If no {@code ApplicationInfo} is explicitly set, a default one is retrieved
 * from {@link ApplicationInfoList#getHomeRepository()}.
 *
 * Note: This is a final class and cannot be subclassed.
 */
@Slf4j
public final class ApplicationInfoContextHolder {

    private static final ThreadLocal<ApplicationInfo> currentApplicationInfo = new ThreadLocal<>();

    /**
     * Retrieves the current {@code ApplicationInfo} instance associated with the
     * current thread. If no {@code ApplicationInfo} is explicitly set, this method
     * defaults to retrieving a predefined {@code ApplicationInfo} instance from
     * {@link ApplicationInfoList#getHomeRepository()} and sets it for the current thread.
     *
     * @return the current {@code ApplicationInfo} associated with the thread,
     *         or a default instance from {@code ApplicationInfoList#getHomeRepository()}
     *         if none is set.
     */
    public static ApplicationInfo getCurrentApplicationInfo() {
        ApplicationInfo applicationInfo = currentApplicationInfo.get();
        if(applicationInfo == null){
            log.debug("No ApplicationInfo set");
            applicationInfo = ApplicationInfoList.getHomeRepository();
            currentApplicationInfo.set(applicationInfo);
        }
        return applicationInfo;
    }

    /**
     * Sets the current {@link ApplicationInfo} instance to be associated with the
     * executing thread's context.
     *
     * This method updates the thread-local storage with the provided
     * {@code ApplicationInfo} object, making it accessible via
     * {@link #getCurrentApplicationInfo()} within the same thread.
     *
     * @param applicationInfo the {@code ApplicationInfo} instance to be set for the
     *                         current thread; must not be null
     * @throws NullPointerException if the provided {@code applicationInfo} is null
     */
    public static void setCurrentApplicationInfo(ApplicationInfo applicationInfo) {
        log.debug("Set ApplicationInfo {}", applicationInfo.getAppCaption() );
        currentApplicationInfo.set(applicationInfo);
    }

    public static void clear() {
        currentApplicationInfo.remove();
    }
}
