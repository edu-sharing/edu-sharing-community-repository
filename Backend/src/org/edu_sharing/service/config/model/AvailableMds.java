package org.edu_sharing.service.config.model;

import javax.xml.bind.annotation.XmlElement;

public class AvailableMds {
	@XmlElement String repository;
	@XmlElement String[] mds;
}
