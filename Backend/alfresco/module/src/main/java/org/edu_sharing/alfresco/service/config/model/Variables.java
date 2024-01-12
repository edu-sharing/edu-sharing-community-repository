package org.edu_sharing.alfresco.service.config.model;
import java.io.Serializable;
import java.util.List;

import jakarta.xml.bind.annotation.XmlElement;

public class Variables implements Serializable {
	@XmlElement public List<KeyValuePair> variable;
}
