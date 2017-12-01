package org.edu_sharing.repository.server.tools;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

/**
 * Tool class for supporting color schemes.
 * 
 * to load alternative colorscheme configuration
 * 
 * @author rotzoll
 */
public class ColorSchemeTool {

	public static final String COOKIE_KEY = "colorschemeid";
	
	private static final String CONFFILE_PATH = "/../../themes/default/";
	private static final String CONFFILE_PREFIX = "colorscheme";
	private static final String CONFFILE_SEPERATOR = "_";
	private static final String CONFFILE_SUFFIX = ".properties.xml";
	
	public class ColorSchemeData {
		
		public String logoURL;
		public String userCSS;
		
		public String colorActionOne;
		public String colorActionTwo;
		public String buttonTextColor;
		
		public String topAreaBackground;
		public String topButtonBackground;
		public String topButtonToggleBackground;
		public String topButtonText;
		
	}
	
	public ColorSchemeData loadColorScheme(String id) {
				
		ColorSchemeData result = new ColorSchemeData();
		
		// build path
		String webAppPath = this.getClass().getClassLoader().getResource("").getPath();
		String configFileStr = webAppPath + CONFFILE_PATH + CONFFILE_PREFIX + CONFFILE_SEPERATOR + id + CONFFILE_SUFFIX;
		
		// maybe fall back to default
		if ((id==null) || ("null".equals(id)) || (!(new File(configFileStr)).exists())) {
			if ((id!=null) && (!"null".equals(id))) System.err.println("ColorSchemeTool: File for given ID("+id+") does not exist ... fallback to default --> "+configFileStr);
			configFileStr = webAppPath + CONFFILE_PATH + CONFFILE_PREFIX + CONFFILE_SUFFIX;
		}
		
		if ((new File(configFileStr)).exists()) {
			
			try {
				Properties prop = new Properties();
				FileInputStream fis = new FileInputStream(configFileStr);
				prop.loadFromXML(fis);
				result = readOutProperties(prop);
			} catch (Exception e) {
				System.err.println("ColorSchemeTool: Exception on loading properties file: "+configFileStr);
				e.printStackTrace();
			}
			
		} else {
			System.err.println("ColorSchemeTool: Default config file is missing: "+configFileStr);
		}

		return result;
	}
	
	public String getColorSchemeIdFromCookie(HttpServletRequest request) {
		String colorSchemeId = null;
		Cookie cookie = null;
		Cookie[] cookies = null;
		cookies = request.getCookies();
		if( cookies != null ){
			for (int i = 0; i < cookies.length; i++){
		    	cookie = cookies[i];
		        if (COOKIE_KEY.equals(cookie.getName())) {
		        	 colorSchemeId = cookie.getValue();
		        }
		    }
		}
		return colorSchemeId;
	}

	private ColorSchemeData readOutProperties(Properties prop) {
		
		ColorSchemeData result = new ColorSchemeData();
		
		String defaultLogo = "/edu-sharing/images/logos/edu_sharing_com.svg";
		result.logoURL = prop.getProperty("logoURL", defaultLogo);
		if ((result.logoURL==null) || ("null".equals(result.logoURL))) result.logoURL = defaultLogo;
		
		result.userCSS = prop.getProperty("userCSS", null);
		result.colorActionOne = prop.getProperty("colorActionOne", "#296ADF");
		result.colorActionTwo = prop.getProperty("colorActionTwo", "#5585DB");
		result.buttonTextColor = prop.getProperty("buttonTextColor", "white");
		
		result.topAreaBackground = prop.getProperty("topAreaBackground", "#666666");
		result.topButtonBackground = prop.getProperty("topButtonBackground", "#296ADF");
		result.topButtonToggleBackground = prop.getProperty("topButtonToggleBackground", "#4c4c4c");
		result.topButtonText = prop.getProperty("topButtonText", "white");
			
		return result;
	}
	
}
