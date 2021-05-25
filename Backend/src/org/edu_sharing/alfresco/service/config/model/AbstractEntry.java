package org.edu_sharing.alfresco.service.config.model;

import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class AbstractEntry implements Serializable {
	@XmlElement public Integer position;
	@XmlElement public String icon;
	@XmlElement public String name;
	@XmlElement	public String url;
	@XmlElement	public Boolean isDisabled;
	@XmlElement public Boolean openInNew;
	@XmlElement public Boolean isSeparate;
	@XmlElement public Boolean isSeparateBottom;
	@XmlElement public Boolean onlyDesktop;
	@XmlElement public Boolean onlyWeb;
}