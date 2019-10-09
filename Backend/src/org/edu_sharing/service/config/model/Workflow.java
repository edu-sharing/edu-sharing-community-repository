package org.edu_sharing.service.config.model;

import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class Workflow implements Serializable {
	@XmlElement	public String id;
	@XmlElement	public String color;
	@XmlElement	public Boolean hasReceiver;
	@XmlElement	public String[] next;
}
