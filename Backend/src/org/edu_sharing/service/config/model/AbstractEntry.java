package org.edu_sharing.service.config.model;

import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class AbstractEntry implements Serializable {
	@XmlElement public Integer position;
	@XmlElement public String icon;
	@XmlElement public String name;
	@XmlElement	public String url;
	@XmlElement	public Boolean isDisabled;
	@XmlElement public Boolean isSeperate;
	@XmlElement public Boolean isSeperateBottom;
	@XmlElement public Boolean onlyDesktop;
}