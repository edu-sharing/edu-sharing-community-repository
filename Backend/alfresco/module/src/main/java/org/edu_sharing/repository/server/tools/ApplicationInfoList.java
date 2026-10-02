/**
 *
 */
package org.edu_sharing.repository.server.tools;

import jakarta.servlet.ServletRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;


public class ApplicationInfoList {

    private static final Log logger = LogFactory.getLog(ApplicationInfoList.class);

    /**
     * always replaced as a whole, so readers get a consistent state without locking. null until the first successful init
     */
    private static volatile ApplicationInfoListSnapshot snapshot;

    public static ApplicationInfo getRepositoryInfo(String file) {
        return getApplicationInfoByProperty(file, ApplicationInfoProperty.APPFILE);
    }

    public static boolean isLocalRepository(String repositoryId) {
        if (repositoryId == null) {
            return true;
        }
        ApplicationInfo repo = getRepositoryInfoById(repositoryId);
        return repo.getAppId().equals(getHomeRepository().getAppId()) || ApplicationInfo.REPOSITORY_TYPE_LOCAL.equals(repo.getRepositoryType());
    }

    static enum ApplicationInfoProperty {TYPE, REPOSITORYTYPE, APPID, HOME, APPFILE}

    ;

    private static ApplicationInfo getApplicationInfoByProperty(String value, ApplicationInfoProperty prop) {
        for (ApplicationInfo appInfo : getSnapshot().appInfos().values()) {
            if (prop.equals(ApplicationInfoProperty.TYPE)) {
                if (value.equals(appInfo.getType())) {
                    return appInfo;
                }
            }
            if (prop.equals(ApplicationInfoProperty.REPOSITORYTYPE)) {
                if (value.equals(appInfo.getRepositoryType())) {
                    return appInfo;
                }
            }
            if (prop.equals(ApplicationInfoProperty.APPID)) {
                if (value.equals(appInfo.getAppId())) {
                    return appInfo;
                }
            }
            if (prop.equals(ApplicationInfoProperty.HOME)) {
                if (appInfo.ishomeNode()) return appInfo;
            }
            if (prop.equals(ApplicationInfoProperty.APPFILE)) {
                if (value.equals(appInfo.getAppFile())) {
                    return appInfo;
                }
            }

        }
        return null;
    }

    public static ApplicationInfo getRenderService() {
        return getApplicationInfoByProperty(ApplicationInfo.TYPE_RENDERSERVICE, ApplicationInfoProperty.TYPE);
    }

    @Deprecated
    public static ApplicationInfo getLearningLocker() {
        return getApplicationInfoByProperty(ApplicationInfo.TYPE_LEARNING_LOCKER, ApplicationInfoProperty.TYPE);
    }

    public static ApplicationInfo getRepositoryInfoById(String repId) {
        return getApplicationInfoByProperty(repId, ApplicationInfoProperty.APPID);
    }

    public static ApplicationInfo getRepositoryInfoByType(String type) {
        return getApplicationInfoByProperty(type, ApplicationInfoProperty.TYPE);
    }

    public static ApplicationInfo getRepositoryInfoByRepositoryType(String type) {
        return getApplicationInfoByProperty(type, ApplicationInfoProperty.REPOSITORYTYPE);
    }

    /**
     *
     * @return Map with AppId, ApplicationInfo
     */
    public static Collection<String> getAppInfoIds() {
        return getSnapshot().appInfos().keySet();
    }

    /**
     *
     * @return Map with AppId, ApplicationInfo
     */
    public static Map<String, ApplicationInfo> getApplicationInfos() {
        /**
         * @TODO refactor calls to this methode so that hashmap building is not needed
         */
        return Collections.synchronizedMap(new HashMap<>(getSnapshot().appInfos()));
    }

    /**
     * lock free in the normal case, only the first access synchronizes
     *
     * @return the current snapshot, an empty one if the application infos could not be loaded yet
     */
    private static ApplicationInfoListSnapshot getSnapshot() {
        ApplicationInfoListSnapshot current = snapshot;
        if (current != null) {
            return current;
        }
        return initAppInfos();
    }

    /**
     * synchronized to prevent errors when multithreads call this static method.
     * If loading fails, the snapshot stays null and the next call tries again.
     */
    private static synchronized ApplicationInfoListSnapshot initAppInfos() {
        if (snapshot == null) {
            snapshot = ApplicationInfoListSnapshot.build();
        }
        return snapshot != null ? snapshot : ApplicationInfoListSnapshot.EMPTY;
    }

    /**
     *
     * @return List of RepositoryInfos with local Repository as the first one
     */
    public static ArrayList<ApplicationInfo> getRepositoryInfosOrdered() {
        //set home reporsitory as the first one
        ArrayList<ApplicationInfo> appInfoList = new ArrayList<>(getSnapshot().appInfos().values());
        Collections.sort(appInfoList);
        return appInfoList;
    }

    public static ApplicationInfo getHomeRepository() {
        ApplicationInfo appInfoHome = getSnapshot().appInfoHome();
        if (appInfoHome == null) logger.error("no home repository found. check your application files");
        return appInfoHome;
    }

    /**
     * True if the request did not arrive on the configured home repository port, i.e. it bypassed
     * the public reverse proxy (e.g. a service calling the internal Tomcat port such as 8080
     * directly) and can be treated as trusted internal traffic.
     */
    public static boolean isInternalPortRequest(ServletRequest request) {
        try {
            int homePort = Integer.parseInt(getHomeRepository().getPort());
            return request.getLocalPort() == homePort;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static ApplicationInfo getRenderingService2() {
        ApplicationInfo appInfoRenderingService2 = getSnapshot().appInfoRenderingService2();
        if (appInfoRenderingService2 == null) logger.warn("no rendering service 2 found. check your application files");
        return appInfoRenderingService2;
    }

    public static List<ApplicationInfo> getAppInfosLtiTool(){
        return getSnapshot().appInfosLtiTool();
    }

    public static ApplicationInfo getHomeRepositoryObeyConfig(String[] allowedRepos) {

        ApplicationInfo realHome = getHomeRepository();

        if (allowedRepos == null || allowedRepos.length == 0) {
            return realHome;
        }
        List<String> reposList = Arrays.asList(allowedRepos);
        if (reposList.contains("-home-") || reposList.contains(realHome.getAppId())) {
            return realHome;
        }
        ApplicationInfo configHome = getRepositoryInfoById(allowedRepos[0]);
        if (configHome == null) {
            logger.error("Config does not allow home, and fallback repo " + allowedRepos[0] + " does not exist! Please check your client.config.xml!");
        } else {
            logger.info("Switching -home- repo to " + allowedRepos[0] + " because of current config");
            return configHome;
        }
        return getHomeRepository();
    }

    /**
     * builds a new snapshot and replaces the current one, readers keep using the previous snapshot until then.
     * If loading fails, the previous snapshot is kept.
     */
	public static synchronized void refresh(){
        logger.debug("calling");
        ApplicationInfoListSnapshot newSnapshot = ApplicationInfoListSnapshot.build();
        if (newSnapshot == null) {
            logger.error("refresh of application infos failed, keeping the previous state");
        } else {
            snapshot = newSnapshot;
        }
        logger.debug("returning");
    }
}
