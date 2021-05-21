package org.edu_sharing.alfresco.service.config.model;
import java.io.Serializable;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;

public class Variables implements Serializable {
	@XmlElement public List<KeyValuePair> variable;
}
