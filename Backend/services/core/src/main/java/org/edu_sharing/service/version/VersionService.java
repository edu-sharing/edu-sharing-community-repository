package org.edu_sharing.service.version;

import java.io.*;
import java.util.Arrays;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gson.Gson;
import org.alfresco.repo.cache.SimpleCache;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.restservices.about.v1.model.Licenses;
import org.edu_sharing.restservices.about.v1.model.Services;
import org.edu_sharing.service.authority.AuthorityServiceHelper;
import org.edu_sharing.service.rendering.RenderingServiceFactory;
import org.edu_sharing.service.rendering.RenderingVersionInfo;
import org.edu_sharing.spring.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

@Service
public class VersionService implements ApplicationListener<RefreshScopeRefreshedEvent> {
	static Logger logger = Logger.getLogger(VersionService.class);

	public SimpleCache<Type, String> versionCache = (SimpleCache<Type, String>) AlfAppContextGate.getApplicationContext().getBean("eduSharingVersionCache", SimpleCache.class);


	@Override
	public void onApplicationEvent(RefreshScopeRefreshedEvent event) {
		versionCache.clear();
	}


	public Licenses getLicenses() {
		try {
			Licenses licenses = new Licenses();
			String path = System.getProperty("catalina.base") + "/webapps/";
			try {
				for (File file :
						Stream.concat(
								Arrays.stream(new File(path + "alfresco/WEB-INF/licenses")
										.listFiles((dir, name) -> name.toLowerCase().endsWith(".txt"))),
								Arrays.stream(new File(path + "edu-sharing/WEB-INF/licenses")
										.listFiles((dir, name) -> name.toLowerCase().endsWith(".txt")
										))

						).collect(Collectors.toList())) {
					try (FileInputStream fis = new FileInputStream(file)) {
						licenses.getRepository().put(file.getName(), IOUtils.toString(fis));
					}
				}
			} catch(Throwable t) {
				logger.error(t.getMessage(), t);
			}
			try {
				licenses.getServices().put(
						Services.Rendering,
						RenderingServiceFactory.getLocalService().getVersion().licenses
				);
			} catch(Throwable t) {
				logger.error(t.getMessage(), t);
			}
			return cleanLicenses(licenses);
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	private Licenses cleanLicenses(Licenses licenses) {
		boolean isAdmin = false;
		try{
			isAdmin = AuthorityServiceHelper.isAdmin();
		} catch(Throwable ignored) { }
		LicenseDisclosure disclosure = LightbendConfigLoader.get().getEnum(LicenseDisclosure.class, "repository.disclosure.licenses");
		if(isAdmin || disclosure.equals(LicenseDisclosure.all)) {
			return licenses;
		}
		if(disclosure.equals(LicenseDisclosure.off)) {
			throw new SecurityException("license disclosure is disabled");
		}
		if(disclosure.equals(LicenseDisclosure.minimal)) {
			cleanupLicenseList(licenses.getRepository());
			licenses.getServices().forEach((key, value) -> {
				cleanupLicenseList(value);
			});
			return licenses;
		}
		throw new SecurityException("license disclosure mode is unknown");
	}

	private void cleanupLicenseList(Map<String, String> licenses) {
		licenses.forEach((key, value) -> {
			Pattern pattern = Pattern.compile("(\\(.*\\).*\\(.*)@([\\w|\\d|\\.]*)([\\s|\\)].*)");
			Matcher m = pattern.matcher(value);
			value = m.replaceAll("$1$3");
			pattern = Pattern.compile("(\\(.*\\).*\\(.*)\\:([\\w|\\d|\\.-]*)([\\s|\\)].*)");
			m = pattern.matcher(value);
			value = m.replaceAll("$1$3");
			licenses.put(key, value);
		});
	}

	public static enum Type{
		REPOSITORY,
		RENDERSERVICE
	}
	private String VERSION_FILE="version.json";
	public String getVersionNoException(Type type){
		try {
			return getVersion(type);
		}catch(Exception e) {
			return "unknown";
		}
	}
	public String getVersion(Type type) throws Exception{
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

	public void invalidateCache(){
		versionCache.clear();
	}
	private String getRenderserviceVersion() throws Exception{
		RenderingVersionInfo version = RenderingServiceFactory.getLocalService().getVersion();
		if(version != null) {
			return version.version;
		}
		return null;
	}
	private String getRepositoryVersion() throws Exception{
		RepositoryVersionInfo.Version version = getRepositoryVersionInfo().version;
		return version.major + "." + version.minor;
	}
	public RepositoryVersionInfo getRepositoryVersionInfo() throws IOException {
		return
				new Gson().fromJson(
						String.join(
                                "",
                                IOUtils.readLines(
                                        VersionService.class.getClassLoader().getResourceAsStream("version.json")
                                )
                        ),
						RepositoryVersionInfo.class
				);
	}
}
