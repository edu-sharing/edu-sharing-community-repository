package org.edu_sharing.alfresco.service.config.model;

import javax.xml.bind.annotation.XmlElement;

public class MenuEntry extends AbstractEntry{
	@XmlElement	public String path;
	@XmlElement	public String scope;
}
