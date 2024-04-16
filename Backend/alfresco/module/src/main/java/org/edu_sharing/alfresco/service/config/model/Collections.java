package org.edu_sharing.alfresco.service.config.model;

import jakarta.xml.bind.annotation.XmlElement;
import java.io.Serializable;

public class Collections implements Serializable {
	@XmlElement	public String[] colors;
}
