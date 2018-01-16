package org.edu_sharing.service.config.model;
import java.io.Serializable;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Config {
	@XmlElement public Values values;
	@XmlElement public Contexts contexts;
}
