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
 * Helps to config what metadata should be displayed on JSP detail view
 * @author Christian
 */
public class DetailDisplayConfigurator {
	
	private static final String SECTION 				= "SECTION";
	private static final String POS 					= "POS";
	private static final String HEADLINE_TEXT 			= "_HEADLINE_TEXT";	
	private static final String SECTIONX_KEY 			= "0";
	private static final String TITLE_KEY				= "TITLE";
	private static final String SECTIONX_HEADLINE_TEXT = "SECTIONX_HEADLINE_TEXT";
	
	// contains wich metadata should be dipslay and at which position to be displayed
	// item key --> position key
	private HashMap<String, String> metadataConfig 		= new HashMap<String, String>();
	// position key --> item Key
	private HashMap<String, String> metadataConfigReversed = new HashMap<String, String>();
	
	// speacial detail view items
	// section key --> headline text
	private HashMap<String, String> sectionHeadlines = new HashMap<String, String>();
	private String titleElementItemKey = "";
 	
	// buffers all the HTML data before all parts get build together in order 
	// position key --> HTML String
	private HashMap<String,String> htmlFragmentsBuffer = new HashMap<String, String>();

	private int itemCounterSectionX = 0;
	
	private String lastTitle = "";
	
	/**
	 * Create just once per search result displaying and reuse for every result item
	 */
	public DetailDisplayConfigurator() {
				
		// check for special config
		boolean setDefaultConfig = false;
		try {
			Properties props = PropertiesHelper.getProperties(Const.PROPERTIES_FILE,PropertiesHelper.TEXT);
		
			// ########## title element item key
			this.titleElementItemKey = props.getProperty("TITLE_ELEMENT");
			if (this.titleElementItemKey==null) titleElementItemKey = "";
			
			// ########## check for sections
			boolean checkForMoreSections = true;
			int sectionCounter = 0;
			while (checkForMoreSections) {
				sectionCounter++;
				
				// check if next section is activated and get headline
				String val = props.getProperty(SECTION+sectionCounter+HEADLINE_TEXT);
				if ((val!=null) && (val.trim().length()>0)) {
					this.sectionHeadlines.put(""+sectionCounter, val.trim());
				} else {
					checkForMoreSections = false;
				}
				
				// check internal position of items under section
				if (checkForMoreSections) {
					int posCounter = 0;
					boolean checkForMoreItems = true;
					while (checkForMoreItems) {
						posCounter++;
						String itemKey = props.getProperty(SECTION+sectionCounter+"_"+POS+posCounter);
						if ((itemKey!=null) && (itemKey.trim().length()>0)) {
							addMetadataItemToConfig(itemKey.trim(), createPositionKey(sectionCounter, posCounter));
						} else {
							checkForMoreItems = false;
						}						
					}
				}
				
			}
			
			// ########## check for sectionX			
			boolean gotSectionX = false;
			String val = props.getProperty(SECTIONX_HEADLINE_TEXT);
			if ((val!=null) && (val.trim().length()>0)) {
				this.sectionHeadlines.put(SECTIONX_KEY, val);
				gotSectionX = true;
			}		
			
			
			// ########## check if failed
			if ((sectionCounter==1) && (!gotSectionX)) {
				System.err.println("DetailDisplayConfigurator: Was not able to load 'DETAIL VIEW DATA CONFIG' from '"+Const.PROPERTIES_FILE+"'");							
				setDefaultConfig =true;
			}
			
			
		} catch (Exception e) {
			System.err.println("ResultDisplayConfigurator: Was not able to load properties from '"+Const.PROPERTIES_FILE+"'");			
			setDefaultConfig = true;
		}
		
		// ########## set default if needed
		if (setDefaultConfig) {
			System.err.println("ResultDisplayConfigurator: Problem with '"+Const.PROPERTIES_FILE+"' ... setting default vaules on JSP Detail view.");				
			titleElementItemKey = CCConstants.LOM_PROP_GENERAL_TITLE;
			this.sectionHeadlines.put(SECTIONX_KEY, "Metadata");
		}

	}
	
	/**
	 * @param key
	 * @param positionKey is a string of format sectionNumber+"_"+internalPositionOfItemNumber
	 */
	private void addMetadataItemToConfig(String key, String positionKey) {
		this.metadataConfig.put(key, positionKey);
		this.metadataConfigReversed.put(positionKey, key);
	}
	
