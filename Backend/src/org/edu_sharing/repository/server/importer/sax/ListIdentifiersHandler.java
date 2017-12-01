package org.edu_sharing.repository.server.importer.sax;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.importer.PersistentHandlerInterface;
import org.edu_sharing.repository.server.tools.HttpQueryTool;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

public  class ListIdentifiersHandler extends DefaultHandler {

	Logger logger = Logger.getLogger(ListIdentifiersHandler.class);

	List<String> openedElements = new ArrayList<String>();

	String currentValue = null;
	String currentResumptionToken = null;
	Attributes currentAtts = null;

	String currentIdentifier = null;
	String currentTimeStamp = null;

	boolean currentRecordIsDeleted = false;

	String completeListSize = null;
	String cursor = null;
	String set = null;
	String oaiBaseUrl = null;
	String metadataPrefix = null;

	String esMetadataSetId = null;

	HttpQueryTool qt = new HttpQueryTool();

	PersistentHandlerInterface persistentHandler = null;
	
	RecordHandlerInterface recordHandler = null;

	public ListIdentifiersHandler(RecordHandlerInterface recordHandler, PersistentHandlerInterface persistentHandler, String oaiBaseUrl, String set, String metadataPrefix,
			String esMetadataSetId, String resumptionToken) {
		this.oaiBaseUrl = oaiBaseUrl;
		this.metadataPrefix = metadataPrefix;
		this.esMetadataSetId = esMetadataSetId;
		this.persistentHandler = persistentHandler;
		this.set = set;
		this.recordHandler = recordHandler;

		String url = this.oaiBaseUrl + "?verb=ListIdentifiers&resumptionToken=" + resumptionToken;
		handleIdentifiersList(url);
	}

	public ListIdentifiersHandler(RecordHandlerInterface recordHandler, PersistentHandlerInterface persistentHandler, String oaiBaseUrl, String set, String metadataPrefix,
			String esMetadataSetId) {

		this.oaiBaseUrl = oaiBaseUrl;
		this.metadataPrefix = metadataPrefix;
		this.esMetadataSetId = esMetadataSetId;
		this.persistentHandler = persistentHandler;
		this.set = set;
		this.recordHandler = recordHandler;

		String url = this.oaiBaseUrl + "?verb=ListIdentifiers&metadataPrefix=" + metadataPrefix + "&set=" + set;
		handleIdentifiersList(url);

	}

