package org.edu_sharing.alfresco.service.config.model;
import java.io.Serializable;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;

public class Language implements Serializable {
	@XmlAttribute public java.lang.String language;
	@XmlElement public List<KeyValuePair> string;

}