	/**
	 * Add HTML redering of an item to fragment buffer.
	 * All fragments will be later put together in right order.
	 * @param key	of Metadata CCConstant
	 * @param html	the html rendering
	 * @return	false if item is not set to be in result config
	 */
	public boolean addHtmlViewOfItem(String key, String label, String value) {
		
		// check if title element
		if (key.equals(titleElementItemKey)) {
			this.htmlFragmentsBuffer.put(TITLE_KEY, value);	
			this.lastTitle = value;
			return true;
		} 
		
		// check if item is to be displayed
		if ((!this.metadataConfig.containsKey(key)) && (!this.sectionHeadlines.containsKey(SECTIONX_KEY))) return false;
		
		// connect html fragment
		String html = "<span class='pair_key'>"+label+":</span> <span class='pair_value'>"+value+"</span>";
		
		// get position key
		String positionKey  = this.metadataConfig.get(key);
		if (positionKey==null) {
			// create a new positionkey for section x
			itemCounterSectionX++;
			positionKey= SECTIONX_KEY+"_"+itemCounterSectionX;
		}
		
		this.htmlFragmentsBuffer.put(positionKey, html);
		return true;
	}
	
	/**
	 * Returns the contructed HTML of all item rendering is correct order
	 * @param resultNumberingOnHeadline numnbering String on result item headline (null=ignore)
	 * @return
	 */
	public String constructResultItemHTML() {
				
		/*
		 * NOTE ABOUT HTML THAT GETS PRODUCED:
		 * Its structural HTML ... to format layout use CSS styling.
		 * All metadata elements are in divs with specifig classes.
		 * This classes can be used for individual format styling.
		 */
		
		String resultHTML = "";
		
		// title
		if ((this.titleElementItemKey!=null) && (this.titleElementItemKey.length()>0)) {
			resultHTML += "\n<h2 class='detail_title'>"+this.htmlFragmentsBuffer.get(TITLE_KEY)+"</h2>";			
		}
		
		// sections
		int sectionCounter = 0;
		boolean checkForMoreSections = true;
		while (checkForMoreSections) {
			sectionCounter++;
			if (this.sectionHeadlines.containsKey(sectionCounter+"")) {
				
				// add section headline
				resultHTML += "\n<h3 class='detail_section_headline' id='detail_section"+sectionCounter+"'>"+this.sectionHeadlines.get(sectionCounter+"")+"</h3>";							
				
				// items of section
				int itemCounter = 0;
				boolean checkForMoreItems = true;
				while (checkForMoreItems) {
					itemCounter++;
					if (this.htmlFragmentsBuffer.containsKey(createPositionKey(sectionCounter, itemCounter))) {
						
						// add item under section
						resultHTML += "\n"+"<div class='detail_"+JspTools.justChars(this.getKeyByPosition(createPositionKey(sectionCounter, itemCounter)))+"'>"+this.htmlFragmentsBuffer.get(createPositionKey(sectionCounter, itemCounter))+"</div>";					
												
					} else {
						checkForMoreItems=false;
					}
				}
				
			} else {
				checkForMoreSections = false;
			}
		}
		
		// Section X
		if (this.sectionHeadlines.containsKey(SECTIONX_KEY)) {
			
			// add section headline
			resultHTML += "\n<h3 class='detail_section_headline' id='detail_sectionx'>"+this.sectionHeadlines.get(SECTIONX_KEY)+"</h3>";							
			
			// items of section
			int itemCounter = 0;
			boolean checkForMoreItems = true;
			while (checkForMoreItems) {
				itemCounter++;
				if (this.htmlFragmentsBuffer.containsKey(createPositionKey(0, itemCounter))) {
					
					// add item under section
					resultHTML += "\n"+"<div class='detail_"+JspTools.justChars(this.getKeyByPosition(createPositionKey(0, itemCounter)))+"'>"+this.htmlFragmentsBuffer.get(createPositionKey(0, itemCounter))+"</div>";					
											
				} else {
					checkForMoreItems=false;
				}
			}
		}
				
		this.htmlFragmentsBuffer.clear();
		return resultHTML;
	}
	
	/**
	 * Returns title data ... if available
	 * @return
	 */
	public String getTitleString() {
		return this.lastTitle;
	}
	
	private String createPositionKey(int sectionNumber, int internalPositionNumber) {
		return  sectionNumber+"_"+internalPositionNumber;
	}
	
	private String getKeyByPosition(String positionKey) {
		String result = this.metadataConfigReversed.get(positionKey);
		if (result==null) result = "(POSITION HAS NO KEY)";
		return result;
	}
	
}