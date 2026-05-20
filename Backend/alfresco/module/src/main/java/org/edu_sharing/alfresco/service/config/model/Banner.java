package org.edu_sharing.alfresco.service.config.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class Banner implements Serializable {
	@Schema(description = "URL to banner image (fixed 150px height, wide width recommended)")
	@XmlElement public String url;
	@Schema(description = "Link to open on banner click")
	@XmlElement public String href;
	@Schema(description = "Components where banner should appear: 'search', 'render', 'collections'")
	@XmlElement public String[] components;
}
