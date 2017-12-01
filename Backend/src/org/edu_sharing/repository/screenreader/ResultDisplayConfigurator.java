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
package org.edu_sharing.repository.screenreader;

import java.util.HashMap;
import java.util.Properties;

import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.PropertiesHelper;


/**
 * Helps to config what metadata should be displayed on JSP search result listing
 * @author Christian
 */
public class ResultDisplayConfigurator {
	
	// contains wich metadata should be dipslay and at which position to be displayed
	private HashMap<String, Integer> metadataConfig 		= new HashMap<String, Integer>();
	private HashMap<Integer, String> metadataConfigReversed = new HashMap<Integer, String>();	
	
	// buffers all the HTML data before all parts get build together in order 
	private HashMap<Integer,String> htmlFragmentsBuffer = new HashMap<Integer, String>();

	private int itemCounter = 0;
	
	private String lastTitle = "";
	
	/**
	 * Create just once per search result displaying and reuse for every result item
	 */
	public ResultDisplayConfigurator() {
				
		// check for special config
		boolean setDefault = false;
		try {
			Properties props = PropertiesHelper.getProperties(Const.PROPERTIES_FILE,PropertiesHelper.TEXT);
		
			boolean checkForMore=true;
			int counter = 0;
			while (checkForMore) {
				counter++;
				String val = props.getProperty("POS"+counter);
				if ((val!=null) && (val.trim().length()>0)) {
					this.addMetadataItemToConfig(val.trim(), counter);
				} else {
					checkForMore = false;
				}
			}
			
			if (counter==1) {
				setDefault=true;
				System.err.println("ResultDisplayConfigurator: Was not able to load 'PREVIEW DATA ON SERACH RESULTS' from '"+Const.PROPERTIES_FILE+"'");							
			}
		
		} catch (Exception e) {
			System.err.println("ResultDisplayConfigurator: Was not able to load properties from '"+Const.PROPERTIES_FILE+"'");			
			setDefault = true;
		}
		
		// set default metadata config
		if (setDefault) {
			this.addMetadataItemToConfig(CCConstants.LOM_PROP_GENERAL_TITLE, 1);
			this.addMetadataItemToConfig(CCConstants.LOM_PROP_GENERAL_DESCRIPTION, 2);
			this.addMetadataItemToConfig(CCConstants.LOM_PROP_TECHNICAL_FORMAT, 3);	
			this.addMetadataItemToConfig(CCConstants.CM_PROP_C_MODIFIED, 4);
			System.out.println("ResultDisplayConfigurator: Default search preview set.");
		}
		
	}
	
	private void addMetadataItemToConfig(String key, int pos) {
		this.metadataConfig.put(key, new Integer(pos));
		this.metadataConfigReversed.put(new Integer(pos), key);
		this.itemCounter++;
	}
	
	/**
	 * Add HTML redering of an item to fragment buffer.
	 * All fragments will be later put together in right order.
	 * @param key	of Metadata CCConstant
	 * @param html	the html rendering
	 * @return	false if item is not set to be in result config
	 */
	public boolean addHtmlViewOfItem(String key, String html, HashMap<String, Object> orgData) {
		
		// check if item is in result config
		if (!this.metadataConfig.containsKey(key)) return false;
		
		// store orgData if this is the title of item
		if (this.metadataConfigReversed.get(new Integer(1))==null) {
			System.err.println("ResultDisplayConfigurator: Not first metadata element as title set - CHECK CONFIGURATION.");
			return false;
		}
		if (this.metadataConfigReversed.get(new Integer(1)).equals(key)){
			try {
				this.lastTitle = (String) orgData.get(key);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		// store in buffer
		Integer position = this.metadataConfig.get(key);
		this.htmlFragmentsBuffer.put(position, html);
		return true;
	}
	
	/**
	 * Returns the contructed HTML of all item rendering is correct order
	 * @param resultNumberingOnHeadline numnbering String on result item headline (null=ignore)
	 * @return
	 */
	public String constructResultItemHTML(String resultNumberingOnHeadline) {
		
		// prepare con
		String counterInformation = "";
		if (resultNumberingOnHeadline!=null) {
			counterInformation = resultNumberingOnHeadline+". ";
		}
		
		/*
		 * NOTE ABOUT HTML THAT GETS PRODUCED:
		 * Its structural HTML ... to format layout use CSS styling.
		 * All metadata elements are in divs with specifig classes.
		 * This classes can be used for individual format styling.
		 */
		String resultHTML = "";
		for (int p=1; p<=this.itemCounter; p++) {
			
			Integer position = new Integer(p);
			if (this.htmlFragmentsBuffer.containsKey(position)) {
				if (p==1) {
					resultHTML += "\n<h2 class='result_headline'>"+counterInformation+this.htmlFragmentsBuffer.get(position)+"</h2>";
				} else {
					resultHTML += "\n"+"<div class='result_"+JspTools.justChars(this.getKeyByPosition(p))+"'>"+this.htmlFragmentsBuffer.get(position)+"</div>";
				}
			} else {
				resultHTML += "\n<!-- missing meta data on item for "+this.getKeyByPosition(p)+" -->";
			}
			
		}
		
		this.htmlFragmentsBuffer.clear();
		return resultHTML;
	}
	
	/**
	 * Returns the title String that was set for last item
	 * @return
	 */
	public String getTitle() {
		return this.lastTitle;
	}
	
	private String getKeyByPosition(int position) {
		String result = this.metadataConfigReversed.get(new Integer(position));
		if (result==null) result = "(POSITION HAS NO KEY)";
		return result;
	}
	
}
