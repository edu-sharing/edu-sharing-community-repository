package org.edu_sharing.alfresco.service.config.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class AvailableMds implements Serializable {
	@Schema(description = "Repository ID ('-home-' for local, or app-id from properties file)")
	@XmlElement public String repository;
	@Schema(description = "Array of allowed metadata set IDs for this repository")
	@XmlElement public String[] mds;
}
