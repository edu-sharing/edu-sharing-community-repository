package org.edu_sharing.service.oai;

import org.edu_sharing.repository.server.tools.PropertiesHelper;

public class OAIProperties {
	
	public static final String PROPERTY_FILE = "/org/edu_sharing/service/oai/oai.properties";
	
	public static String OAI_PATH = "oai.path";
	String oaiPath = null;
	
	
	private OAIProperties() {
		synchronized (OAIProperties.class) {
			try {
				oaiPath = PropertiesHelper.getProperty(OAI_PATH, PROPERTY_FILE, PropertiesHelper.TEXT);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public String getOaiPath() {
		return oaiPath;
	}
	
	
	public static OAIProperties instance = new OAIProperties();
}
