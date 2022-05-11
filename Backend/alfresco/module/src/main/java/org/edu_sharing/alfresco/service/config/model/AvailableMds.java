package org.edu_sharing.alfresco.service.config.model;

import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class AvailableMds implements Serializable {
	@XmlElement public String repository;
	@XmlElement public String[] mds;
}
