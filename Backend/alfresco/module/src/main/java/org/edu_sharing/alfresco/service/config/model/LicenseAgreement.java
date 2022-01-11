package org.edu_sharing.alfresco.service.config.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlValue;
import java.io.Serializable;

public class LicenseAgreement implements Serializable {
	@XmlAccessorType(XmlAccessType.FIELD)
	private static class LicenseAgreementNode implements Serializable{
		@XmlAttribute public String language;
		@XmlValue public String value;
	}
	@XmlElement	public LicenseAgreementNode[] nodeId;
}
