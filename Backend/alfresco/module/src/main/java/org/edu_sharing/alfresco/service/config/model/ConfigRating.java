package org.edu_sharing.alfresco.service.config.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class ConfigRating implements Serializable {
	@Schema(description = "Rating display mode: none (disabled), likes (like button), or stars (star rating)")
	public enum RatingMode {
		@Schema(description = "Rating feature disabled")
		none,
		@Schema(description = "Like button for quick feedback")
		likes,
		@Schema(description = "Star rating system")
		stars,
	}
	@Schema(description = "Rating mode configuration")
	@XmlElement
	public RatingMode mode;
}
