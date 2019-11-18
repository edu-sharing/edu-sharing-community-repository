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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import javax.servlet.http.HttpSession;

import org.alfresco.repo.node.MLPropertyInterceptor;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.edu_sharing.lightbend.LightbendConfigLoader;
import org.edu_sharing.metadataset.v2.MetadataReaderV2;
import org.edu_sharing.metadataset.v2.MetadataSetInfo;
import org.edu_sharing.metadataset.v2.MetadataSetV2;
import org.edu_sharing.metadataset.v2.tools.MetadataHelper;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSet;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSets;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.AuthenticatorRemoteAppResult;
import org.edu_sharing.repository.server.tools.AuthenticatorRemoteRepository;
import org.edu_sharing.repository.server.tools.PropertiesHelper;
import org.edu_sharing.repository.server.tools.metadataset.MetadataReader;
import org.edu_sharing.service.config.ConfigServiceFactory;

public class RepoFactory {

	/**
	 * appID, MCBaseClient Subclass
	 * 
	 * this property is for performance reasons: use reflections only the first
	 * time then use direct contructor calls with getInstance of the
	 * MCBaseClient subclass
	 */
	static HashMap<String, MCBaseClient> appClassCache = new HashMap<String, MCBaseClient>();
	
	static HashMap<String,AuthenticationTool> appAuthToolCache = new HashMap<String,AuthenticationTool>();

	private static Log logger = LogFactory.getLog(RepoFactory.class);

	static Properties eduSharingProps = null;
	