	public void handleIdentifiersList(String url) {

		String result = qt.query(url);
		

		try {

			InputSource inputSource = new InputSource(new ByteArrayInputStream(result.getBytes(Charset.forName("utf-8"))));

			XMLReader xmlReader = XMLReaderFactory.createXMLReader();
			
			// Stream to xml file
			xmlReader.setContentHandler(this);

			// Start parsing
			xmlReader.parse(inputSource);
			
		} catch (SAXException e) {
			logger.error(e.getMessage(), e);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		currentValue += new String(ch, start, length);
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		openedElements.add(localName);
		currentValue = "";
		currentAtts = atts;

		String parentLocalName = (openedElements.size() > 1) ? openedElements.get(openedElements.size() - 2).toLowerCase() : "";
		String lowerLocalName = localName.toLowerCase();
		if (parentLocalName.equals("listidentifiers") && lowerLocalName.equals("header")) {

			String status = currentAtts.getValue("status");
			if (status != null && status.trim().equals("deleted")) {
				currentRecordIsDeleted = true;
			}
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		currentValue = currentValue.trim();

		String parentLocalName = (openedElements.size() > 1) ? openedElements.get(openedElements.size() - 2).toLowerCase() : "";

		String grandParentLocalName = (openedElements.size() > 2) ? openedElements.get(openedElements.size() - 3).toLowerCase() : "";

		String grandGrandParentLocalName = (openedElements.size() > 3) ? openedElements.get(openedElements.size() - 4).toLowerCase() : "";

		String lowerLocalName = localName.toLowerCase();

		if (parentLocalName.equals("oai-pmh") && lowerLocalName.equals("error")) {
			logger.error("oai service error:" + currentValue + " code:" + currentAtts.getValue("code"));
		}

		// reset deleted status
		if (parentLocalName.equals("listidentifiers") && lowerLocalName.equals("header")) {

			currentRecordIsDeleted = false;
		}

		if (grandParentLocalName.equals("listidentifiers") && parentLocalName.equals("header") && lowerLocalName.equals("identifier")) {
			currentIdentifier = currentValue;
			if (currentTimeStamp != null) {
				handleRecord(currentIdentifier, currentTimeStamp);
			}
		}

		if (grandParentLocalName.equals("listidentifiers") && parentLocalName.equals("header") && lowerLocalName.equals("datestamp")) {
			currentTimeStamp = currentValue;
			if (currentIdentifier != null) {
				handleRecord(currentIdentifier, currentTimeStamp);
			}
		}

		if (parentLocalName.equals("listidentifiers") && lowerLocalName.equals("header")) {
			// reset the data pair
			currentIdentifier = null;
			currentTimeStamp = null;
		}

		if (parentLocalName.equals("listidentifiers") && lowerLocalName.equals("resumptiontoken")) {
			currentResumptionToken = currentValue;
			completeListSize = currentAtts.getValue("completeListSize");
			cursor = currentAtts.getValue("cursor");
		}

		// cleanup stack
		if (openedElements.get(openedElements.size() - 1).equals(localName)) {
			logger.debug("will remove " + localName + " from stack");
			openedElements.remove(openedElements.size() - 1);

		} else {
			String message = "something went wrong closed element is not the last on stack.";
			logger.error(message);
			throw new SAXException(message);
		}
	}

	/**
	 * must be called inside a /record/header processing so that currentRecordIsDeleted has the right context
	 * @param replId
	 * @param timeStamp
	 */
	private void handleRecord(String replId, String timeStamp) {
	
		if (currentRecordIsDeleted) {
			logger.info("record " + currentIdentifier + " is deleted");
			return;
		}
		
		boolean mustBePersisted = persistentHandler.mustBePersisted(replId, timeStamp);

		if (mustBePersisted) {
			
			String url = this.oaiBaseUrl + "?verb=GetRecord&identifier=" + replId + "&metadataPrefix=" + this.metadataPrefix;
			logger.info("url:" + url);
			String result = qt.query(url);

			try {
				RecordHandlerInterface rh = recordHandler;
				
				/**
				 * prevent a Invalid byte 2 of 3-byte UTF-8 sequence. org.xml.sax.SAXParseException: Invalid byte 2 of 3-byte UTF-8 sequence.
				 * with explicit utf-8 encoding
				 */
				rh.handleRecord(new ByteArrayInputStream(result.getBytes(Charset.forName("utf-8"))));
				if (rh.getProperties().size() > 0) {
					persistentHandler.safe(rh.getProperties(), cursor, set);
				} else {
					logger.warn("no properties found for object:" + replId);
				}
				
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			} catch (SAXParseException e) {
				logger.error("oai document with id:" + currentValue + " is not well formed");
				logger.error(e.getMessage(), e);
			} catch (SAXException e) {
				logger.error(e.getMessage(), e);
			} catch (Throwable e) {
				logger.error(e.getMessage(), e);
			}
		}
	}

	@Override
	public void startDocument() throws SAXException {
		currentResumptionToken = null;
		currentValue = null;
	}

	@Override
	public void endDocument() throws SAXException {

		logger.info("end with cursor:" + cursor);

		if (currentResumptionToken != null && !currentResumptionToken.trim().equals("")) {
			if (currentResumptionToken != null && currentResumptionToken.trim().length() > 0 && new Integer(completeListSize) > new Integer(cursor)) {
				String url = this.oaiBaseUrl + "?verb=ListIdentifiers&resumptionToken=" + currentResumptionToken;
				this.handleIdentifiersList(url);
			}
		}

	}
}
