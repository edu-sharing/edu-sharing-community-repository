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

import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetFormsProperty;

public class Const {

	public static final String	METADATASET_SCREENREADER 		= "screenreader";
	public static final String	METADATASET_SCREENREADER_PATH   = "/org/edu_sharing/metadataset/metadataset_screenreader.xml";	
	
	public static final String[] SUPPORTED_WIDGET_TYPES = {
		MetadataSetFormsProperty.WIDGET_DATEPICKER,
		MetadataSetFormsProperty.WIDGET_CHECKBOX,
		MetadataSetFormsProperty.WIDGET_LISTBOX,	
		MetadataSetFormsProperty.WIDGET_SEARCHUSER,	
		MetadataSetFormsProperty.WIDGET_TEXTFIELD,
		MetadataSetFormsProperty.WIDGET_TAXON_SIMPLE
	};
	
	public static boolean isSupportedMetadataField(final String constFromMetadataSetFormsProperty) {
		for (String type : SUPPORTED_WIDGET_TYPES) {
			if (type.equals(constFromMetadataSetFormsProperty)) return true;
		}
		return false;
	}	
	
	public static final String PARA_SUCHANFRAGE = "suchanfrage";
	public static final String PARA_STARTITEM 	= "startitem";	

	public static final String PARA_REPO_LIST = "repList";		
	public static final String PARA_REPO_PREFIX = "repID_";	
	
	public static final String PARA_REPO = "repoID";
	public static final String PARA_META = "metaID";	
	
	public static final String VAL_TRUE 	= "TRUE";	
	public static final String VAL_FALSE 	= "FALSE";		

	public static final String PARA_TYP1 	= "typ1";
	public static final String PARA_TYP2 	= "typ2";
	
	public static final String FLAG_EXISTS =  "exists";	

	public static final String SESSION_SEARCH_RESULT_BUFFER = "sessionSRB";
		
	/*
	 * SCREENREADER.PROPERTIES FILE
	 */
	
	public static final String PROPERTIES_FILE = "screenreader.properties";
	
	public static final String CUSTOM_CSS_PATH = "CUSTOMCSSPATH";
}
