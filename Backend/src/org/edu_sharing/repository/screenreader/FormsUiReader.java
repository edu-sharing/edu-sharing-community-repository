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
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetQueryProperty;

/**
 * Reads metadata from HTTP request
 * @author Christian
 */
public class FormsUiReader {

	/*
	 * deliver data as getValues() from org.edu_sharing.repository.client.tools.metadata.ValueTool
	 */
	public static String[] getFormValueFromParameter( MetadataSetQueryProperty prop, SearchMetadataHandler searchMetadataHandler, String key) throws MetadataMailformedException, MetadataQueryNotSupportedException {
		
		String insertName 	= key+"_"+FormsUiBuilder.getNameOrGenerate(prop);
		String values[] 	= searchMetadataHandler.getParametersFromRequest(insertName);
		String widgetType 	= prop.getWidget();
	
		// check if data is missing
		if (values==null) {
			
			// maybe ignore - because not supported
			if ((widgetType!=null) && (!Const.isSupportedMetadataField(widgetType))) {			
				throw new MetadataQueryNotSupportedException();
			}
			
		}
	
		// check if data is in correct format based on type		
		if ((values!=null) && (values.length>0)) {
			
			if (widgetType.equals(MetadataSetFormsProperty.WIDGET_DATEPICKER)) {
				
				// remove formatting helping data
				if (values[0].equals("TT.MM.JJJJ")) { 
					values=null;
				} else {
					
					String[] parts = values[0].split(".");
					
					// check if format is using the right syntax 
					if (parts.length!=3) {
						throw new MetadataMailformedException("Bitte das Datum nach dem Format Muster TT.MM.JJJ eingeben.");
					}
					
					// check if numbers where used
					for (String part : parts) {
						Integer test = null;
						try { test = new Integer(part); } catch (Exception e) {}
						if (test==null) {
							throw new MetadataMailformedException("Bitte das Datum nach folgendem Beispiel eingeben: 01.03.2009");							
						}
					}
					
				}
				
			}			
		}
	
		System.out.print("- ("+insertName+") "+widgetType+" : ");
		if (values!=null) {
			for (String value: values) {
				System.out.println(value);
			}
		} else {
			System.out.println("[NULL]");			
		}
		return values;
	
	}

	
	
}
