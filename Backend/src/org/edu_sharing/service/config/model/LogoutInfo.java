package org.edu_sharing.service.config.model;

import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class LogoutInfo implements Serializable {
	@XmlElement public String url;
	@XmlElement public Boolean destroySession;
	@XmlElement public Boolean ajax;
	@XmlElement	public String next;
}
