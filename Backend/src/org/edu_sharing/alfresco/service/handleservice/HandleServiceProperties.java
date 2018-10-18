package org.edu_sharing.alfresco.service.handleservice;

import org.edu_sharing.repository.server.tools.PropertiesHelper;

public class HandleServiceProperties {

	String handleServerPrefix = null;

	String handleServerPrivKey = null;

	String handleServerEMail = null;
	
	String handleServerRepoId = null;

	public static final String KEY_HANDLESERVER_PREFIX = "handle_server_prefix";

	public static final String KEY_HANDLESERVER_PRIVKEY = "handle_server_privkey";

	public static final String KEY_HANDLESERVER_EMAIL = "handle_server_email";
	
	public static final String KEY_HANDLESERVER_REPOID = "handle_server_repoid";

	public static final String PROPERTY_FILE = "/org/edu_sharing/alfresco/service/handleservice/handleservice.properties";

	private HandleServiceProperties() {

		synchronized (HandleServiceProperties.class) {
			try {
				handleServerPrefix = PropertiesHelper.getProperty(KEY_HANDLESERVER_PREFIX, PROPERTY_FILE, PropertiesHelper.TEXT);
				handleServerPrivKey = PropertiesHelper.getProperty(KEY_HANDLESERVER_PRIVKEY, PROPERTY_FILE, PropertiesHelper.TEXT);
				handleServerEMail = PropertiesHelper.getProperty(KEY_HANDLESERVER_EMAIL, PROPERTY_FILE, PropertiesHelper.TEXT);
				handleServerRepoId = PropertiesHelper.getProperty(KEY_HANDLESERVER_REPOID, PROPERTY_FILE, PropertiesHelper.TEXT);
				if(handleServerRepoId == null) {
					handleServerRepoId = "";
				}
			
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
	
	public String getHandleServerRepoId() {
		return handleServerRepoId;
	}
	
	public static final HandleServiceProperties instance = new HandleServiceProperties();
}
