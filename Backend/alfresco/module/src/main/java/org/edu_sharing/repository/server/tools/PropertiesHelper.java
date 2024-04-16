/**
 *
 *  
 * 
 * 
 *	
 *
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 *
 */
package org.edu_sharing.repository.server.tools;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.logging.Logger;

public class PropertiesHelper {

	public static class Config {
		public enum PathPrefix {
			NODE("node"),
			CLUSTER_PROPERTIES("cluster/applications"),
			CLUSTER("cluster"),
			DEFAULTS("defaults"),
			DEFAULTS_METADATASETS("defaults/metadatasets"),
			DEFAULTS_MAILTEMPLATES("defaults/mailtemplates"),
			DEFAULTS_DATABASE("defaults/database");

			private final String path;

			PathPrefix(String path) {
				this.path = path;
			}
			@Override
			public String toString() {
				return path;
			}
		}
		public static String CONFIG_FILENAME = "client.config.xml";
		public static String PATH_CONFIG = "config/";
		public static String getPropertyFilePath(String propertyFile) {
			return PATH_CONFIG + PathPrefix.CLUSTER_PROPERTIES + "/" +
					propertyFile;
		}
		public static URLClassLoader getClassLoaderForPath(String configPath) {
			File file = new File( getAbsolutePathForConfigFile(configPath));
			URL[] urls;
			try {
				urls = new URL[]{file.getParentFile().toURI().toURL()};
				return new URLClassLoader(urls);
			} catch (MalformedURLException e) {
				throw new RuntimeException(e);
			}
		}

		public static String getAbsolutePathForConfigFile(String configFile) {
			return System.getProperty("catalina.base") + "/shared/classes/" + configFile;
		}

		public static ResourceBundle getResourceBundleForFile(String configFile) throws IOException {
			return new PropertyResourceBundle(getInputStreamForFile(configFile));
		}

		public static InputStream getInputStreamForFile(String configFile) throws FileNotFoundException {
			return new FileInputStream(System.getProperty("catalina.base") + "/shared/classes/" + configFile);
		}
	}

	final public static String EXCEPTION_UNHANDLED_TYPE = "Unhandled type given.";
	public static final String XML = "xml";
	public static final String TEXT = "text";
	
	static Logger logger = Logger.getLogger(PropertiesHelper.class.getName());

	public static Properties getProperties(String propertyFile, String type)
			throws Exception {

		Properties props = new Properties();
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		if (type.equals(XML)) {
			try {
				// propertyFile = getPropertyFilePath(propertyFile);
				URL url = classLoader.getResource(propertyFile);

				if (url == null) {
					throw new Exception("Could Not Find Ressource:"
							+ propertyFile);
				}

				InputStream is = url.openStream();
				props.loadFromXML(is);
				is.close();

			} catch (InvalidPropertiesFormatException e) {
				logger.info(e.getMessage());
			} catch (IOException e) {
				logger.info(e.getMessage());
			}
			
			return props;
		}
		else if (type.equals(TEXT)) {

			try {
				// propertyFile = getPropertyFilePath(propertyFile);
				URL url = classLoader.getResource(propertyFile);
				InputStream is = url.openStream();
				props.load(is);
				is.close();
				
			} catch (InvalidPropertiesFormatException e) {
				logger.info(e.getMessage());
			} catch (IOException e) {
				logger.info(e.getMessage());
			} catch (Exception e) {
				logger.info(e.getMessage());
			}
			return props;
		} else {
			throw new Exception(EXCEPTION_UNHANDLED_TYPE);
		}
	}

	public static String getProperty(String _key, String _propertyFileName,
			String _type) throws Exception {
		Properties props = getProperties(_propertyFileName, _type);
		return props.getProperty(_key);
	}

	public static boolean setProperty(String _key, String _value,
			String _propertyFileName, String _type) {
		boolean success = false;
		Properties props = new Properties();
		if (_type.equals(XML)) {
			try {
				InputStream is = new FileInputStream(_propertyFileName);
				props.loadFromXML(is);
				props.setProperty(_key, _value);
				props.storeToXML(new FileOutputStream(_propertyFileName),
						" changed");
				is.close();
				success = true;
				
			} catch (InvalidPropertiesFormatException e) {
				e.printStackTrace();
			} catch (IOException e) {

				e.printStackTrace();
			}
		}
		return success;
	}

	public static <T> ArrayList<T> addToRecentProperty(T element, ArrayList<T> list, int limit) {
		list.remove(element);
		list.add(0, element);
		while(list.size()>limit){
			list.remove(limit-1);
		}
		return list;
	}
}
