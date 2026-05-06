package org.edu_sharing.alfresco.service.config.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class LogoutInfo implements Serializable {
	@Schema(description = "URL to navigate to on logout")
	@XmlElement public String url;
	@Schema(description = "URL for local users (overrides url if set)")
	@XmlElement public String localUrl;
	@Schema(description = "URL for Shibboleth/SSO users (overrides url if set)")
	@XmlElement public String ssoUrl;
	@Schema(description = "If true, destroy the edu-sharing session before navigating to URL")
	@XmlElement public Boolean destroySession;
	@Schema(description = "If true, call URL via AJAX; if false, navigate via browser")
	@XmlElement public Boolean ajax;
	@Schema(description = "URL to navigate to after AJAX call completes (only if ajax=true)")
	@XmlElement public String next;
}
