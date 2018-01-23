package org.edu_sharing.service.config.model;
import java.io.Serializable;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Config {
	@XmlElement public Values values;
	@XmlElement public Contexts contexts;
	@XmlElement public List<Language> language;
	@XmlElement public Variables variables;
}
