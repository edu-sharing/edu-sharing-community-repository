package org.edu_sharing.alfresco.service.config.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class AbstractEntry implements Serializable {
	@Schema(description = "Position in menu (negative = count from end)")
	@XmlElement public Integer position;
	@Schema(description = "Material Design icon identifier")
	@XmlElement public String icon;
	@Schema(description = "Display name (can include language translation strings)")
	@XmlElement public String name;
	@Schema(description = "URL to open on click")
	@XmlElement public String url;
	@Schema(description = "If true, display grayed out with no function")
	@XmlElement public Boolean isDisabled;
	@Schema(description = "If true (default), open link in new tab")
	@XmlElement public Boolean openInNew;
	@Schema(description = "If true, separate with line above")
	@XmlElement public Boolean isSeparate;
	@Schema(description = "If true, separate with line below")
	@XmlElement public Boolean isSeparateBottom;
	@Schema(description = "If true, only visible on desktop")
	@XmlElement public Boolean onlyDesktop;
	@Schema(description = "If true (default: false), hide in Cordova apps")
	@XmlElement public Boolean onlyWeb;
}