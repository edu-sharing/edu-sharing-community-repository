package org.edu_sharing.service.config.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlValue;

public class LicenseAgreement {
	@XmlAccessorType(XmlAccessType.FIELD)
	private static class Node{
		@XmlAttribute public String language;
		@XmlValue public String value;
	}
	@XmlElement	public Node[] nodeId;
}
