package org.edu_sharing.service.version;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.alfresco.repo.cache.SimpleCache;
import org.apache.commons.io.FileUtils;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.HttpQueryTool;
import org.json.JSONObject;


public class VersionService {

	public static SimpleCache<Type, String> versionCache = (SimpleCache<Type, String>) AlfAppContextGate.getApplicationContext().getBean("eduSharingVersionCache");
	public static enum Type{
		REPOSITORY,
		RENDERSERVICE
	}
	private static String VERSION_FILE="version.html";
	public static String getVersionNoException(Type type){
		try {
			return getVersion(type);
		}catch(Exception e) {
			return "unknown";
		}
	}
	public static String getVersion(Type type) throws Exception{
		if(versionCache.getKeys().contains(type)){
			return versionCache.get(type);
		}
		String value;
		if(type.equals(Type.REPOSITORY)) {
			value=getRepositoryVersion();
		}else if(type.equals(Type.RENDERSERVICE)) {
			value=getRenderserviceVersion();
		}else {
			throw new IllegalArgumentException("Unknown type "+type);
		}
		versionCache.put(type,value);
		return value;
	}
	public static void invalidateCache(){
		versionCache.clear();
	}
	private static String getRenderserviceVersion() throws Exception{
		ApplicationInfo homeRepo=ApplicationInfoList.getHomeRepository();
		String url=homeRepo.getContentUrl();
		url=url.replace("index.php", "version.php");
		String data = new HttpQueryTool().query(url);
		return new JSONObject(data).getString("version");
	}
	private static String getRepositoryVersion() throws Exception{
		String versionString=getRepositoryVersionInfo();
		Matcher m=Pattern.compile("version\\.main:.?((\\d|\\.)*)").matcher(versionString);
		m.find();
		return m.group().split(":")[1].trim();
	}
	private static String getRepositoryVersionInfo() throws IOException {
		return FileUtils.readFileToString(new File(Context.getCurrentInstance().getRequest().getSession().getServletContext().getRealPath(VERSION_FILE)));
	}
}
