package org.edu_sharing.service.config.model;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlValue;

public class Variables{
	@XmlElement public List<KeyValuePair> variable;
}
