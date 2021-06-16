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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;
import java.util.logging.Logger;

public class PropertiesHelper {

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
				// don't cache:
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
		ClassLoader classLoader = PropertiesHelper.class.getClassLoader();
		if (_type.equals(XML)) {
			try {
				// don't cache:
				URL url = classLoader.getResource(_propertyFileName);

				InputStream is = url.openStream();
				props.loadFromXML(is);
				props.setProperty(_key, _value);
				props.storeToXML(new FileOutputStream(url.getFile()),
						" changed");
				is.close();
				success = true;
				
			} catch (InvalidPropertiesFormatException e) {
				StackTraceElement[] stackTraceEl = e.getStackTrace();
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

	public static boolean validatePropertyFile(String _propFile) {
		
		Properties props = new Properties();
		ClassLoader classLoader = PropertiesHelper.class.getClassLoader();
		boolean result = false;
		
		if (_propFile == null || _propFile.trim().equals(""))
			return false;
		try {
			// don't cache:

			URL url = classLoader.getResource(_propFile);

			InputStream is = url.openStream();
			props.load(is);
			is.close();
			result = true;

		} catch (InvalidPropertiesFormatException e) {
			return false;
		} catch (IOException e) {
			return false;
		} catch (Exception e) {
			return false;
		}
		
		return result;
	}

}
