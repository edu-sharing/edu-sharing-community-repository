package org.edu_sharing.service.config.model;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlValue;

public class Language{
	@XmlAttribute public java.lang.String language;
	@XmlElement public List<Language.String> string;
	public static class String{
		@XmlAttribute public java.lang.String key;
		@XmlValue public java.lang.String value;
		@Override
		public boolean equals(Object obj) {
			return ((String)obj).key.equals(key);
		}
		
	}
}
