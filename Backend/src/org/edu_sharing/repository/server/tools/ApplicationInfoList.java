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
package org.edu_sharing.repository.server.tools;

import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class ApplicationInfoList {
	
	private static Log logger = LogFactory.getLog(ApplicationInfoList.class);
	
	private static Map<String, ApplicationInfo> appInfos = null;
	
	public static ApplicationInfo getRepositoryInfo(String file){
		ApplicationInfo result = null;
		if(appInfos == null || appInfos.size() < 1){
			//init
			getApplicationInfos();
		}
		for(String key:appInfos.keySet()){
			ApplicationInfo repInfo = appInfos.get(key);
			if(repInfo != null && repInfo.getAppFile().equals(file)){
				result = repInfo;
				return result;
			}
		}
		
		return result;		
	}
	
	public static ApplicationInfo getRenderService() {

		if(appInfos == null || appInfos.size() < 1){

			getApplicationInfos();
		}
		for(ApplicationInfo appInfo : appInfos.values()){
			if(ApplicationInfo.TYPE_RENDERSERVICE.equals(appInfo.getType())){
				return appInfo;
			}
		}
		return null;
	}

	public static ApplicationInfo getLearningLocker() {

		if(appInfos == null || appInfos.size() < 1){

			getApplicationInfos();
		}
		for(ApplicationInfo appInfo : appInfos.values()){
			if(ApplicationInfo.TYPE_LEARNING_LOCKER.equals(appInfo.getType())){
				return appInfo;
			}
		}
		return null;
	}
	
	public static ApplicationInfo getRepositoryInfoById(String repId){
		ApplicationInfo result = null;
		if(appInfos == null || appInfos.size() < 1){
			
			getApplicationInfos();
		}
		for(String key:appInfos.keySet()){
			ApplicationInfo repInfo = appInfos.get(key);
			if(repInfo != null && repInfo.getAppId().equals(repId)){
				result = repInfo;
				return result;
			}
		}
		return result;
	}
	public static ApplicationInfo getRepositoryInfoByType(String type){
		ApplicationInfo result = null;
		if(appInfos == null || appInfos.size() < 1){
			getApplicationInfos();
		}
		for(String key:appInfos.keySet()){
			ApplicationInfo repInfo = appInfos.get(key);
			if(repInfo != null && repInfo.getType().equals(type)){
				result = repInfo;
				return result;
			}
		}
		return result;
	}
	public static ApplicationInfo getRepositoryInfoByRepositoryType(String type){
		ApplicationInfo result = null;
		if(appInfos == null || appInfos.size() < 1){
			getApplicationInfos();
		}
		for(String key:appInfos.keySet()){
			ApplicationInfo repInfo = appInfos.get(key);
			if(repInfo != null && repInfo.getRepositoryType().equals(type)){
				result = repInfo;
				return result;
			}
		}
		return result;
	}
	/**
	 * 
	 * @return Map with AppId, ApplicationInfo
	 */
	public static Map<String, ApplicationInfo> getApplicationInfos(){
		logger.debug("Classloader(Thread):"+Thread.currentThread().getContextClassLoader().getClass().getName());
		logger.debug("Classloader(ApplicationInfoList):"+ApplicationInfoList.class.getClassLoader().getClass().getName());
		if(appInfos == null){
			logger.debug("appInfos == null");
			initAppInfos();
		}else{
			logger.debug("appInfos not null");
		}
		return appInfos;
	}
	
	/**
	 * synchronized to prevent errors when multithreads call this static method
	 */
	private static synchronized void initAppInfos(){
		String repStr = null;
		try{
			repStr = PropertiesHelper.getProperty("applicationfiles", "ccapp-registry.properties.xml", PropertiesHelper.XML);
		}catch(Exception e){
			logger.error("Could not find Repository Registry",e);
			return;
		}
		logger.debug("appStr"+repStr);
		//Linked hashMap for keeping insertion order

		appInfos = Collections.synchronizedMap(new LinkedHashMap());
		if(repStr != null && repStr.trim().length() > 0){
			logger.debug("appStr != null && appStr.trim().length() > 0");
			 String[] repField = repStr.split(",");
			 if(repField.length >= 1){
				 logger.debug("repField.length >= 1");
				for(String repFile: repField){
					if(repFile != null && repFile.trim().length() > 0){
						try{
							ApplicationInfo repInfo = new ApplicationInfo(repFile.trim());
							logger.debug("put:"+repFile.trim()+" "+repInfo);
							appInfos.put(repInfo.getAppId(), repInfo);
						}catch(Exception e){
							e.printStackTrace();
						}
					}
			 	}
			 }
		 }
	}
	
	/**
	 * 
	 * @return List of RepositoryInfos with local Repository as the first one
	 */
	public static ArrayList<ApplicationInfo> getRepositoryInfosOrdered(){
		//set home reporsitory as the first one
		ArrayList<ApplicationInfo> appInfoList = new ArrayList<ApplicationInfo>();
		for(String key : ApplicationInfoList.getApplicationInfos().keySet()){
			ApplicationInfo repInfo = ApplicationInfoList.getApplicationInfos().get(key);
			appInfoList.add(repInfo);
		}
		Collections.sort(appInfoList);
		return appInfoList;
	}
	
	public static ApplicationInfo getHomeRepository(){
		
		if(appInfos == null || appInfos.size() < 1){
			//init
			getApplicationInfos();
		}
		ApplicationInfo homeRepository = null;
			
		for(String appKey:appInfos.keySet()){
			ApplicationInfo appInfo = appInfos.get(appKey);
			if(appInfo != null && appInfo.ishomeNode()){
				homeRepository = appInfo;
			}
		}
		if(homeRepository == null) logger.error("no home repository found. check your application files");
		return homeRepository;
	}
	public static ApplicationInfo getHomeRepositoryObeyConfig(String[] allowedRepos){
		if(appInfos == null || appInfos.size() < 1){
			getApplicationInfos();
		}
		ApplicationInfo realHome = getHomeRepository();

		if(allowedRepos == null || allowedRepos.length == 0 ){
			return realHome;
		}
		List<String> reposList = Arrays.asList(allowedRepos);
		if(reposList.contains("-home-") || reposList.contains(realHome.getAppId())) {
			return realHome;
		}
		ApplicationInfo configHome = getRepositoryInfoById(allowedRepos[0]);
		if(configHome==null){
			logger.error("Config does not allow home, and fallback repo " + allowedRepos[0]+" does not exist! Please check your client.config.xml!");
		} else {
			logger.info("Switching -home- repo to " + allowedRepos[0] + " because of current config");
			return configHome;
		}
		return getHomeRepository();
	}
	
	public static void refresh(){
		logger.debug("calling");
		appInfos = null;
		getApplicationInfos();
		logger.debug("returning");
	}
}
