package org.edu_sharing.repository.server.tools.metadataset;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetValueKatalog;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public class SAXValueSpaceHandler implements ContentHandler {

	// concrete
	private String currentProperty;

	// concrete key props
	private String currentCaption;
	private String currentStatement;
	private String currentParent;

	// abstract
	private String currentValue;

	List<MetadataSetValueKatalog> result = new ArrayList<MetadataSetValueKatalog>();

	// constructor initialized
	String valuespace_i18n_prefix;
	String valuespaceI18nBundle;
	String valuespace_key;

	MetadataReader mdReader = new MetadataReader();

	HashMap<MetadataSetValueKatalog, String> hasParentMap = new HashMap<MetadataSetValueKatalog, String>();

	/**
	 * @param valueSpaceFile
	 * @param valuespaceI18nBundle can be null
	 * @param valuespace_i18n_prefix can be null
	 * @param valuespace_key
	 * @throws SAXException
	 * @throws IOException
	 */
	public SAXValueSpaceHandler(InputStream isValueSpaceFile, String valuespaceI18nBundle, String valuespace_i18n_prefix, String valuespace_key) throws SAXException, IOException {
		this.valuespace_i18n_prefix = valuespace_i18n_prefix;
		this.valuespaceI18nBundle = valuespaceI18nBundle;
		this.valuespace_key = valuespace_key;
		
		XMLReader xmlReader = XMLReaderFactory.createXMLReader();
		InputSource inputSource = new InputSource(isValueSpaceFile);
		xmlReader.setContentHandler(this);
		
	    // start parser
	    xmlReader.parse(inputSource);
	    
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		
		if (localName.equals("valuespace")) {
			currentProperty = atts.getValue("property");
		}

		if (localName.equals("key")) {
			currentCaption = atts.getValue("cap");
			currentStatement = atts.getValue("statement");
			currentParent = atts.getValue("parent");
		}

	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {

		if (localName.equals("key") && currentProperty.equals(valuespace_key)) {

			String key = currentValue;
			String caption = currentCaption;
			String statement = currentStatement;

			// so that also empty values work
			if (key == null) key = "";

			MetadataSetValueKatalog metadataSetValue = new MetadataSetValueKatalog();
			metadataSetValue.setKey(key);
			if (caption != null)
				metadataSetValue.setCaption(caption);

			// search statement
			if (statement != null) metadataSetValue.setStatement(statement);

			String i18nKey = (valuespace_i18n_prefix != null) ? i18nKey = valuespace_i18n_prefix + key : key;
			if(valuespaceI18nBundle != null){
				HashMap<String, String> i18n = mdReader.getI18nMap(valuespaceI18nBundle, i18nKey);
				if (i18n.size() > 0) {
					metadataSetValue.setI18n(i18n);
				}
			}

			// parent
			if (currentParent != null && !currentParent.trim().equals("")) {
				hasParentMap.put(metadataSetValue, currentParent);
			}

			result.add(metadataSetValue);
		}

	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		currentValue = new String(ch, start, length);
	}

	@Override
	public void endDocument() throws SAXException {
		// set parent keys
		// ATTENTION there can arise a loop
		if (hasParentMap.size() > 0) {
			for (MetadataSetValueKatalog mdsv : result) {
				if (hasParentMap.keySet().contains(mdsv)) {
					String parent = hasParentMap.get(mdsv);
					for (MetadataSetValueKatalog mdsv2 : result) {
						if (mdsv2.getKey().equals(parent)) {
							mdsv.setParentValue(mdsv2);
						}
					}
				}
			}
		}

	}

	@Override
	public void endPrefixMapping(String prefix) throws SAXException {
	}

	@Override
	public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
	}

	@Override
	public void processingInstruction(String target, String data) throws SAXException {
	}

	@Override
	public void setDocumentLocator(Locator locator) {
	}

	@Override
	public void startDocument() throws SAXException {
	}

	@Override
	public void skippedEntity(String name) throws SAXException {
	}

	@Override
	public void startPrefixMapping(String prefix, String uri) throws SAXException {
	}

	public SAXValueSpaceHandler() {
	}
	
	public List<MetadataSetValueKatalog> getResult() {
		return result;
	}
}
