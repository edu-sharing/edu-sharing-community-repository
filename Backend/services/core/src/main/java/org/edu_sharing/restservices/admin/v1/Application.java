package org.edu_sharing.restservices.admin.v1;

import lombok.Data;
import org.edu_sharing.repository.server.tools.ApplicationInfo;

@Data
public class Application extends ApplicationSimple {
	private String webserverUrl;
	private String clientBaseUrl;
	private String repositoryType;
	private String xml;
	private String file;
	private String contentUrl;
	private String configUrl;

	public void fill(ApplicationInfo appInfo) {
		super.fill(appInfo);
		setWebserverUrl(appInfo.getWebServerUrl());
		setContentUrl(appInfo.getContentUrl());
		setClientBaseUrl(appInfo.getClientBaseUrl());
		setRepositoryType(appInfo.getRepositoryType());
		setXml(appInfo.getXml());
		setFile(appInfo.getAppFileName());
		if (ApplicationInfo.TYPE_RENDERSERVICE.equals(getType()) && getContentUrl() != null) {
			setConfigUrl(appInfo.getContentUrl().replace("/application/esmain/index.php", "/admin"));
		}
	}
}
