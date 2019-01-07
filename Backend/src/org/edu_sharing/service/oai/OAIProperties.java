package org.edu_sharing.service.oai;

import org.apache.log4j.Logger;
import org.edu_sharing.repository.server.tools.PropertiesHelper;

public class OAIProperties {
	
	public static final String PROPERTY_FILE = "/org/edu_sharing/service/oai/oai.properties";
	
	public static String OAI_PATH = "oai.path";
	String oaiPath = null;
	
	Logger logger = Logger.getLogger(OAIProperties.class);
	
	private OAIProperties() {
		synchronized (OAIProperties.class) {
			try {
				oaiPath = PropertiesHelper.getProperty(OAI_PATH, PROPERTY_FILE, PropertiesHelper.TEXT);
			} catch (Exception e) {
				logger.debug("could not load " + PROPERTY_FILE);
			}
		}
	}
	
	public String getOaiPath() {
		return oaiPath;
	}
	
	
	public static OAIProperties instance = new OAIProperties();
}
