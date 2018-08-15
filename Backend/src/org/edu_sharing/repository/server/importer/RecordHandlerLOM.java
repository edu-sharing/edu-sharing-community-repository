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
package org.edu_sharing.repository.server.importer;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.forms.TaxonTool;
import org.edu_sharing.repository.server.RepoFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class RecordHandlerLOM implements RecordHandlerInterface {
	Log logger = LogFactory.getLog(RecordHandlerLOM.class);
	XPathFactory pfactory = XPathFactory.newInstance();
	XPath xpath = pfactory.newXPath();
	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	
	
	
	String metadataSetId = null;
	
	HashMap toSafeMap = new HashMap();
	
	public RecordHandlerLOM( String metadataSetId) {
		logger.info("initializing...");
		
		this.metadataSetId = metadataSetId;
		
		if(metadataSetId == null || metadataSetId.trim().equals("")){
			metadataSetId = "default";
		}
		this.metadataSetId = metadataSetId;
	}
	public void handleRecord(Node nodeRecord, String cursor, String set) throws Throwable {
		logger.debug("starting handleRecord()");

		toSafeMap.clear();

		// @test
		//XPath xpath = pfactory.newXPath();

		String replicationTimeStamp = (String) xpath.evaluate("header/datestamp", nodeRecord, XPathConstants.STRING);
		/**
		 * general
		 */
		// general identifier
		String replicationId = (String) xpath.evaluate("metadata/lom/general/identifier/entry", nodeRecord, XPathConstants.STRING);
		String lomCatalogId = (String) xpath.evaluate("metadata/lom/general/identifier/catalog", nodeRecord, XPathConstants.STRING);

		
		logger.info("lomCatalogId:" + lomCatalogId + " replicationId:" + replicationId);

		HashMap generalTitleI18n = getMultiLangValue((Node) xpath.evaluate("metadata/lom/general/title", nodeRecord, XPathConstants.NODE));

		String generallanguage = (String) xpath.evaluate("metadata/lom/general/language", nodeRecord, XPathConstants.STRING);
		HashMap generalDescriptionI18n = getMultiLangValue((Node) xpath.evaluate("metadata/lom/general/description", nodeRecord, XPathConstants.NODE));

		Node generalNode = (Node) xpath.evaluate("metadata/lom/general", nodeRecord, XPathConstants.NODE);

		List generalKeywords = getMultivalue(generalNode, "keyword");
		
		
		String structureSrc = (String) xpath.evaluate("metadata/lom/general/structure/source", nodeRecord, XPathConstants.STRING);
		String structureVal = (String) xpath.evaluate("metadata/lom/general/structure/value", nodeRecord, XPathConstants.STRING);

		String aggregationLevelSrc = (String) xpath.evaluate("metadata/lom/general/aggregationLevel/source", nodeRecord, XPathConstants.STRING);
		String aggregationLevelVal = (String) xpath.evaluate("metadata/lom/general/aggregationLevel/value", nodeRecord, XPathConstants.STRING);

		// SAFE PART
		// so that it can be found in search
		toSafeMap.put(CCConstants.CCM_PROP_IO_OBJECTTYPE, "0");
		toSafeMap.put(CCConstants.CCM_PROP_IO_REPLICATIONSOURCE, lomCatalogId);
		toSafeMap.put(CCConstants.CCM_PROP_IO_REPLICATIONSOURCEID, replicationId);
		toSafeMap.put(CCConstants.CCM_PROP_IO_REPLICATIONSOURCETIMESTAMP, replicationTimeStamp);
		
		String titleEntry = (String)generalTitleI18n.values().iterator().next();
		toSafeMap.put(CCConstants.LOM_PROP_GENERAL_TITLE, titleEntry);
		String name = new String(titleEntry);
		name = name.replaceAll(
				RepoFactory.getEdusharingProperty(CCConstants.EDU_SHARING_PROPERTIES_PROPERTY_VALIDATOR_REGEX_CM_NAME), "_");

		//replace ending dot with nothing
		//cmNameReadableName = cmNameReadableName.replaceAll("\\.$", "");
		name = name.replaceAll("[\\.]*$", "");
		
		toSafeMap.put(CCConstants.CM_NAME, name);
		toSafeMap.put(CCConstants.CM_PROP_C_TITLE, titleEntry);
		
		toSafeMap.put(CCConstants.LOM_PROP_GENERAL_LANGUAGE, generallanguage);
		
		
		//@TODO what to do with metadataset
		
		toSafeMap.put(CCConstants.CM_PROP_METADATASET_EDU_METADATASET, metadataSetId);
		
		
		if(generalDescriptionI18n != null && generalDescriptionI18n.size() > 0){
			toSafeMap.put(CCConstants.LOM_PROP_GENERAL_DESCRIPTION, generalDescriptionI18n);
		}

		if (generalKeywords != null && generalKeywords.size() > 0) {
			toSafeMap.put(CCConstants.LOM_PROP_GENERAL_KEYWORD, generalKeywords);
		}

		// @TODO structureSrc
		// toSafeMap.put(CCConstants.LOM_PROP_GENERAL_STRUCTURE_SRC,
		// structureSrc);
		if(structureVal != null && !structureVal.equals("")){
			toSafeMap.put(CCConstants.LOM_PROP_GENERAL_STRUCTURE, structureVal);
		}
		// @TODO aggregationLevelVal
		// toSafeMap.put(CCConstants.LOM_PROP_GENERAL_AGGREGATIONLEVEL_SRC,
		// aggregationLevelSrc);
		if(aggregationLevelVal != null && !aggregationLevelVal.trim().equals("")){
			toSafeMap.put(CCConstants.LOM_PROP_GENERAL_AGGREGATIONLEVEL, aggregationLevelVal);
		}
		
		
		/**
		 * lifecycle
		 */
		Node lifecycleNode = (Node) xpath.evaluate("metadata/lom/lifeCycle", nodeRecord, XPathConstants.NODE);
		if (lifecycleNode != null) {
			Node versionNode = (Node) xpath.evaluate("version", lifecycleNode, XPathConstants.NODE);
			HashMap lifecycleVersion = getMultiLangValue(versionNode);

			String lifecycleStatusSource = (String) xpath.evaluate("status/source", lifecycleNode, XPathConstants.STRING);
			String lifecycleStatusValue = (String) xpath.evaluate("status/value", lifecycleNode, XPathConstants.STRING);

			// lifecycleContribute
			ArrayList<HashMap<String, Object>> lifecycleContributes = getContributes(lifecycleNode);

			HashMap<String,ArrayList<String>> replLifecycleContributer = new HashMap<String,ArrayList<String>>();
			if(lifecycleContributes != null){
				for (HashMap contr : lifecycleContributes) {
					String role = (String) contr.get(CCConstants.LOM_PROP_CONTRIBUTE_ROLE);
					String entity = (String) contr.get(CCConstants.LOM_PROP_CONTRIBUTE_ENTITY);
					
					if(role != null){
						String lc_property = CCConstants.getLifecycleContributerProp(role.trim());
						if(lc_property != null && !lc_property.trim().equals("")){
							ArrayList<String> tmpLCList = replLifecycleContributer.get(lc_property);
							if(tmpLCList == null) tmpLCList = new ArrayList<String>();
							tmpLCList.add(entity);
							replLifecycleContributer.put(lc_property, tmpLCList);
						}else{
							logger.warn("can not map lifecycle contributer role "+role+" to edu-sharing property");
						}
					}
				}
			}
			
			for(Map.Entry<String,String> entry : CCConstants.getLifecycleContributerPropsMap().entrySet()){
				ArrayList<String> entityListForLifecycleContrProp = replLifecycleContributer.get(entry.getValue());
				
				//alfresco dont likes to safe empty lists
				if(entityListForLifecycleContrProp == null || entityListForLifecycleContrProp.size() == 0){
					toSafeMap.put(entry.getValue(),null);
				}else{
					toSafeMap.put(entry.getValue(), entityListForLifecycleContrProp);
				}
			}
			

			// SAFE PART
			if(lifecycleVersion != null) toSafeMap.put(CCConstants.LOM_PROP_LIFECYCLE_VERSION, lifecycleVersion);
			// @TODO toSafeMap.put(CCConstants.LOM_PROP_LIFECYCLE_STATUS_SOURCE,
			// lifecycleStatusSource);
			if(lifecycleStatusValue != null) toSafeMap.put(CCConstants.LOM_PROP_LIFECYCLE_STATUS, lifecycleStatusValue);
		}
		
		
		/**
		 * metaMetadata
		 */
		Node nodeMetaMetadata = (Node) xpath.evaluate("metadata/lom/metaMetadata", nodeRecord, XPathConstants.NODE);
		if (nodeMetaMetadata != null) {
			String identifierCatalog = (String) xpath.evaluate("identifier/catalog", nodeMetaMetadata, XPathConstants.STRING);
			String identifierEntry = (String) xpath.evaluate("identifier/entry", nodeMetaMetadata, XPathConstants.STRING);
			String language = (String) xpath.evaluate("language", nodeMetaMetadata, XPathConstants.STRING);

			ArrayList<HashMap<String, Object>> metaMetadataContributes = getContributes(nodeMetaMetadata);

			
			HashMap<String,ArrayList<String>> replMetadataContributer = new HashMap<String,ArrayList<String>>();
			
			if (metaMetadataContributes != null) {
				for (HashMap<String, Object> map : metaMetadataContributes) {
					String role = (String) map.get(CCConstants.LOM_PROP_CONTRIBUTE_ROLE);
					String entity = (String) map.get(CCConstants.LOM_PROP_CONTRIBUTE_ENTITY);
					
					
					if (role != null){
						String mdContributerProp = CCConstants.getMetadataContributerProp(role.trim());
						if(mdContributerProp != null && !mdContributerProp.trim().equals("")){
							ArrayList<String> tmpMDCList = replMetadataContributer.get(mdContributerProp);
							if(tmpMDCList == null) tmpMDCList = new ArrayList<String>();
							tmpMDCList.add(entity);
							replMetadataContributer.put(mdContributerProp, tmpMDCList);
						}else{
							logger.error("can not map metadata contributer role "+role+" to edu-sharing property");
						}
					}
				}
			}
			
			for(Map.Entry<String,String> entry : CCConstants.getMetadataContributerPropsMap().entrySet()){
				ArrayList<String> entityListForMetadataContrProp = replMetadataContributer.get(entry.getValue());
				
				//alfresco dont likes to safe empty lists
				if(entityListForMetadataContrProp == null || entityListForMetadataContrProp.size() == 0){
					toSafeMap.put(entry.getValue(),null);
				}else{
					toSafeMap.put(entry.getValue(), entityListForMetadataContrProp);
				}
			}
				
		}
		
		/**
		 * technical
		 */
		Node nodeTechnical = (Node) xpath.evaluate("metadata/lom/technical", nodeRecord, XPathConstants.NODE);
		if(nodeTechnical != null){
			String format = (String) xpath.evaluate("format", nodeTechnical, XPathConstants.STRING);
			String size = (String) xpath.evaluate("size", nodeTechnical, XPathConstants.STRING);
			String location = (String) xpath.evaluate("location", nodeTechnical, XPathConstants.STRING);
			// @TODO duration (noch nicht vorhanden)
	
			// SAFE PART
			if(format != null && !format.trim().equals(""))	toSafeMap.put(CCConstants.LOM_PROP_TECHNICAL_FORMAT, format);
			if(size != null && !size.equals("")) toSafeMap.put(CCConstants.LOM_PROP_TECHNICAL_SIZE, size);
			if(location != null && !location.trim().equals("")){
				toSafeMap.put(CCConstants.LOM_PROP_TECHNICAL_LOCATION, location);
				toSafeMap.put(CCConstants.CCM_PROP_IO_WWWURL, location);
			}
		
		}

		/**
		 * rights
		 */
		Node lomNode = (Node) xpath.evaluate("metadata/lom", nodeRecord, XPathConstants.NODE);
		if(lomNode != null){
			String rightsCostSrc = (String) xpath.evaluate("rights/cost/source", lomNode, XPathConstants.STRING);
			String rightsCostValue = (String) xpath.evaluate("rights/cost/value", lomNode, XPathConstants.STRING);
			String copyRightSrc = (String) xpath.evaluate("rights/copyrightAndOtherRestrictions/source", lomNode, XPathConstants.STRING);
			String copyRightVal = (String) xpath.evaluate("rights/copyrightAndOtherRestrictions/value", lomNode, XPathConstants.STRING);
	
			HashMap rightsDescription = getMultiLangValue((Node) xpath.evaluate("rights/description", lomNode, XPathConstants.NODE));
			// @TODO was passiert mit description lang=x-t-cc-url wert z.B. =
			// http://creativecommons.org/licenses/by-nc/2.0/de/
	
			// SAFE PART
			if (copyRightVal != null && !copyRightVal.trim().equals("")) {
	
				if (copyRightVal.equals("yes") || copyRightVal.equals("no")) {
					Boolean copyRightValBool = (copyRightVal.equals("yes")) ? true : false;
					toSafeMap.put(CCConstants.LOM_PROP_RIGHTS_COPY_RIGHT, copyRightValBool);
				} else {
					logger.info("unknown value for copyRightVal:" + copyRightVal);
				}
			}
			if (rightsCostValue != null && !rightsCostValue.trim().equals("")) {
				if (rightsCostValue.equals("yes") || rightsCostValue.equals("no")) {
					Boolean rightsCostValueBool = (rightsCostValue.equals("yes")) ? true : false;
					toSafeMap.put(CCConstants.LOM_PROP_RIGHTS_COST, rightsCostValueBool);
				}
			}
			if(rightsDescription != null && rightsDescription.size() > 0){
				toSafeMap.put(CCConstants.LOM_PROP_RIGHTS_RIGHTS_DESCRIPTION, rightsDescription);
			}
		}
		
		/**
		 * educational
		 * 
		 * @todo typicallearningtime
		 */
		NodeList nodeEducationalList = (NodeList) xpath.evaluate("metadata/lom/educational", nodeRecord, XPathConstants.NODESET);
		
		// LOM Replication lists
		List<String> lomReplicationLearningresourceTypeList = new ArrayList<String>();
		List<String> lomReplicationEducationalContextList = new ArrayList<String>();
		List<String> lomReplicationEducationalTypicalAgeRange = new ArrayList<String>();
		List<String> lomReplicationEducationalIntendedEndUserRole = new ArrayList<String>();
		List<String> lomReplicationEducationalLanguage = new ArrayList<String>();
		for (int eduIdx = 0; eduIdx < nodeEducationalList.getLength(); eduIdx++) {
			Node nodeEducational = nodeEducationalList.item(eduIdx);
			NodeList learningresourceTypeList = (NodeList) xpath.evaluate("learningResourceType", nodeEducational, XPathConstants.NODESET);
			
			for (int i = 0; i < learningresourceTypeList.getLength(); i++) {
				Node learningresourceTypeNode = learningresourceTypeList.item(i);
				String learningResourceTypeSource = (String) xpath.evaluate("source", learningresourceTypeNode, XPathConstants.STRING);
				String learningResourceTypeValue = (String) xpath.evaluate("value", learningresourceTypeNode, XPathConstants.STRING);
				lomReplicationLearningresourceTypeList.add(learningResourceTypeValue);
				// @TODO learningResourceTypeSource
			}

			NodeList intendedEndUserRoleList = (NodeList) xpath.evaluate("intendedEndUserRole", nodeEducational, XPathConstants.NODESET);
			for (int i = 0; i < intendedEndUserRoleList.getLength(); i++) {
				Node intendedEndUserNode = intendedEndUserRoleList.item(i);
				String intendedEndUserRoleSource = (String) xpath.evaluate("source", intendedEndUserNode, XPathConstants.STRING);
				String intendedEndUserRoleValue = (String) xpath.evaluate("value", intendedEndUserNode, XPathConstants.STRING);
				// @TODO intendedEndUserRoleSource
				lomReplicationEducationalIntendedEndUserRole.add(intendedEndUserRoleValue);
			}

			NodeList contextList = (NodeList) xpath.evaluate("context", nodeEducational, XPathConstants.NODESET);
			for (int i = 0; i < contextList.getLength(); i++) {
				Node contextNode = contextList.item(i);
				String contexteSource = (String) xpath.evaluate("source", contextNode, XPathConstants.STRING);
				String contextValue = (String) xpath.evaluate("value", contextNode, XPathConstants.STRING);
				lomReplicationEducationalContextList.add(contextValue);
				// @TODO contexteSource
			}

			List typicalAgeRangeList = getMultivalue(nodeEducational, "typicalAgeRange");
			
			
			if(typicalAgeRangeList != null){
				for(Object tmpTypicalAgerange: typicalAgeRangeList){
					if(tmpTypicalAgerange instanceof Map){
						Map maptypicalAgeRange = (Map)tmpTypicalAgerange;
						for(Object key : maptypicalAgeRange.keySet()){
							String tarAsString = (String)maptypicalAgeRange.get(key);
							if(!lomReplicationEducationalTypicalAgeRange.contains(tarAsString)){
								lomReplicationEducationalTypicalAgeRange.add(tarAsString);
							}
						}
					}else{
						String tarAsString = tmpTypicalAgerange.toString();
						if(!lomReplicationEducationalTypicalAgeRange.contains(tarAsString)){
							lomReplicationEducationalTypicalAgeRange.add(tarAsString);
						}
					}
				}
			}
			
			String educationalLanguage = (String) xpath.evaluate("language", nodeEducational, XPathConstants.STRING);
			if(educationalLanguage != null && !educationalLanguage.trim().equals("")) lomReplicationEducationalLanguage.add(educationalLanguage);
		}
		
		
		// alfreco don't likes to safe empty lists when the prop is multilang
		// and multivalue
		if (lomReplicationLearningresourceTypeList.size() > 0){
			toSafeMap.put(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_LEARNINGRESSOURCETYPE, lomReplicationLearningresourceTypeList);
		}else{
			toSafeMap.put(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_LEARNINGRESSOURCETYPE, null);
		}
		if (lomReplicationEducationalContextList.size() > 0){
			toSafeMap.put(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_CONTEXT, lomReplicationEducationalContextList);
		}else{
			toSafeMap.put(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_CONTEXT, null);
		}
		
		if(lomReplicationEducationalTypicalAgeRange.size() > 0){
			toSafeMap.put(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_TYPICALAGERANGE, lomReplicationEducationalTypicalAgeRange);
		}else{
			toSafeMap.put(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_TYPICALAGERANGE, null);
		}
		
		if(lomReplicationEducationalIntendedEndUserRole.size() > 0){
			toSafeMap.put(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_INTENDEDENDUSERROLE, lomReplicationEducationalIntendedEndUserRole);
		}else{
			toSafeMap.put(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_INTENDEDENDUSERROLE, null);
		}
		
		if(lomReplicationEducationalLanguage.size() > 0){
			toSafeMap.put(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_LANGUAGE, lomReplicationEducationalLanguage);
		}else{
			toSafeMap.put(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_LANGUAGE, null);
		}
		
		
		
		/**
		 * classification
		 */
		//for replication
		//ArrayList<String>
		NodeList nodeClassificationList = (NodeList) xpath.evaluate("metadata/lom/classification", nodeRecord, XPathConstants.NODESET);
		
		// LOM Replication
		List lomReplicationTaxonEntry = new ArrayList();
		List lomReplicationTaxonId = new ArrayList();
		List<String> lomReplicationClassificationPurpose = new ArrayList<String>();
		List<String> lomReplicationTaxonPathXML = new ArrayList<String>();
		for (int i = 0; i < nodeClassificationList.getLength(); i++) {
			Node classificationNode = nodeClassificationList.item(i);
			String purposeSource = (String) xpath.evaluate("purpose/source", classificationNode, XPathConstants.STRING);
			String purposeValue = (String) xpath.evaluate("purpose/value", classificationNode, XPathConstants.STRING);
			
			if(!lomReplicationClassificationPurpose.contains(purposeValue)) lomReplicationClassificationPurpose.add(purposeValue);

			NodeList taxonPathList = (NodeList) xpath.evaluate("taxonPath", classificationNode, XPathConstants.NODESET);
			for (int taxonPathIdx = 0; taxonPathIdx < taxonPathList.getLength(); taxonPathIdx++) {
				Node taxonPathNode = taxonPathList.item(taxonPathIdx);
				HashMap taxonPathSource = getMultiLangValue((Node) xpath.evaluate("source", taxonPathNode, XPathConstants.NODE));
				
				String replTaxonPathSource = (String)xpath.evaluate("source", taxonPathNode, XPathConstants.STRING);
				
				NodeList taxonList = (NodeList) xpath.evaluate("taxon", taxonPathNode, XPathConstants.NODESET);
				for (int taxonIdx = 0; taxonIdx < taxonList.getLength(); taxonIdx++) {
					Node taxonNode = taxonList.item(taxonIdx);
					String taxonId = (String) xpath.evaluate("id", taxonNode, XPathConstants.STRING);
					if(taxonId != null) taxonId = taxonId.trim();
					HashMap taxonEntry = getMultiLangValue((Node) xpath.evaluate("entry", taxonNode, XPathConstants.NODE));

					String replTaxonEntry = (String)xpath.evaluate("entry", taxonNode, XPathConstants.STRING);
					
					if(!lomReplicationTaxonEntry.contains(taxonEntry)) lomReplicationTaxonEntry.add(taxonEntry);
					if(!lomReplicationTaxonId.contains(taxonId))lomReplicationTaxonId.add(taxonId);
					
					
					if(taxonId != null && !taxonId.trim().equals("")){
						lomReplicationTaxonPathXML.add(new TaxonTool().getTaxonXML(replTaxonPathSource, taxonId, replTaxonEntry));
					}
				}

			}
			
		}

		
		if(lomReplicationTaxonEntry.size() > 0){
			toSafeMap.put(CCConstants.CCM_PROP_IO_REPL_TAXON_ENTRY, lomReplicationTaxonEntry);
		}else{
			toSafeMap.put(CCConstants.CCM_PROP_IO_REPL_TAXON_ENTRY, null);
		}
		if(lomReplicationTaxonId != null && lomReplicationTaxonId.size() > 0){
			toSafeMap.put(CCConstants.CCM_PROP_IO_REPL_TAXON_ID, lomReplicationTaxonId);
		}else{
			toSafeMap.put(CCConstants.CCM_PROP_IO_REPL_TAXON_ID, null);
		}
		
		if(lomReplicationClassificationPurpose.size() > 0){
			toSafeMap.put(CCConstants.CCM_PROP_IO_REPL_CLASSIFICATION_PURPOSE,lomReplicationClassificationPurpose);
		}else{
			toSafeMap.put(CCConstants.CCM_PROP_IO_REPL_CLASSIFICATION_PURPOSE,null);
		}
		
		if(lomReplicationTaxonPathXML.size() > 0){
			toSafeMap.put(CCConstants.CCM_PROP_IO_REPL_TAXONPATH_XML,lomReplicationTaxonPathXML);
		}else{
			toSafeMap.put(CCConstants.CCM_PROP_IO_REPL_TAXONPATH_XML,null);
		}
		
		
		/**
		 * relation
		 * 
		 * @TODO
		 */

		/**
		 * Annotation
		 * 
		 * @TODO
		 */

		/**
		 * Requirement
		 * 
		 * @TODO
		 */

		/**
		 * @TODO Für die Suche: alle LOM Daten die in verschachtelten Objekten
		 *       sitzen replizeiren und direkt an dem IO speichern
		 */

		/**
		 * additional data from elixier
		 * 
		 */
		


	}
	
	public HashMap getMultiLangValue(Node node) throws XPathExpressionException {
		HashMap result = new HashMap();
		if (node != null) {
			NodeList langList = (NodeList) xpath.evaluate("string", node, XPathConstants.NODESET);
			for (int i = 0; i < langList.getLength(); i++) {
				Node langValNode = langList.item(i);
				String language = (String) xpath.evaluate("@language", langValNode, XPathConstants.STRING);
				String value = (String) xpath.evaluate(".", langValNode, XPathConstants.STRING);
				if (language != null && !language.trim().equals("") && value != null && !value.trim().equals("")) {
					// wir brauchen das LAND damit i18n richtig funktioniert
					if (language.length() == 2) {
						language = language + "_" + language.toUpperCase();
					}

					value = adaptValue(value);
					result.put(language, value);
				}
			}
		}
		return result;
	}

	public List getMultivalue(Node parent, String nodeName) throws XPathExpressionException {
		if (parent == null){
			logger.debug("returning null cause parent == null");
			return null;
		}

		List result = new ArrayList();

		NodeList nodelist = (NodeList) xpath.evaluate(nodeName, parent, XPathConstants.NODESET);
		for (int i = 0; i < nodelist.getLength(); i++) {
			Node node = nodelist.item(i);
			// test if multilang
			Double countStringTag = (Double) xpath.evaluate("count(string)", node, XPathConstants.NUMBER);
			if (countStringTag != null && countStringTag > 0) {
				HashMap multiLangVal = getMultiLangValue(node);
				if (multiLangVal != null && multiLangVal.size() > 0)
					result.add(multiLangVal);
			} else {
				String value = (String) xpath.evaluate(".", node, XPathConstants.STRING);
				if (value != null && !value.trim().equals("")) {
					value = adaptValue(value);
					result.add(value);
				}
			}
		}
		if (result.size() > 0)
			return result;
		else
			return null;
	}

	public ArrayList<HashMap<String, Object>> getContributes(Node parent) throws XPathExpressionException {
		ArrayList<HashMap<String, Object>> result = new ArrayList<HashMap<String, Object>>();
		NodeList contributes = (NodeList) xpath.evaluate("contribute", parent, XPathConstants.NODESET);
		for (int i = 0; i < contributes.getLength(); i++) {
			HashMap<String, Object> map = new HashMap<String, Object>();
			Node nodeContribute = contributes.item(i);
			String contributeRoleSource = (String) xpath.evaluate("role/source", nodeContribute, XPathConstants.STRING);
			String contributeRoleValue = (String) xpath.evaluate("role/value", nodeContribute, XPathConstants.STRING);
			String contributeEntity = (String) xpath.evaluate("entity", nodeContribute, XPathConstants.STRING);
			String contributeDate = (String) xpath.evaluate("date", nodeContribute, XPathConstants.STRING);

			
			
			
			Date date = null;
			try {
				SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SS");
				date = df.parse(contributeDate);
			} catch (Exception e) {
				logger.debug("error wrong contribute date:"+contributeDate);
			}

			if (date != null){
				map.put(CCConstants.LOM_PROP_CONTRIBUTE_DATE, date);
			}
			if (contributeEntity != null){
				//replace chars from vcard:
				contributeEntity = contributeEntity.replace("&quot;", "");
				map.put(CCConstants.LOM_PROP_CONTRIBUTE_ENTITY, contributeEntity);
			}
			if (contributeRoleValue != null){
				map.put(CCConstants.LOM_PROP_CONTRIBUTE_ROLE, contributeRoleValue);
			}
			// @TODO contributeRoleSource
			result.add(map);
		}
		if (result.size() > 0)
			return result;
		else
			return null;
	}
	
	private String adaptValue(String value) {
		String result = org.htmlparser.util.Translate.decode(value);
		return result;
	}
	
	@Override
	public HashMap<String, Object> getProperties() {
		// TODO Auto-generated method stub
		return toSafeMap;
	}
}
