package org.edu_sharing.alfresco.service.config.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlElement;

public class MenuEntry extends AbstractEntry{
	@Schema(description = "Internal path (e.g. 'workspace', 'collections?scope=EDU_ALL'). Used instead of url for internal navigation")
	@XmlElement public String path;
	@Schema(description = "Scope for highlighting (e.g. 'workspace')")
	@XmlElement public String scope;
}
