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

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.sf.vcard4j.java.AddressBook;
import net.sf.vcard4j.java.Type;
import net.sf.vcard4j.java.VCard;
import net.sf.vcard4j.java.type.ADR;
import net.sf.vcard4j.java.type.EMAIL;
import net.sf.vcard4j.java.type.FN;
import net.sf.vcard4j.java.type.N;
import net.sf.vcard4j.java.type.ORG;
import net.sf.vcard4j.java.type.TEL;
import net.sf.vcard4j.java.type.TITLE;
import net.sf.vcard4j.java.type.URL;
import net.sf.vcard4j.java.type.X;
import net.sf.vcard4j.parser.DomParser;
import org.apache.log4j.Logger;
import org.apache.xerces.dom.DocumentImpl;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.StringTool;
import org.w3c.dom.Document;

public class VCardConverter {
	
	static Logger logger = Logger.getLogger(VCardConverter.class);
	public static ArrayList<HashMap<String,Object>> vcardToHashMap(String propPrefix, String vcardString){
		
		ArrayList<HashMap<String,Object>> result = null;
		DomParser parser = new DomParser();
		
		Document document = new DocumentImpl();
		
		if(vcardString == null || vcardString.trim().equals("")) return null;
	
		StringReader stringReader = new StringReader(vcardString);
		
		try{
			
			parser.parse(stringReader, document);
		    result = new ArrayList<HashMap<String,Object>>();
		    
			AddressBook addressBook = new AddressBook(document);
			for (Iterator vcards = addressBook.getVCards(); vcards.hasNext(); ) {
				
				HashMap<String,Object> vcardMap = new HashMap<String,Object>();
				VCard vcard = (VCard) vcards.next();
				N n = (N)getType(CCConstants.VCARD_T_N, vcard);
				ADR adr = (ADR)getType(CCConstants.VCARD_T_ADR, vcard);
				EMAIL mail = (EMAIL)getType(CCConstants.VCARD_T_EMAIL, vcard);
				ORG org = (ORG)getType(CCConstants.VCARD_T_ORG, vcard);
				TEL tel = (TEL)getType(CCConstants.VCARD_T_TEL, vcard);
				TITLE title = (TITLE)getType(CCConstants.VCARD_T_TITLE, vcard);
				URL url = (URL)getType(CCConstants.VCARD_T_URL, vcard);
				FN fn = (FN)getType(CCConstants.VCARD_T_FN, vcard);
				X xLomComtrDate = (X)getType(CCConstants.VCARD_T_X_ES_LOM_CONTRIBUTE_DATE,vcard);
				
				vcardMap.put(propPrefix+CCConstants.VCARD_T_FN,(fn != null)? fn.get(): "");
				vcardMap.put(propPrefix+CCConstants.VCARD_SURNAME,(n != null)? n.getFamily(): "");
				vcardMap.put(propPrefix+CCConstants.VCARD_GIVENNAME,(n != null)?  n.getGiven() : "");
				vcardMap.put(propPrefix+CCConstants.VCARD_STREET,(adr != null)? adr.getStreet() : "");
				vcardMap.put(propPrefix+CCConstants.VCARD_CITY,(adr != null)?  adr.getLocality(): "");
				vcardMap.put(propPrefix+CCConstants.VCARD_PLZ,(adr != null)?  adr.getPcode(): "");
				vcardMap.put(propPrefix+CCConstants.VCARD_REGION,(adr != null)?  adr.getRegion(): "");
				vcardMap.put(propPrefix+CCConstants.VCARD_COUNTRY,(adr != null)?  adr.getCountry(): "");
				vcardMap.put(propPrefix+CCConstants.VCARD_EMAIL, (mail!= null)? mail.get(): "");
				vcardMap.put(propPrefix+CCConstants.VCARD_ORG,(org != null)? org.getOrgname(): "");
				vcardMap.put(propPrefix+CCConstants.VCARD_TITLE,(title != null)? title.get(): "");
				vcardMap.put(propPrefix+CCConstants.VCARD_TEL,(tel != null)? tel.get(): "");
				vcardMap.put(propPrefix+CCConstants.VCARD_URL,(url != null)? url.get(): "");
				
				if(xLomComtrDate != null && xLomComtrDate.get() != null){
					vcardMap.put(propPrefix+CCConstants.VCARD_EXT_LOM_CONTRIBUTE_DATE,(xLomComtrDate != null)? xLomComtrDate.get(): "");
				}
				
				result.add(vcardMap);
			}
			
		}catch(Exception e){
			logger.debug(e.getMessage(),e);
			return new ArrayList<HashMap<String,Object>>();
		}
		
		return result;
	}

