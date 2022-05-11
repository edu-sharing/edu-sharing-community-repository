package org.edu_sharing.alfresco.authentication;

public class HttpContext {

	static ThreadLocal<String> currentMetadataSet = new ThreadLocal<String>();
	
	
	public static void setCurrentMetadataSet(String currentMetadataSet) {
		HttpContext.currentMetadataSet.set(currentMetadataSet);
	}
	
	public static String getCurrentMetadataSet() {
		return currentMetadataSet.get();
	}
}
