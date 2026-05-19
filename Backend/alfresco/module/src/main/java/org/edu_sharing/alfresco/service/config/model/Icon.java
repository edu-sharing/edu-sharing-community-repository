package org.edu_sharing.alfresco.service.config.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class Icon implements Serializable {
	@Schema(description = "Icon URL (recommended size: 35x27)")
	@XmlElement public String url;
}
