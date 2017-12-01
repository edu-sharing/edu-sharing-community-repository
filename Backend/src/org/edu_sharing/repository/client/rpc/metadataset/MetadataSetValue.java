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
package org.edu_sharing.repository.client.rpc.metadataset;

import java.util.HashMap;

import org.edu_sharing.repository.client.tools.CCConstants;


/**
 * @author rudolph
 */
public class MetadataSetValue implements com.google.gwt.user.client.rpc.IsSerializable {
	
	String key;
	
	/**
	 * for example:
	 * <de_DE,Haus>
	 * <en_EN,House>
	 */
	HashMap<String,String> i18n;
	
	
	String caption = null;
	
	/**
	 * 
	 */
	public MetadataSetValue() {
	}

	/**
	 * @return the key
	 */
	public String getKey() {
		return key;
	}

	/**
	 * @param key the key to set
	 */
	public void setKey(String key) {
		this.key = key;
	}

	/**
	 * @return the i18n
	 */
	public HashMap<String, String> getI18n() {
		return i18n;
	}

	/**
	 * @param i18n the i18n to set
	 */
	public void setI18n(HashMap<String, String> i18n) {
		this.i18n = i18n;
	}
	
	
	public String getValue(String locale){
		String result = null;
		HashMap<String, String>  i18nLabel = getI18n();
		if(i18nLabel != null){
			result = i18nLabel.get(locale);
			if(result == null){
				result = i18nLabel.get(CCConstants.defaultLocale);
			}
		}
		
		if(result == null && this.caption != null && !this.caption.trim().equals("")){
			result = this.caption;
		}
		
		if(result == null){
			result = this.key;
		}
		return result;
	}
	
	public String getCaption() {
		return caption;
	}

	public void setCaption(String caption) {
		this.caption = caption;
	}
	
}
