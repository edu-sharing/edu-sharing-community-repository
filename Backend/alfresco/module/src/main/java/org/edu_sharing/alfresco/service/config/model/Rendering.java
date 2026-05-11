package org.edu_sharing.alfresco.service.config.model;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class Rendering implements Serializable {
	@Schema(description = "If true (default), show preview area with image on gray background")
	@XmlElement public Boolean showPreview;
	@Schema(description = "If true (default: false), show download button in preview area")
	@XmlElement public Boolean showDownloadButton;
	@Schema(description = "If true (default), prerender files automatically after upload (DEPRECATED: configure in backend)")
	@XmlElement public Boolean prerender;
	@Schema(description = "GDPR configuration for rendering privacy")
	@XmlElement public RenderingGdpr[] gdpr;

	public static class RenderingGdpr implements Serializable {
		@Schema(description = "Pattern to match against file types")
		@XmlElement public String matcher;
		@Schema(description = "Display name for privacy notice")
		@XmlElement public String name;
		@Schema(description = "URL to privacy information")
		@XmlElement public String privacyInformationUrl;
	}
}