	/**
	 * get an MCBaseClient instance that can access the repository with repositoryId.
	 * for remote repositories: the remote authinfo is saved in session
	 * 
	 * checks if valid authInfo is found in session 
	 * if remote repository and no authinfo was found the AuthByApp mechanism is used
	 * 
	 * else it throws exeption
	 * @param repositoryId
	 * @param session
	 * @return
	 * @throws Throwable
	 */
	public static MCBaseClient getInstance(String repositoryId, HttpSession session) throws Throwable {
		
		if(repositoryId == null){
			repositoryId = ApplicationInfoList.getHomeRepository().getAppId();
		}
		
		AuthenticationTool authTool = RepoFactory.getAuthenticationToolInstance(repositoryId);
		
		//for remote repositories: the authinfo is created by this method if its missing
		HashMap<String,String> authInfo = authTool.validateAuthentication(session);
		
		ApplicationInfo repInfo = ApplicationInfoList.getRepositoryInfoById(repositoryId);
		if(authInfo != null){
			return getInstanceForRepo(repInfo,authInfo);
		}else if((AuthenticationUtil.isRunAsUserTheSystemUser() || "admin".equals(AuthenticationUtil.getRunAsUser())) 
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
	 * @param repositoryId
	 * @param homeRepAuthInfo the AuthenticationInfo of the home repository
	 * @return
	 * @throws Throwable
	 */
	public static MCBaseClient getInstance(String repositoryId, HashMap homeRepAuthInfo) throws Throwable {

		logger.debug("repositoryId:" + repositoryId);
		ApplicationInfo repInfo = null;
		if (repositoryId == null || repositoryId.trim().equals("")) {
			repInfo = ApplicationInfoList.getHomeRepository();
		} else {
			repInfo = ApplicationInfoList.getRepositoryInfoById(repositoryId);
		}
		HashMap remoteAuthInfo = null;

		// authenticate when it's an remote Repository and an
		// AuthenticationWebservice is configured
		// edmond for example does not need to beauthenticated so leave the
		// authenticationwebservice in the config file blank:
		if (!repInfo.ishomeNode() && repInfo.getAuthenticationwebservice() != null && !repInfo.getAuthenticationwebservice().trim().equals("")) {

			// automatisch anlegen wenn der user noch nicht da ist
			// getAuthInfoForApp(authInfo, repInfo, true);
			AuthenticatorRemoteAppResult resultRemoteAuth = new AuthenticatorRemoteRepository().getAuthInfoForApp(homeRepAuthInfo, repInfo);
			remoteAuthInfo = resultRemoteAuth.getAuthenticationInfo();
			return getInstanceForRepo(repInfo, remoteAuthInfo);
		}else{
			return getInstanceForRepo(repInfo, homeRepAuthInfo);
		}
	}

	/**
	 * 
	 * @param repInfo
	 * @param authInfo 	the authenticationinfo that can be used to access the repository. 
	 * 				  	for remote repositories: this param must contain a valid ticket of the remote repository, 
	 * 					no authbyapp mechanism is done here
	 * @return
	 * @throws Throwable
	 */
	public static MCBaseClient getInstanceForRepo(ApplicationInfo repInfo, HashMap authInfo) throws Throwable {

		
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
		logger.debug("isMLAware:"+isMLAware);
		MLPropertyInterceptor.setMLAware(isMLAware);
		//MLPropertyInterceptor.setMLAware(false);

		// use reflections only the first time the app was loaded
		if (appClassCache.containsKey(repositoryId)) {
			logger.debug("getting MCBaseClient by CACHE for " + repositoryId);
			MCBaseClient result = null;
			result = appClassCache.get(repositoryId).getInstance(repInfo.getAppFile(), authInfo);
			
			logger.debug("returning " + result.getClass().getSimpleName());
			return result;
		} else {
			logger.debug("getting MCBaseClient by REFLECTION for " + repositoryId);
			String classname = repInfo.getSearchclass();
			Class clazz = Class.forName(classname);
			Object obj = clazz.getConstructor(new Class[] { String.class, HashMap.class }).newInstance(new Object[] { repInfo.getAppFile(), authInfo });
			
			MCBaseClient mcBaseClient = (MCBaseClient) obj;
			appClassCache.put(repositoryId, mcBaseClient);
			logger.debug("returning " + mcBaseClient.getClass().getSimpleName());

			return mcBaseClient;
		}

	}
	
	public static AuthenticationTool getAuthenticationToolInstance(String applicationId) throws Throwable{
		
		if(applicationId == null) applicationId = ApplicationInfoList.getHomeRepository().getAppId();
		
		AuthenticationTool result = appAuthToolCache.get(applicationId);
		
		if(result == null){
			ApplicationInfo appInfo = ApplicationInfoList.getRepositoryInfoById(applicationId);
			//allow homeapplication and ws client
			if(appInfo.getAuthenticationtoolclass() == null && appInfo.ishomeNode()){
				result = new AuthenticationToolAPI();
				appAuthToolCache.put(applicationId, result);
			}else if(appInfo.getAuthenticationtoolclass() != null ){
				Class clazz = Class.forName(appInfo.getAuthenticationtoolclass());
				result = (AuthenticationTool)clazz.getConstructor(new Class[] { String.class}).newInstance(	new Object[] { appInfo.getAppId()});
			}else if(appInfo.ishomeNode()){
				result = new AuthenticationToolAPI();
				appAuthToolCache.put(applicationId, result);
			}else{
				//TODO getAuthClass from ApplicationInfoFile
				//Class clazz = Class.forName("org.edu_sharing.repository.server.AuthenticationToolWS");
				//result = (AuthenticationTool)clazz.getConstructor(new Class[] { String.class}).newInstance(	new Object[] { appInfo.getAppId()});
				result = new AuthenticationToolAPI();
			}
		}
		return result;
	}

	public static void refresh() {
		appClassCache.clear();
		repositoryMetadataSets.clear();
		MetadataReaderV2.refresh();
		ConfigServiceFactory.refresh();
		defaultMetadataSet = null;
		eduSharingProps = null;
	}

	public static void clearMetadataSets() {
		repositoryMetadataSets.clear();
	}

	public static HashMap<String, MetadataSets> getRepositoryMetadataSets() {
		logger.debug("starting");
		String devmode = ApplicationInfoList.getHomeRepository().getDevmode();
		if (devmode != null && new Boolean(devmode) == true) {
			repositoryMetadataSets.clear();
			defaultMetadataSet = null;
		}
		/*
		if (repositoryMetadataSets.size() == 0) {
			fillMetadatasets(repositoryMetadataSets);
		}
		*/
		logger.debug("returning");
		return repositoryMetadataSets;
	}
	
	/**
	 * creates a new repoMDS Map HashMap<String, MetadataSets> 
	 * fills it
	 * let the repositoryMetadataSets reference point to the new
	 */
	public static void refreshMetadataSets(){
		HashMap<String, MetadataSets> newRepositoryMetadataSets = new HashMap<String, MetadataSets>();
		//fillMetadatasets(newRepositoryMetadataSets);
		
		//umhängen aber arauf achten das niemand drauf zu greift
		synchronized(repositoryMetadataSets){
			repositoryMetadataSets = newRepositoryMetadataSets;
		}
		
	}
	
	/**
	 * adds metdatasets to param repositoryMetadataSets without clearing it
	 * @param repositoryMetadataSets
	 */
	/*
	private static void fillMetadatasets(HashMap<String, MetadataSets> repositoryMetadataSets){
		for (String key : ApplicationInfoList.getApplicationInfos().keySet()) {
			ApplicationInfo tmpAppInfo = ApplicationInfoList.getRepositoryInfoById(key);
			if (tmpAppInfo.getType().equals(CCConstants.APPLICATIONTYPE_REPOSITORY)) {
				String metadatasets = tmpAppInfo.getMetadatsets();
				// when no metadatasets are konfigured take the default one
				if (metadatasets == null) {
					logger.info("no metadatasets kopnfigured for Repository " + tmpAppInfo.getAppId() + " taking default metadatasets");
					metadatasets = "/org/edu_sharing/metadataset/metadatasets_default.xml";
				}
				MetadataReader mdr = new MetadataReader();
				MetadataSets metadataSets = mdr.getMetadataSets(metadatasets, tmpAppInfo.getAppId());
				if (metadataSets != null) {
					logger.debug("putting metadatasets for rep:" + tmpAppInfo.getAppId() + " metadataSets:" + metadataSets);
					repositoryMetadataSets.put(key, metadataSets);
				} else {
					logger.info("no metadatasets found for rep" + tmpAppInfo.getAppId());
				}
			}
		}
	}
	*/

	private static HashMap<String, MetadataSets> repositoryMetadataSets = new HashMap<String, MetadataSets>();

	private static MetadataSet defaultMetadataSet = null;

	public static MetadataSet getDefaultMetadataSet() {
		if (defaultMetadataSet == null) {
			String homeAppId = ApplicationInfoList.getHomeRepository().getAppId();
			defaultMetadataSet = getMetadataSetsForRepository(homeAppId).getMetadataSetById(CCConstants.metadatasetdefault_id);
		}
		return defaultMetadataSet;
	}

	public static MetadataSets getMetadataSetsForRepository(String repositoryId) {
		if (repositoryId == null) {
			repositoryId = ApplicationInfoList.getHomeRepository().getAppId();
		}

		HashMap<String, MetadataSets> repMetadataSets = getRepositoryMetadataSets();
		for (String key : repMetadataSets.keySet()) {
			if (key.equals(repositoryId)) {
				return repMetadataSets.get(key);
			}
		}
		return null;
	}
	public static List<MetadataSetInfo> getMetadataSetsV2ForRepository(String repositoryId) throws Exception {
		if (repositoryId == null) {
			repositoryId = ApplicationInfoList.getHomeRepository().getAppId();
		}
		ApplicationInfo appInfo = ApplicationInfoList.getRepositoryInfoById(repositoryId);
		ArrayList<MetadataSetInfo> sets = new ArrayList<MetadataSetInfo>();
		for(String id : appInfo.getMetadatsetsV2()){
			MetadataSetInfo info=new MetadataSetInfo();
			MetadataSetV2 mds = MetadataHelper.getMetadataset(appInfo, id);
			info.setId(id);
			info.setName(mds.getName());
			sets.add(info);
		}
		return sets;
	}
	static HashMap<String, MetadataSet> standaloneMetadataSets = new HashMap<String, MetadataSet>();

	public static MetadataSet getStandaloneMetadataSet(String file) throws Throwable {
		MetadataSet standAloneMDS = standaloneMetadataSets.get(file);
		if (standAloneMDS == null) {
			MetadataReader mdr = new MetadataReader();
			standAloneMDS = mdr.getMetadataSet(file);
			standaloneMetadataSets.put(file, standAloneMDS);
		}
		return standAloneMDS;
	}
}
