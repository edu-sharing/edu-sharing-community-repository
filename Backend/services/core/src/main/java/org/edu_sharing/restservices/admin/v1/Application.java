package org.edu_sharing.restservices.admin.v1;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
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
		if (getContentUrl() != null) {
			if (ApplicationInfo.TYPE_RENDERSERVICE.equals(getType())) {
				setConfigUrl(getContentUrl().replace("/application/esmain/index.php", "/admin"));
			} else if (ApplicationInfo.TYPE_RENDERSERVICE_2.equals(getType())) {
				// rendering service 2: the content url is the base url of the service
				setConfigUrl(StringUtils.removeEnd(getContentUrl(), "/rendering") + "/rendering-admin");
			}
		}
	}
}
