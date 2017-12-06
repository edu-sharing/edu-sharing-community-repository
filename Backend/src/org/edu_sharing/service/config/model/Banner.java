package org.edu_sharing.service.config.model;

import javax.xml.bind.annotation.XmlElement;

public class Banner {
	@XmlElement	public String url;
	@XmlElement	public String[] components;
}
