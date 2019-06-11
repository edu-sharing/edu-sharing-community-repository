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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.rpc.RepositoryInfo;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSet;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSets;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.Edu_SharingProperties;
import org.edu_sharing.repository.server.tools.metadataset.MetadataReader;


/**
 * This class is used to externalize methods from SearchResultWidget,
 * so that they can also be used outside the GWT context
 * @author Christian
 */
public class MCAlfrescoServiceImplExt {

	static Logger logger = Logger.getLogger(MCAlfrescoServiceImplExt.class);
	public static RepositoryInfo getRepositoryiesInfo(){
		return MCAlfrescoServiceImplExt.getRepositoryiesInfo(null);
	}
	
	/**
	 * @param metadataSetName: load a single, non registered metadataset using the resource pattern:
	 * org.edu_sharing.metadataset.metadataset_${metadataSetName}.xml
	 * If null all registered metadatasets are loaded
	 *
	 * @return list of property filename(key) and caption (value)
	 */
	public static RepositoryInfo getRepositoryiesInfo(String metadataSetName){
		
		RepositoryInfo result = null;
		result = new RepositoryInfo();
		HashMap<String, HashMap<String,String>> repInfo = new LinkedHashMap<String, HashMap<String,String>>();
		for (String key : ApplicationInfoList.getApplicationInfos().keySet()) {
			
			ApplicationInfo tmpAppInfo = ApplicationInfoList.getRepositoryInfoById(key);
			if (tmpAppInfo.ishomeNode() || (tmpAppInfo.getType().equals(CCConstants.APPLICATIONTYPE_REPOSITORY) && tmpAppInfo.getSearchable())) {				
				ApplicationInfo appInfo = tmpAppInfo;
				HashMap<String,String> repProps = new HashMap<String,String>();
				repProps.put(CCConstants.APPLICATIONINFO_APPCAPTION, appInfo.getAppCaption());
				repProps.put(CCConstants.APPLICATIONINFO_APPID, appInfo.getAppId());
				repProps.put(CCConstants.APPLICATIONINFO_ISHOMENODE, new Boolean(appInfo.ishomeNode()).toString());
				repProps.put(CCConstants.APPLICATIONINFO_REPOSITORYTYPE, appInfo.getRepositoryType());
				repProps.put(CCConstants.APPLICATIONINFO_RECOMMENDOBJECTS_QUERY, appInfo.getRecommend_objects_query());
				repProps.put(CCConstants.APPLICATIONINFO_LOGO, appInfo.getLogo());
				if(appInfo.getCustomCss() != null){
					repProps.put(CCConstants.APPLICATIONINFO_CUSTOM_CSS, appInfo.getCustomCss());
				}
				repProps.put(CCConstants.APPLICATIONINFO_LOGOUTURL,appInfo.getLogoutUrl());
				
				repInfo.put(key,repProps);
			}
			
		}
		
		result.setRepInfoMap(repInfo);
		
		//the default mdss
		HashMap<String, MetadataSets> repoMdss = RepoFactory.getRepositoryMetadataSets();
		
		//standalone mds
		if(metadataSetName != null){
			
			//for security reasons only allow letters
			metadataSetName = metadataSetName.replaceAll(CCConstants.metadataseStandaloneNameFilterRegex, "");
			
			String resource = CCConstants.metadataseStandaloneFileTemplate.replace("${name}", metadataSetName);
			
			try{
				MetadataSet mds = RepoFactory.getStandaloneMetadataSet(resource);
				if(mds != null){
					
					MetadataSets mdss = new MetadataSets();
					List<MetadataSet> mdsList = new ArrayList<MetadataSet>();
					mdsList.add(mds);
					mdss.setMetadataSets(mdsList);
					
					repoMdss = new HashMap<String, MetadataSets>();
					repoMdss.put(ApplicationInfoList.getHomeRepository().getAppId(), mdss);
				}else{
					logger.error("could not load standalone metadataset "+resource+" Using the default registered.");
				}
			}catch(Throwable e){
				logger.error("error while loading standalone metadataset: "+resource+" Using the default registered.", e);
			}
			
		}
		
		//do late init
		for(Map.Entry<String, MetadataSets> mdss : repoMdss.entrySet()){
			
			for(MetadataSet mds : mdss.getValue().getMetadataSets()){
				if(!mds.isLateInitDone()){
					new MetadataReader().initPostWebappLoad(mds);
				}
			}
			
		}
		
		result.setRepMetadataSetsMap(repoMdss);
		
		//help links
		result.setHelpUrlCC(Edu_SharingProperties.instance.getHelpUrlCC());
		result.setHelpUrlES(Edu_SharingProperties.instance.getHelpUrlES());
		result.setHelpUrlCustom(Edu_SharingProperties.instance.getHelpUrlCustom());
		result.setHelpUrlShare(Edu_SharingProperties.instance.getHelpUrlShare());
		result.setFuzzyUserSearch(Edu_SharingProperties.instance.isFuzzyUserSearch());
		
		return result;
	}	
	
}
