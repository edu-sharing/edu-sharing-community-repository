package org.edu_sharing.alfresco.service.config.model;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlValue;
import java.io.Serializable;

public class KeyValuePair implements Serializable {
	@XmlAttribute public java.lang.String key;
	@XmlValue public java.lang.String value;
	@Override
	public boolean equals(Object obj) {
		return ((KeyValuePair)obj).key.equals(key);
	}
}
