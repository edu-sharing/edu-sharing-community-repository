package org.edu_sharing.service.config.model;
import org.apache.log4j.Logger;

import java.io.Serializable;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Config implements Serializable {

	static Logger logger=Logger.getLogger(Config.class);

	@XmlElement public Values values;
	@XmlElement public Contexts contexts;
	@XmlElement public List<Language> language;
	@XmlElement public Variables variables;
}
