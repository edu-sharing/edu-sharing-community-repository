/**
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */
package org.edu_sharing.repository.server;

import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.node.MLPropertyInterceptor;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.metadataset.v2.MetadataSet;
import org.edu_sharing.metadataset.v2.MetadataSetInfo;
import org.edu_sharing.metadataset.v2.tools.MetadataHelper;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.appcontext.AppContextServiceLocator;
import org.edu_sharing.repository.server.tools.*;

import java.util.*;

@Slf4j
public class RepoFactory {

    /**
     * appID, MCBaseClient Subclass
     *
     * this property is for performance reasons: use reflections only the first
     * time then use direct contructor calls with getInstance of the
     * MCBaseClient subclass
     */
    static Map<String, MCBaseClient> appClassCache = new HashMap<>();

    static Map<String, AuthenticationTool> appAuthToolCache = new HashMap<>();


    static Properties eduSharingProps = null;
    private static long lastRefreshed = System.currentTimeMillis();

    /**
     * get an MCBaseClient instance that can access the repository with repositoryId.
     * for remote repositories: the remote authinfo is saved in session
     *
     * checks if valid authInfo is found in session
     * if remote repository and no authinfo was found the AuthByApp mechanism is used
     *
     * else it throws exeption
     */
    public static MCBaseClient getInstance(String repositoryId, HttpSession session) throws Throwable {

        if (repositoryId == null) {
            repositoryId = ApplicationInfoList.getHomeRepository().getAppId();
        }

        AuthenticationTool authTool = RepoFactory.getAuthenticationToolInstance(repositoryId);

        //for remote repositories: the authinfo is created by this method if its missing
        Map<String, String> authInfo = authTool.validateAuthentication(session);

        ApplicationInfo repInfo = ApplicationInfoList.getRepositoryInfoById(repositoryId);
        if (authInfo != null) {
            return getInstanceForRepo(repInfo, authInfo);
        } else if ((AuthenticationUtil.isRunAsUserTheSystemUser() || "admin".equals(AuthenticationUtil.getRunAsUser()))
                && ApplicationInfoList.getHomeRepository().getAppId().equals(repositoryId)) {
            return new MCAlfrescoAPIClient();
        }
        throw new Exception("not authenticated");
    }

    /**
     * get an MCBaseClient instance that can access the repository with repositoryId.
     * for remote repositories: the remote repo authinfo is temporary created and not saved in session
     *
     * @TODO for remote repositories: the logout servlet can not end those sessions cause their tickets are not saved in session
     * maybe make this method deprecated and always use the one with the session
     *
     * @param homeRepAuthInfo the AuthenticationInfo of the home repository
     */
    public static MCBaseClient getInstance(String repositoryId, Map<String, String> homeRepAuthInfo) throws Throwable {

        log.debug("repositoryId:{}", repositoryId);
        ApplicationInfo repInfo;
        if (StringUtils.isBlank(repositoryId)) {
            repInfo = ApplicationInfoList.getHomeRepository();
        } else {
            repInfo = ApplicationInfoList.getRepositoryInfoById(repositoryId);
        }
        Map<String, String> remoteAuthInfo = null;

        // authenticate when it's an remote Repository and an
        // AuthenticationWebservice is configured
        // edmond for example does not need to beauthenticated so leave the
        // authenticationwebservice in the config file blank:
        if (!repInfo.ishomeNode() && StringUtils.isNotBlank(repInfo.getAuthenticationwebservice())) {

            // automatisch anlegen wenn der user noch nicht da ist
            // getAuthInfoForApp(authInfo, repInfo, true);
            AuthenticatorRemoteAppResult resultRemoteAuth = new AuthenticatorRemoteRepository().getAuthInfoForApp((String) homeRepAuthInfo.get(CCConstants.AUTH_USERNAME), repInfo);
            remoteAuthInfo = resultRemoteAuth.getAuthenticationInfo();
            return getInstanceForRepo(repInfo, remoteAuthInfo);
        } else {
            return getInstanceForRepo(repInfo, homeRepAuthInfo);
        }
    }

    /**
     *
     * @param authInfo    the authenticationinfo that can be used to access the repository.
     * 				  	for remote repositories: this param must contain a valid ticket of the remote repository,
     * 					no authbyapp mechanism is done here
     */
    public static MCBaseClient getInstanceForRepo(ApplicationInfo repInfo, Map<String, String> authInfo) throws Throwable {


        String repositoryId = repInfo.getAppId();
        // we don't want to get Multilang props with the current langauage
        // setting of the user in alfresco.
        // we want all props and filter it on client side. the language setting
        // comes from gwt and can be set in the host application
        // http://forums.alfresco.com/en/viewtopic.php?f=36&t=26020
        // I wanted to do this only one time in the reflection Part, but it
        // seems that is sometime set back, so we do it every time when we
        // access the Repository

        boolean isMLAware = LightbendConfigLoader.get().getBoolean("repository.multilang");
        log.debug("isMLAware:" + isMLAware);
        MLPropertyInterceptor.setMLAware(isMLAware);
        //MLPropertyInterceptor.setMLAware(false);

        // use reflections only the first time the app was loaded
        if (appClassCache.containsKey(repositoryId)) {
            log.debug("getting MCBaseClient by CACHE for {}", repositoryId);
            MCBaseClient result = appClassCache.get(repositoryId).getInstance(repInfo.getAppFile(), authInfo);

            log.debug("returning {}", result.getClass().getSimpleName());
            return result;
        } else {
            MCBaseClient mcBaseClient;
            if (repInfo.isRemoteAlfresco()) {
                mcBaseClient = new MCAlfrescoAPIClient(repInfo.getAppFile(), authInfo);
            } else {
                log.debug("getting MCBaseClient by REFLECTION for {}", repositoryId);
                mcBaseClient =  new MCAlfrescoAPIClient(repInfo.getAppId(), authInfo);
            }
            appClassCache.put(repositoryId, mcBaseClient);
            log.debug("returning {}", mcBaseClient.getClass().getSimpleName());

            return mcBaseClient;
        }

    }

    public static AuthenticationTool getAuthenticationToolInstance(String applicationId) throws Throwable {
        return AppContextServiceLocator.getInstance().get(AuthenticationTool.class, applicationId);
    }

    public static long getLastRefreshed() {
        return lastRefreshed;
    }

    public static void refresh() {
        lastRefreshed = System.currentTimeMillis();
        appClassCache.clear();
        eduSharingProps = null;
    }

    public static List<MetadataSetInfo> getMetadataSetsForRepository(String repositoryId) throws Exception {
        if (repositoryId == null) {
            repositoryId = ApplicationInfoList.getHomeRepository().getAppId();
        }
        ApplicationInfo appInfo = ApplicationInfoList.getRepositoryInfoById(repositoryId);
        ArrayList<MetadataSetInfo> sets = new ArrayList<>();
        for (String id : appInfo.getMetadatsets()) {
            MetadataSetInfo info = new MetadataSetInfo();
            MetadataSet mds = MetadataHelper.getMetadataset(appInfo, id);
            info.setId(id);
            info.setName(mds.getName());
            sets.add(info);
        }
        return sets;
    }

    public static String getEdusharingProperty(String key) {
        try {
            if (eduSharingProps == null) {
                eduSharingProps = PropertiesHelper.getProperties("edu-sharing.properties", PropertiesHelper.TEXT);
            }
            return eduSharingProps.getProperty(key);
        } catch (Exception e) {
            log.error("Problems opening edu-sharing.properties:" + e.getMessage());
        }
        return null;
    }

}
