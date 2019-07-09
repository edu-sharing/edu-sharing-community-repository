package org.edu_sharing.repository.server.importer;

import java.io.StringReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.jobs.quartz.RemoveDeletedImportsFromSetJob;
import org.edu_sharing.repository.server.tools.HttpQueryTool;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * 
 * @author mv
 *
 */
public class ImportCleanerIdentifiersList {
	
	Logger logger = Logger.getLogger(ImportCleanerIdentifiersList.class);
	
	String oaiBaseUrl = null;
	
	XPathFactory pfactory = XPathFactory.newInstance();
	XPath xpath = pfactory.newXPath();
	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	
	List<String> nodeAtOaiService = new ArrayList<String>();
	
	/**
	 * <nodeuuid,replicationsourceid>
	 */
	Map<String,String> allNodesInSet = new HashMap<String,String>();
	
	MCAlfrescoAPIClient mcAlfrescoBaseClient = new MCAlfrescoAPIClient();
	
	List<String> toDeleteList = new ArrayList<String>();
	
	public ImportCleanerIdentifiersList(String baseUrl, String set, String metadataPrefix, boolean testMode) {
		try {
			
			oaiBaseUrl = baseUrl;
			readAllNodesInRepository(set);
			
			
			String url = oaiBaseUrl + "?verb=listIdentifiers&set=" + set + "&metadataPrefix=" + metadataPrefix;
			readAllNodesAtOaiService(url,set,metadataPrefix);
			
			logger.info("found:" + allNodesInSet.size() +" in repository for set " + set);
			logger.info("found:" + nodeAtOaiService.size() +" at oai service " + oaiBaseUrl + " set:" + set);
			
			
			for(Map.Entry<String, String> entry : allNodesInSet.entrySet()) {
				if(!nodeAtOaiService.contains(entry.getValue())){
					toDeleteList.add(entry.getKey());
				}
			}
			
			int deletedCounter = 0;
			for(String toDelete : toDeleteList) {
				logger.debug("will delete replicationsourceid:" + allNodesInSet.get(toDelete) +" alfresco id:" + toDelete +" cause it does not longer exist in set");
				
				if(!testMode) {
					mcAlfrescoBaseClient.removeNode(toDelete, null, false);
				}
				deletedCounter++;
			}
			
			if(testMode) {
				logger.info("runned in TESTMODE no nodes where deleted. set param " + RemoveDeletedImportsFromSetJob.PARAM_TESTMODE + " to true. " + deletedCounter + " would be deleted");
			}else {
				logger.info("finished! deleted " + deletedCounter + " nodes");
			}
			
		} catch (Throwable e) {
			logger.error(e.getMessage(), e);
		}
	}
	
	private void readAllNodesInRepository(String set) throws Throwable {
		
		String companyHomeId = mcAlfrescoBaseClient.getCompanyHomeNodeId();
		HashMap<String, Object> importFolderProps = mcAlfrescoBaseClient.getChild(companyHomeId, CCConstants.CCM_TYPE_MAP, CCConstants.CM_NAME,
				OAIPMHLOMImporter.FOLDER_NAME_IMPORTED_OBJECTS);
		
		if(importFolderProps == null) {
			logger.error("no import folder found");
			return;
		}
		
		String impFolderId = (String)importFolderProps.get(CCConstants.SYS_PROP_NODE_UID);
		HashMap<String, Object> setFolderProps = mcAlfrescoBaseClient.getChild(impFolderId, CCConstants.CCM_TYPE_MAP, CCConstants.CM_NAME, set);
		
		if(setFolderProps == null) {
			logger.error("no set folder found");
			return;
		}
		
		String setFolderId = (String)setFolderProps.get(CCConstants.SYS_PROP_NODE_UID);
		HashMap<String, HashMap<String, Object>> nodesInSet = mcAlfrescoBaseClient.getChildrenRecursive(setFolderId, CCConstants.CCM_TYPE_IO);
	
		for(Map.Entry<String, HashMap<String,Object>> entry : nodesInSet.entrySet()) {
			allNodesInSet.put((String)entry.getValue().get(CCConstants.SYS_PROP_NODE_UID), (String)entry.getValue().get(CCConstants.CCM_PROP_IO_REPLICATIONSOURCEID));
		}
	}
	

	
	private void readAllNodesAtOaiService(String url, String set, String metadataPrefix) throws Throwable{
		
		logger.info("url:"+url);
		
		String queryResult = new HttpQueryTool().query(url);
		if(queryResult != null){
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(new InputSource(new StringReader(queryResult)));
			
			String cursor = (String)xpath.evaluate("/OAI-PMH/ListIdentifiers/resumptionToken/@cursor", doc, XPathConstants.STRING);
			String completeListSize = (String)xpath.evaluate("/OAI-PMH/ListRecords/resumptionToken/@completeListSize", doc, XPathConstants.STRING);
			String token = (String)xpath.evaluate("/OAI-PMH/ListIdentifiers/resumptionToken", doc, XPathConstants.STRING);
			
			token = URLEncoder.encode(token);
			
			handleIdentifierList(doc,cursor,set);
			//&& completeListSize != null && cursor != null &&  (new Integer(completeListSize) > new Integer(cursor))
		
			if(token != null && token.trim().length() > 0 ){
				try{
					String urlNext = this.oaiBaseUrl+"?verb=ListIdentifiers&resumptionToken="+token;
					readAllNodesAtOaiService(urlNext,set,metadataPrefix);
					
				}catch(NumberFormatException e){
					logger.error(e.getMessage(),e);
				}
			}else{
				logger.info("no more resumption. import finished!");
			}
		}
	}
	
	private void handleIdentifierList(Document docIdentifiers, String cursor, String set) throws Throwable{
		NodeList nodeList = (NodeList)xpath.evaluate("/OAI-PMH/ListIdentifiers/header", docIdentifiers, XPathConstants.NODESET);
		
		int nrOfRs = nodeList.getLength();
		for(int i = 0; i < nrOfRs;i++){
			Node headerNode = nodeList.item(i);
			String identifier = (String)xpath.evaluate("identifier", headerNode, XPathConstants.STRING);
			String timeStamp = (String)xpath.evaluate("datestamp", headerNode, XPathConstants.STRING);
			
			String status = (String)xpath.evaluate("@status", headerNode, XPathConstants.STRING);
			if(status != null && status.trim().equals("deleted")){
				
				logger.info("Object with Identifier:"+identifier+" is deleted. Will continue with the next one");
				continue;
			}
			
			nodeAtOaiService.add(identifier);
		}
	}
}
