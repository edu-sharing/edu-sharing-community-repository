package org.edu_sharing.repository.server.tools;

public class Edu_SharingProperties {
	
	public static final String KEY_EXPORT_METADATA = "export_metadata";
	
	public static final String KEY_HELP_URL_CC = "help_url_cc";
	
	public static final String KEY_HELP_URL_ES = "help_url_es";
	
	public static final String KEY_HELP_URL_CUSTOM = "help_url_custom";
	
	public static final String KEY_HELP_URL_SHARE = "help_url_share";
	
	public static final String KEY_FUZZY_USERSEARCH = "fuzzy_usersearch";

	public static final String PROPERTY_FILE = "edu-sharing.properties";
	
	
	String exportMetdata = null;
	
	String helpUrlCC = null;
	
	String helpUrlES = null;
	
	String helpUrlCustom = null;
	
	String helpUrlShare = null;
	
	boolean fuzzyUserSearch = false;
	
	private  Edu_SharingProperties() {
		
		synchronized(Edu_SharingProperties.class){
			try{
				exportMetdata = PropertiesHelper.getProperty(KEY_EXPORT_METADATA, PROPERTY_FILE, PropertiesHelper.TEXT);
				helpUrlCC = PropertiesHelper.getProperty(KEY_HELP_URL_CC, PROPERTY_FILE, PropertiesHelper.TEXT);
				helpUrlES = PropertiesHelper.getProperty(KEY_HELP_URL_ES, PROPERTY_FILE, PropertiesHelper.TEXT);
				helpUrlCustom = PropertiesHelper.getProperty(KEY_HELP_URL_CUSTOM, PROPERTY_FILE, PropertiesHelper.TEXT);
				helpUrlShare = PropertiesHelper.getProperty(KEY_HELP_URL_SHARE, PROPERTY_FILE, PropertiesHelper.TEXT);
				fuzzyUserSearch = Boolean.parseBoolean(PropertiesHelper.getProperty(KEY_FUZZY_USERSEARCH, PROPERTY_FILE, PropertiesHelper.TEXT));
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}

	public String getExportMetdata() {
		return exportMetdata;
	}

	public String getHelpUrlCC() {
		return helpUrlCC;
	}

	
	public String getHelpUrlES() {
		return helpUrlES;
	}

	public String getHelpUrlCustom() {
		return helpUrlCustom;
	}
	
	public String getHelpUrlShare() {
		return helpUrlShare;
	}
	
	public boolean isFuzzyUserSearch() {
		return fuzzyUserSearch;
	}
	
	public static final Edu_SharingProperties instance = new Edu_SharingProperties();
	
}
