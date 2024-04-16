package org.edu_sharing.alfresco.service.config.model;

import jakarta.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class Banner implements Serializable {
	@XmlElement	public String url;
	@XmlElement	public String href;
	@XmlElement	public String[] components;
}
