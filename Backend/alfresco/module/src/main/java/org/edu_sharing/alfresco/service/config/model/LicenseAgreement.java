package org.edu_sharing.alfresco.service.config.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlValue;
import java.io.Serializable;

public class LicenseAgreement implements Serializable {
	@XmlAccessorType(XmlAccessType.FIELD)
	private static class LicenseAgreementNode implements Serializable{
		@XmlAttribute public String language;
		@XmlValue public String value;
	}
	@XmlElement	public LicenseAgreementNode[] nodeId;
}
