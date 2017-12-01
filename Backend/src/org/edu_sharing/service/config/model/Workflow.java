package org.edu_sharing.service.config.model;

import javax.xml.bind.annotation.XmlElement;

public class Workflow {
	@XmlElement	public String id;
	@XmlElement	public String color;
	@XmlElement	public Boolean hasReceiver;
	@XmlElement	public String[] next;
}
