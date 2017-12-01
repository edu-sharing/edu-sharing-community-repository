package org.edu_sharing.repository.client.tools;

import com.google.gwt.core.client.GWT;

/*
 * How to use Theme class data in UI Binder:
 * http://stackoverflow.com/questions/15483551/gwt-how-to-use-constants-in-uibinder-template
 */
public class Theme {

	
	private static String themeId = "default";

	public static String getThemeId() {
		return themeId;
	}

	public static void setThemeId(String themeId) {
		Theme.themeId = themeId;
	}
	
	
	public static String getImagesPath(){
		return GWT.getModuleBaseURL()+"themes/"+ Theme.getThemeId()+"/images/";
	}
	
	
	public static String getImagesPathActions16(){
		return getImagesPath() + "common/actions/16px/";
	}
	
}
