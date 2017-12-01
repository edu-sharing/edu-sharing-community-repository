package org.edu_sharing.repository.server.importer;

import java.io.StringReader;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.xpath.XPathConstants;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public class OAIPMHLOMValidator extends OAIPMHLOMImporter{
	
	Logger logger = Logger.getLogger(OAIPMHLOMValidator.class);
	
	String metadataPrefix = null;
	
	public OAIPMHLOMValidator(String oai_base_url, int nrOfResumptions, int nrOfRecords, String metadataPrefix, String[] sets) throws Exception{
		
		super();
		this.setBaseUrl(oai_base_url);
		this.setNrOfRecords(nrOfRecords);
		this.setNrOfResumptions(nrOfResumptions);
		this.setMetadataPrefix(metadataPrefix);
		this.setSet(sets[0]);
		this.setPersistentHandler(new  PersistentHandlerInterface( ){
			
			@Override
			public boolean mustBePersisted(String replId, String timeStamp) {
				return true;
			}
			
			@Override
			public String safe(Map props, String cursor, String set) throws Throwable {
				return null;
			}
		});
	}

	@Override
	protected void handleGetRecordStuff(String result, String cursor, String set, String identifier) {
		try{
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(new InputSource(new StringReader(result)));
			String errorcode = (String)xpath.evaluate("/OAI-PMH/error", doc, XPathConstants.STRING);
			if(errorcode == null || errorcode.trim().equals("")){
				logger.debug("everything is ok with identifier:"+identifier);
			}else{
				logger.error(errorcode);
			}
		}catch(org.xml.sax.SAXParseException e){
			logger.error("SAXParseException occured: cursor:"+cursor+ " set:"+set +" metadataPrefix:"+this.metadataPrefix + " identifier:"+ identifier );
		}catch(Throwable e){
			logger.error(e.getMessage(),e);
		}
	}
	
	public static void main(String[] args){
		try {
			OAIPMHLOMImporter importer = new OAIPMHLOMValidator("http://daunddort/cp/oai_pmh/oai.php", -1, -1, "oai_lom-de", new String[]{"mbnrw"});
			importer.startImport();
		} catch(Throwable e) {
			e.printStackTrace();
		}
	}
	
}
