package org.edu_sharing.repository.client.tools.forms;


public class TaxonTool {

	
	public final static String TAXONPATH_XML_VALUE = "<taxonpath><source>${source}</source><id>${id}</id><entry>${entry}</entry></taxonpath>";
	
	public String getTaxonXML(String source, String id, String entry){
		
		source = (source == null) ? "" : source;
		id = (id == null) ? "" : id;
		entry = (entry == null) ? "" : entry;
		
		String result = new String(TAXONPATH_XML_VALUE);
		
		result = result.replace("${source}", source);
		result = result.replace("${id}", id);
		result = result.replace("${entry}", entry);
		return result;
	}
	
}
