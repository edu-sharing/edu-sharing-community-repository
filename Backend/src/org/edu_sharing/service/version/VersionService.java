package org.edu_sharing.service.version;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.chemistry.opencmis.commons.impl.IOUtils;
import org.apache.commons.io.FileUtils;
import org.edu_sharing.repository.server.authentication.Context;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.spring.ApplicationContextFactory;
import org.json.JSONObject;

import com.gargoylesoftware.htmlunit.util.UrlUtils;
import com.icegreen.greenmail.foedus.util.StreamUtils;

import io.swagger.jaxrs.utils.ReaderUtils;
import sun.net.util.URLUtil;


public class VersionService {
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
		if(type.equals(Type.REPOSITORY)) {
			return getRepositoryVersion();
		}
		if(type.equals(Type.RENDERSERVICE)) {
			return getRenderserviceVersion();
		}
		throw new IllegalArgumentException("Unknown type "+type);
	}
	private static String getRenderserviceVersion() throws Exception{
		ApplicationInfo rs=ApplicationInfoList.getRenderService();
		String url=rs.getContentUrlBackend()!=null ? rs.getContentUrlBackend() : rs.getContentUrl();
		url=url.replace("index.php", "version.php");
		InputStream is = new URL(url).openStream();
		String data=IOUtils.readAllLines(is);
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
