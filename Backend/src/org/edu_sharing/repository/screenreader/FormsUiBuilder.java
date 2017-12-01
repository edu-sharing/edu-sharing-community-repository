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
import java.util.Iterator;
import java.util.List;

import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetFormsProperty;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetQueryProperty;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetValueKatalog;


/**
 * Builds HTML forms code based on meta data objects
 * @author Christian
 *
 */
public class FormsUiBuilder {

	public static String getFormInputHTML(MetadataSetQueryProperty prop, SearchMetadataHandler metadataHandler, String nameKey) throws MetadataMailformedException {
		
		// get type
		String widgetType = prop.getWidget();
		
		if (widgetType==null) throw new MetadataMailformedException("Widget type is NULL on metadata");
		if (!Const.isSupportedMetadataField(widgetType)) throw new MetadataMailformedException("Widget type '"+widgetType+"' on metadata not supported (yet)");
		
		// get info wich type of widget should be created
		String label = null;
		if (prop.getLabel() != null) {
			label = prop.getLabel().getValue(metadataHandler.getLanguage());
		}		
		
		HashMap<String, Object> props = null;
		
		String formElementValue = null; 
		Boolean multiple = prop.getMultiple();
		String copyfrom = prop.getCopyfrom();
		
		String title = null;
		if(prop.getWidgetTitle() != null){
			title = prop.getWidgetTitle().getValue(metadataHandler.getLanguage());
		}	
		
		// name
		String insertName = nameKey+"_"+getNameOrGenerate(prop);
		
		/**
		 * TEXTFIELD
		 */
		if(widgetType.equals(MetadataSetFormsProperty.WIDGET_TEXTFIELD)){
					
			// value
			String insertValue = "";
			if (formElementValue != null) {
				insertValue = formElementValue;
			} else if (copyfrom != null && (formElementValue == null || formElementValue.trim().equals("")) && props != null) {
				String copyFromValue = (String) props.get(copyfrom);
				if (copyFromValue != null) {
					insertValue = copyFromValue;
				}
			}
			
			// get history value if set
			insertValue = metadataHandler.getParameterFromRequest(insertName, insertValue);
			
			String insertLabel = "";
			String id = metadataHandler.getNextUniqueElementId();
			if (label==null) label = "[MISSING '"+prop.getName()+"']";
			if (label!=null) insertLabel = " <label for='"+id+"'>"+label+" </label>";			
		
			return "<span id='"+MetadataSetFormsProperty.WIDGET_TEXTFIELD+"_"+insertName+"'>"+insertLabel+"<input id='"+id+"' tabindex='"+metadataHandler.getNextTabIndex()+"' type='text' title='' name='"+insertName+"' value='"+insertValue+"' /></span>";
		}		
		
		/**
		 * CHECKBOX
		 */		
		if(widgetType.equals(MetadataSetFormsProperty.WIDGET_CHECKBOX)){	
			
			// checked - default vaule
			String insertChecked = "";			
			if (props != null) {
				if (formElementValue != null && formElementValue.equals("true")) {
					insertChecked = "checked";
				}
			}
			
			// hidden data field & history value
			String hidden = "<input type='hidden' name='"+insertName+"_"+Const.FLAG_EXISTS+""+"' value='"+Const.FLAG_EXISTS+"' />";
			if (metadataHandler.getParameterFromRequest(insertName+"_"+Const.FLAG_EXISTS, "").equals(Const.FLAG_EXISTS)) {
				// set history value
				if (metadataHandler.getParameterFromRequest(insertName, Const.VAL_FALSE).equals(Const.VAL_TRUE)) {
					insertChecked = "checked";
				} else {
					insertChecked = "";					
				}
			}
			
			// label
			String insertLabel = "";
			String id = metadataHandler.getNextUniqueElementId();
			if (label!=null) insertLabel = " <label for='"+id+"'>"+label+"</label>";
						
			return "<input type='checkbox' id='"+id+"' tabindex='"+metadataHandler.getNextTabIndex()+"' name='"+insertName+"' value='"+Const.VAL_TRUE+"' "+insertChecked+"/>"+insertLabel+hidden+"<br/>\n";
		}	
		
		/**
		 * DATE
		 */		
		if(widgetType.equals(MetadataSetFormsProperty.WIDGET_DATEPICKER)){
			
			// get history value if set
			String insertValue = "TT.MM.JJJJ";
			insertValue = metadataHandler.getParameterFromRequest(insertName, insertValue);
			
			// label
			String insertLabel = "";
			String id = metadataHandler.getNextUniqueElementId();
			if (label!=null) insertLabel = " <label for='"+id+"'>"+label+" </label>";
			return insertLabel+"<input id='"+id+"' tabindex='"+metadataHandler.getNextTabIndex()+"' type='text' size='10' title='' name='"+insertName+"' value='"+insertValue+"' /> ";
		}	
		
		/**
		 * USER
		 */		
		if(widgetType.equals(MetadataSetFormsProperty.WIDGET_SEARCHUSER)){
			
			// get history value if set
			String insertValue = "";
			insertValue = metadataHandler.getParameterFromRequest(insertName, insertValue);
			
			// label
			String insertLabel = "";
			String id = metadataHandler.getNextUniqueElementId();
			if (label==null) label = "[MISSING '"+prop.getName()+"']";
			if (label!=null) insertLabel = " <label for='"+id+"'>"+label+" </label>";
			return insertLabel+"<input id='"+id+"' tabindex='"+metadataHandler.getNextTabIndex()+"' type='text' title='' name='"+insertName+"' value='"+insertValue+"' /> ";
		}	
		
		/**
		 * TAXON_SIMPLE
		 */		
		if(widgetType.equals(MetadataSetFormsProperty.WIDGET_TAXON_SIMPLE)){
			
			// get history value if set
			String insertValue = "";
			insertValue = metadataHandler.getParameterFromRequest(insertName, insertValue);
			
			// label
			String insertLabel = "";
			String id = metadataHandler.getNextUniqueElementId();
			if (label==null) label = "[MISSING '"+prop.getName()+"']";
			if (label!=null) insertLabel = " <label for='"+id+"'>"+label+" </label>";
			return "<span id='"+MetadataSetFormsProperty.WIDGET_TAXON_SIMPLE+"_"+insertName+"'>"+insertLabel+"<input id='"+id+"' tabindex='"+metadataHandler.getNextTabIndex()+"' type='text' title='' name='"+insertName+"' value='"+insertValue+"' /></span>";
		}			
		
		/**
		 * LIST SELECT BOX
		 */		
		if(widgetType.equals(MetadataSetFormsProperty.WIDGET_LISTBOX)){
						
			// multiple
			String insertMultiple = "";
			if (multiple) insertMultiple = "multiple";
			
			// String hidden data
			String hidden = "";
			
			// options
			String optionStr = "";
			List<MetadataSetValueKatalog> values = prop.getValuespace();
			if (values!=null) {
				Iterator<MetadataSetValueKatalog> i = values.iterator();
				while (i.hasNext()) {
					MetadataSetValueKatalog value = i.next();
					String key = value.getKey();
					String val = value.getValue(metadataHandler.getLanguage());
					if ((key!=null) && (val!=null)) {
						
						// history
						String selected = "";
						hidden += "<input type='hidden' name='"+insertName+"_"+JspTools.justChars(key)+"_"+Const.FLAG_EXISTS+"' value='"+Const.FLAG_EXISTS+"'>\n";
						if (metadataHandler.getParameterFromRequest(insertName+"_"+JspTools.justChars(key)+"_"+Const.FLAG_EXISTS, "").equals(Const.FLAG_EXISTS)) {
							if (metadataHandler.isValueInParameters(insertName, key)) {
								selected = "selected='selected' ";
							}
						}
						
						optionStr += "<option value='"+key+"' "+selected+">"+val+"</option>\n";	
					} else {
						if (key!=null) {
							optionStr += "<!-- ERROR:  No value str for key '"+key+"'-->\n";
						} else {
							optionStr += "<!-- ERROR:  Not valid entry in meta data  -->\n";							
						}
					}
				}
			}
			
			// label
			String id = metadataHandler.getNextUniqueElementId();
			String insertLabel = "";
			if (label==null) label = "[MISSING '"+prop.getName()+"']";
			if (label!=null) insertLabel = " <label for='"+id+"'>"+label+" </label>";
			hidden += "<input type='hidden' name='"+insertName+"_"+Const.FLAG_EXISTS+"' value='"+Const.FLAG_EXISTS+"'>\n";
			return "<select tabindex='"+metadataHandler.getNextTabIndex()+"' id='"+id+"' name='"+insertName+"' "+insertMultiple+">\n"+optionStr+"</select>\n"+insertLabel+hidden;
		}		
		
		
		if (title == null) title = "";
		return "\n<!-- org.edu_sharing.repository.screenreader.FormsUiBuilder.getFormInputHTML() : NO IMPLEMENTATION FOR METADATA PROP '"+widgetType+"' YET (Title '"+title+"') -->";
	}
	
	public static String wrappIntoFieldset(String label, String stylename, String queryHTML, SearchMetadataHandler metadataHandler) {
		String insertStyle = "";
		String insertLegend = "";
		if (stylename.length()>0) insertStyle = "class='"+stylename+"'";
		if (label.length()>0) insertLegend = "<legend>"+JspTools.toHTML(metadataHandler.getNextFormFieldIndex()+". "+label)+"</legend>";
		return "\n<fieldset "+insertStyle+">\n"+insertLegend+queryHTML+"\n</fieldset><br />\n";		
	}
	
	/**
	 * Generates a name from property
	 * @param prop
	 * @return
	 */
	public static String getNameOrGenerate(MetadataSetQueryProperty prop) {
		String result = "";
		String property = prop.getName();
		if (property!=null) {
			result = JspTools.justChars(property);			
		}
		if (result.length()<=1) {
			result = JspTools.justChars(prop.getType()) + "_" + result;
			if ((prop.getLabel()!=null) && (prop.getLabel().getKey()!=null)) {
				result += "_"+prop.getLabel().getKey();
			}
		}
		return result;
	}
	
}
