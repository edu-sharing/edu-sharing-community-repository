package org.edu_sharing.service.config.model;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlValue;

public class Variables implements Serializable {
	@XmlElement public List<KeyValuePair> variable;
}
