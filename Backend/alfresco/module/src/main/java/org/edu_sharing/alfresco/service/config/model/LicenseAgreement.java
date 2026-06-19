package org.edu_sharing.alfresco.service.config.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.*;

import java.io.Serializable;

public class LicenseAgreement implements Serializable {
	@XmlAccessorType(XmlAccessType.FIELD)
	private static class LicenseAgreementNode implements Serializable{
		@Schema(description = "Language code for this agreement entry")
		@XmlAttribute public String language;
		@Schema(description = "HTML content of the license agreement")
		@XmlValue public String value;
	}
	@Schema(description = "Array of license agreement entries (one per language, with fallback)")
	@XmlElement public LicenseAgreementNode[] nodeId;
}
