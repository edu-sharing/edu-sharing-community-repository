package org.edu_sharing.service.config.model;

import javax.xml.bind.annotation.XmlElement;

public class ContextMenuEntry extends AbstractEntry{
	@XmlElement public String mode;
	@XmlElement public Boolean ajax;
	@XmlElement	public String permission;
	@XmlElement	public Boolean isDirectory;
	@XmlElement	public Boolean multiple;
	@XmlElement	public Boolean remove;
}
