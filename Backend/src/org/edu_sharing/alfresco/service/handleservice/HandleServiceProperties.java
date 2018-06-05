package org.edu_sharing.alfresco.service.handleservice;

import org.edu_sharing.repository.server.tools.PropertiesHelper;

public class HandleServiceProperties {

	String handleServerPrefix = null;

	String handleServerPrivKey = null;

	String handleServerEMail = null;

	public static final String KEY_HANDLESERVER_PREFIX = "handle_server_prefix";

	public static final String KEY_HANDLESERVER_PRIVKEY = "handle_server_privkey";

	public static final String KEY_HANDLESERVER_EMAIL = "handle_server_email";

	public static final String PROPERTY_FILE = "/org/edu_sharing/alfresco/service/handleservice/handleservice.properties";

	private HandleServiceProperties() {

		synchronized (HandleServiceProperties.class) {
			try {
				handleServerPrefix = PropertiesHelper.getProperty(KEY_HANDLESERVER_PREFIX, PROPERTY_FILE, PropertiesHelper.TEXT);
				handleServerPrivKey = PropertiesHelper.getProperty(KEY_HANDLESERVER_PRIVKEY, PROPERTY_FILE, PropertiesHelper.TEXT);
				handleServerEMail = PropertiesHelper.getProperty(KEY_HANDLESERVER_EMAIL, PROPERTY_FILE, PropertiesHelper.TEXT);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public String getHandleServerEMail() {
		return handleServerEMail;
	}
	
	public String getHandleServerPrefix() {
		return handleServerPrefix;
	}
	
	public String getHandleServerPrivKey() {
		return handleServerPrivKey;
	}
	
	public static final HandleServiceProperties instance = new HandleServiceProperties();
}
