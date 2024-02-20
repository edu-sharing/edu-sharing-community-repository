package org.edu_sharing.alfresco.service.config.model;

import jakarta.xml.bind.annotation.XmlElement;

public class MenuEntry extends AbstractEntry{
	@XmlElement	public String path;
	@XmlElement	public String scope;
}
