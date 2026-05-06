package org.edu_sharing.alfresco.service.config.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class Mainnav implements Serializable {
	@Schema(description = "Navigation icon configuration")
	@XmlElement public Icon icon;
	@Schema(description = "Main menu style customization")
	@XmlElement public String mainMenuStyle;
}
