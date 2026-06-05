package org.edu_sharing.repository.server.importer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.forms.VCardTool;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.VCardPIDMapper;
import org.edu_sharing.service.license.LicenseService;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class RecordHandlerDublinCoreDMG implements RecordHandlerInterface {

	Log logger = LogFactory.getLog(RecordHandlerDublinCoreDMG.class);
	XPathFactory pfactory = XPathFactory.newInstance();
	XPath xpath = pfactory.newXPath();
	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	
	String metadataSetId = null;
	Map<String, Object> toSafeMap = new HashMap<>();

    List<String> toRemoveList = new ArrayList<>();
	
	int counter = 0;

    LicenseService licenseService = new LicenseService();
	
	@Override
	public Map<String, Object> getProperties() {
		return toSafeMap;
	}

    @Override
    public List<String> getPropertiesToRemove() {
        return toRemoveList;
    }

    public RecordHandlerDublinCoreDMG(String metadataSetId) {
		logger.info("initializing...");
		
		if(metadataSetId == null || metadataSetId.trim().isEmpty()){
			metadataSetId = "default";
		}
		this.metadataSetId = metadataSetId;
	}
	
	@Override
	public void handleRecord(Node nodeRecord, String cursor, String set) throws Throwable {
		logger.debug("starting...");
		toSafeMap.clear();
		if(counter > 2000){
			logger.info("to much records for one resumption:"+counter);
			return;
		}
		
		String replicationId = (String) xpath.evaluate("header/identifier", nodeRecord, XPathConstants.STRING);
		
		String lomCatalogId = null;
		if(replicationId != null){
			if(replicationId.split(":").length == 3) {
				lomCatalogId = replicationId.substring(0, replicationId.lastIndexOf(":"));
			}
			if(replicationId.split(":").length == 2){
				lomCatalogId = replicationId.split(":")[0];
			}
		}
		
		Map<String, Object> generalIdentifierToSafeMap = new HashMap<>();
		generalIdentifierToSafeMap.put(CCConstants.LOM_PROP_IDENTIFIER_ENTRY, replicationId);
		generalIdentifierToSafeMap.put(CCConstants.LOM_PROP_IDENTIFIER_CATALOG, lomCatalogId);
		
		toSafeMap.put(CCConstants.CCM_PROP_IO_OBJECTTYPE, "0");
		toSafeMap.put("TYPE#" + CCConstants.LOM_TYPE_IDENTIFIER + "#" + CCConstants.LOM_ASSOC_IDENTIFIER, generalIdentifierToSafeMap);
		toSafeMap.put(CCConstants.CCM_PROP_IO_REPLICATIONSOURCEID, replicationId);
		toSafeMap.put(CCConstants.CCM_PROP_IO_REPLICATIONSOURCE, lomCatalogId);
		
		logger.info("lomCatalogId:" + lomCatalogId + " replicationId:" + replicationId);
		
		String title = (String) xpath.evaluate("metadata/dc/title", nodeRecord, XPathConstants.STRING);
		logger.info("title:"+title);
		if(title != null){
			String safeTitle = title.replaceAll( ApplicationInfoList.getHomeRepository().getValidatorRegexCMName(), "_");
			safeTitle = clearEnd(safeTitle);
			
			
			logger.info("safeTitle:"+safeTitle);
			toSafeMap.put(CCConstants.CM_NAME, safeTitle);
			toSafeMap.put(CCConstants.LOM_PROP_GENERAL_TITLE, title);
		}

        // subjects
        List<String> taxonIds = new ArrayList<>();
        List<String> keywords = new ArrayList<>();
        NodeList subjects = (NodeList) xpath.evaluate("metadata/dc/subject", nodeRecord, XPathConstants.NODESET);
        for(int i = 0; i < subjects.getLength(); i++){
            Node subjectNode = subjects.item(i);
            String subject = (String)xpath.evaluate(".", subjectNode, XPathConstants.STRING);
            if(subject != null && !subject.trim().startsWith("https://w3id.org/kim/hochschulfaechersystematik")){
                keywords.add(subject);
            }else{
                taxonIds.add(subject);
            }
        }

        if(keywords.isEmpty()){
            toRemoveList.add(CCConstants.LOM_PROP_GENERAL_KEYWORD);
        }else{
            toSafeMap.put(CCConstants.LOM_PROP_GENERAL_KEYWORD, keywords);
        }

        if(taxonIds.isEmpty()){
            toRemoveList.add(CCConstants.CCM_PROP_IO_REPL_TAXON_ID);
        }else {
            toSafeMap.put(CCConstants.CCM_PROP_IO_REPL_TAXON_ID, taxonIds);
        }



        // types
        List<String> lrts = new ArrayList<>();
        NodeList types = (NodeList) xpath.evaluate("metadata/dc/type", nodeRecord, XPathConstants.NODESET);
        for(int i = 0; i < types.getLength(); i++){
            Node subjectNode = types.item(i);
            String subject = (String)xpath.evaluate(".", subjectNode, XPathConstants.STRING);
            if(subject != null && !subject.trim().isEmpty()){
                lrts.add(subject);
            }
        }

        if(lrts.isEmpty()){
            toRemoveList.add(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_LEARNINGRESSOURCETYPE);
        }else {
            toSafeMap.put(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_LEARNINGRESSOURCETYPE, lrts);
        }

		
		NodeList descriptionList = (NodeList) xpath.evaluate("metadata/dc/description", nodeRecord, XPathConstants.NODESET);
		
		
		//description and thumbnail, description is not a multivalue property in edu-sharing so we concat the dc description multivalue
		String description = null;
		for(int i = 0; i < descriptionList.getLength(); i++){
			Node descNode = descriptionList.item(i);
			String descrString = ((String)xpath.evaluate(".", descNode, XPathConstants.STRING)).trim();
			if(descrString.contains("http://") && (descrString.endsWith(".jpg") || descrString.endsWith(".png") || descrString.endsWith(".gif") || descrString.endsWith(".jpeg"))){
				toSafeMap.put(CCConstants.CCM_PROP_IO_THUMBNAILURL, descrString);
			}else{
				description = (description == null) ? descrString : description + "; "+descrString;
			}
			
			logger.info("descrString:"+descrString);
		}
		if(description  != null){
			toSafeMap.put(CCConstants.LOM_PROP_GENERAL_DESCRIPTION, description);
		}

        List<String> creators = new ArrayList<>();
		NodeList metadataCreator =  (NodeList) xpath.evaluate("metadata/dc/creator", nodeRecord, XPathConstants.NODESET);
		for(int i = 0; i < metadataCreator.getLength(); i++){
			Node metadataCreatorNode = metadataCreator.item(i);
			String metadataCreatorString = ((String)xpath.evaluate(".", metadataCreatorNode, XPathConstants.STRING)).trim();
			if(!metadataCreatorString.isEmpty()){
				Map<String,String> vcardMap = VCardPIDMapper.build(metadataCreatorString);
				String vcardString = VCardTool.hashMap2VCard(vcardMap);
                creators.add(vcardString);
			}
		}
        if(creators.isEmpty()){
            toRemoveList.add(CCConstants.CCM_PROP_IO_REPL_METADATACONTRIBUTER_CREATOR);
        }else{
            toSafeMap.put(CCConstants.CCM_PROP_IO_REPL_METADATACONTRIBUTER_CREATOR, creators);
        }

        List<String> publisher = new ArrayList<>();
		NodeList contributerPublisher = (NodeList) xpath.evaluate("metadata/dc/publisher", nodeRecord, XPathConstants.NODESET);
		for(int i = 0; i < contributerPublisher.getLength(); i++){
			Node contributerPublisherNode= contributerPublisher.item(i);
			String contributerPublisherString = ((String)xpath.evaluate(".", contributerPublisherNode, XPathConstants.STRING)).trim();
			if(!contributerPublisherString.isEmpty()){
				Map<String,String> vcardMap =  VCardPIDMapper.build(contributerPublisherString);
				String vcardString = VCardTool.hashMap2VCard(vcardMap);
                publisher.add(vcardString);
			}
		}

        if(publisher.isEmpty()){
            toRemoveList.add(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_PUBLISHER);
        }else {
            toSafeMap.put(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_PUBLISHER, publisher);
        }
		
		NodeList identifierList = (NodeList) xpath.evaluate("metadata/dc/identifier", nodeRecord, XPathConstants.NODESET);
		String technicalLocation = null;
		String downloadLocation = null;
		for(int i = 0; i < identifierList.getLength(); i++){
			Node identifierNode = identifierList.item(i);
			String identifierElement = (String)xpath.evaluate(".", identifierNode, XPathConstants.STRING);
			//if it's a protocol like http://
			if(identifierElement.matches("[a-zA-Z]*://.*")){
				if(i == 0) {
					technicalLocation = identifierElement;
				}else {
					downloadLocation = identifierElement;
				}
			}
		}
		
		if(technicalLocation != null && !technicalLocation.trim().isEmpty()){
			toSafeMap.put(CCConstants.CCM_PROP_IO_WWWURL, technicalLocation);
		}

		if(downloadLocation != null && !downloadLocation.trim().isEmpty()){
			toSafeMap.put(CCConstants.LOM_PROP_TECHNICAL_LOCATION,downloadLocation);
		}
		
		String lomRights = (String) xpath.evaluate("metadata/dc/rights", nodeRecord, XPathConstants.STRING);
		toSafeMap.put(CCConstants.LOM_PROP_RIGHTS_RIGHTS_DESCRIPTION, lomRights);

        if(lomRights != null && !lomRights.trim().isEmpty()) {
            try {
                LicenseService.LicenseUrl licenseUrl = licenseService.parseLicenseUrl(lomRights.trim());
                toSafeMap.put(CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY, licenseUrl.licenseKey());
                toSafeMap.put(CCConstants.CCM_PROP_IO_COMMONLICENSE_CC_VERSION,licenseUrl.version());
                toSafeMap.put(CCConstants.CCM_PROP_IO_COMMONLICENSE_CC_LOCALE,licenseUrl.language());

            } catch (IllegalArgumentException e) {
                logger.info("can not parse url: " + lomRights + " - " + e.getMessage());
            }
        }
		
		counter++;
	}
	
	String clearEnd(String toClear){
		
		String safeTitle = toClear;
		
		//filename should not end with " "
		safeTitle = safeTitle.trim();
		
		//filename should not end with "."
		if(safeTitle.endsWith(".")){
			safeTitle = safeTitle.substring(0, safeTitle.length() - 1);
			clearEnd(safeTitle);
		}
		
		return safeTitle;
	}
	
}