	public static String getNameForVCard(String prefix,HashMap<String,Object> data){
		if(isPersonVCard(prefix,data)){
			String name="";
			if(data.containsKey(prefix+CCConstants.VCARD_GIVENNAME))
				name+=data.get(prefix+CCConstants.VCARD_GIVENNAME)+" ";
			if(data.containsKey(prefix+CCConstants.VCARD_SURNAME))
				name+=data.get(prefix+CCConstants.VCARD_SURNAME)+" ";
			if(!name.trim().isEmpty())
				return name.trim();
		}
		if(data.containsKey(prefix+CCConstants.VCARD_ORG))
			return (String) data.get(prefix+CCConstants.VCARD_ORG);
		return null;		
	}
	public static ArrayList<HashMap<String,Object>> vcardToHashMap(String vcardString){
		return vcardToHashMap("", vcardString);
	}
	
	private static Type getType(String typeName, VCard vcard){
		Iterator iter = vcard.getTypes(typeName);
		if(iter != null && iter.hasNext()){
			return (Type)iter.next();
		}else{
			return null;
		}
	}
	
	static ArrayList<String> vcardProps = null;

	static public boolean isVCardProp(String property) {
		if(vcardProps == null){
			vcardProps = new ArrayList<String>();
			vcardProps.add(CCConstants.LOM_PROP_CONTRIBUTE_ENTITY);
			vcardProps.add(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_AUTHOR);
			vcardProps.add(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_PUBLISHER);
			vcardProps.add(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_CONTENT_PROVIDER);
			vcardProps.add(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_EDITOR);
			vcardProps.add(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_EDUCATIONAL_VALIDATOR);
			vcardProps.add(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_GRAPHICAL_DESIGNER);
			vcardProps.add(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_INITIATOR);
			vcardProps.add(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_INSTRUCTIONAL_DESIGNER);
			vcardProps.add(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_SCRIPT_WRITER);
			vcardProps.add(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_SUBJECT_MATTER_EXPERT);
			vcardProps.add(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_TECHNICAL_IMPLEMENTER);
			vcardProps.add(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_TECHNICAL_VALIDATOR);
			vcardProps.add(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_TERMINATOR);
			vcardProps.add(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_UNKNOWN);
			vcardProps.add(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_VALIDATOR);
			
			vcardProps.add(CCConstants.CCM_PROP_IO_REPL_METADATACONTRIBUTER_CREATOR);
			vcardProps.add(CCConstants.CCM_PROP_IO_REPL_METADATACONTRIBUTER_PROVIDER);
			vcardProps.add(CCConstants.CCM_PROP_IO_REPL_METADATACONTRIBUTER_VALIDATOR);
		}
		if(vcardProps.contains(property)){
			return true;
		}else{
			return false;
		}
	}

	/**
	 * tests if Property is VCard Prop and converts the String to HashMap used
	 * for contributers
	 * 
	 * @param type
	 * @param property
	 * @param value
	 * @return
	 */
	public static HashMap<String, Object> getVCardHashMap(String type, String property, String value) {

		HashMap<String, Object> result = null;
		
		// VCard
		if (isVCardProp(property)) {
				
				//split for multivalue vcards:
				String[] splitted = value.split(StringTool.escape(CCConstants.MULTIVALUE_SEPARATOR));
				for(String split:splitted){
					ArrayList<HashMap<String, Object>> vcards = vcardToHashMap(property, split);
					if (vcards != null) {
						if(vcards.size()==0)
							return new HashMap<String,Object>();
						if(result == null){
							result = vcards.get(0);
						}else{
							HashMap<String, Object> tmpVCardMap = vcards.get(0);
							for(Map.Entry<String,Object> vcardEntry : tmpVCardMap.entrySet()){
								//check if vcardprop already has a value in result
								String vcardValInResult = (String)result.get(vcardEntry.getKey());
								if(vcardValInResult != null && !vcardValInResult.trim().equals("")){
									result.put(vcardEntry.getKey(), vcardValInResult + CCConstants.MULTIVALUE_SEPARATOR + (String)vcardEntry.getValue());
								}else{
									result.put(vcardEntry.getKey(), vcardEntry.getValue());
								}
							}
						}
						
					}
				}
		}
		
		return result;
	}
	
	/**
	 * Search for all vcard properties and inflate them, returning a new property map with all inflated vcard props
	 * @param nodeType
	 * @param props
	 * @return
	 */
	public static Map<String, Object> addVCardProperties(String nodeType, Map<String, Object> props) {
		HashMap<String, Object> propsNew = new HashMap<String, Object>();
		propsNew.putAll(props);
		for(Map.Entry<String, Object> entry : props.entrySet()){
			if(entry == null || entry.getKey() == null || entry.getValue() == null) continue;
			HashMap<String, Object> vcard = getVCardHashMap(nodeType, entry.getKey(), entry.getValue().toString());
			if(vcard!=null)
				propsNew.putAll(vcard);
		}
		return propsNew;
	}

    public static boolean isPersonVCard(String prefix, HashMap<String, Object> data) {
        return data.get(prefix+CCConstants.VCARD_GIVENNAME)!=null && !data.get(prefix+CCConstants.VCARD_GIVENNAME).toString().isEmpty() ||
                data.containsKey(prefix+CCConstants.VCARD_SURNAME) && !data.get(prefix+CCConstants.VCARD_SURNAME).toString().isEmpty();
    }
}