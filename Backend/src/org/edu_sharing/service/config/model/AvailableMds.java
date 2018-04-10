package org.edu_sharing.service.config.model;

import javax.xml.bind.annotation.XmlElement;

public class AvailableMds {
	@XmlElement public String repository;
	@XmlElement public String[] mds;
}
