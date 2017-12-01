package org.edu_sharing.repository.server.exporter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.MLText;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetValueKatalog;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.metadataset.MetadataReader;
import org.springframework.context.ApplicationContext;
import org.springframework.extensions.surf.util.ISO8601DateFormat;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class OAILOMExporter {

	Logger logger = Logger.getLogger(OAILOMExporter.class);

	ServiceRegistry serviceRegistry = null;

	NodeService nodeService = null;

	NodeRef nodeRef = null;

	Document doc;

	public OAILOMExporter(String ioId) throws ParserConfigurationException {
		ApplicationContext context = AlfAppContextGate.getApplicationContext();
		serviceRegistry = (ServiceRegistry) context.getBean(ServiceRegistry.SERVICE_REGISTRY);
		nodeService = serviceRegistry.getNodeService();

		nodeRef = new NodeRef(MCAlfrescoAPIClient.storeRef, ioId);

		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

		// root element record
		doc = docBuilder.newDocument();
	}

	public void export(String outputDir) {

		QName type = nodeService.getType(nodeRef);

		if (!type.equals(QName.createQName(CCConstants.CCM_TYPE_IO))) {
			logger.error("this was not an io");
			return;
		}
		
		Element lom = doc.createElement("lom");
		doc.appendChild(lom);
		
		lom.setAttribute("xmlns", "http://ltsc.ieee.org/xsd/LOM");
		lom.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
		lom.setAttribute("xsi:schemaLocation", "http://www.w3.org/2001/XMLSchema-instance");
		lom.setAttribute("xsi:schemaLocation", "http://ltsc.ieee.org/xsd/LOM  http://ltsc.ieee.org/xsd/lomv1.0/lom.xsd");

		//general
		Element general = createAndAppendElement("general", lom);
		Element identifier = createAndAppendElement("identifier", general);
		createAndAppendElement("catalog",identifier, ApplicationInfoList.getHomeRepository().getAppId(), false);
		createAndAppendElement("entry",identifier, QName.createQName(CCConstants.SYS_PROP_NODE_UID));
		
		Element titleEle = createAndAppendElement("title", general);
		Element titleStrEle = createAndAppendElement("string", titleEle,QName.createQName(CCConstants.LOM_PROP_GENERAL_TITLE));
		if(titleStrEle != null)titleStrEle.setAttribute("language", "de");
		createAndAppendElement("language", general,QName.createQName(CCConstants.LOM_PROP_GENERAL_LANGUAGE));
		Element descriptionEle = createAndAppendElement("description", general);
		Element descriptionStrEle = createAndAppendElement("string", descriptionEle,QName.createQName(CCConstants.LOM_PROP_GENERAL_DESCRIPTION));
		if(descriptionStrEle != null)descriptionStrEle.setAttribute("language", "de");
		
		Element keywordEle = createAndAppendElement("keyword", general);
		Element keywordStrEle = createAndAppendElement("string", keywordEle,QName.createQName(CCConstants.LOM_PROP_GENERAL_KEYWORD));
		if(keywordStrEle != null)keywordStrEle.setAttribute("language", "de");
		
		//@TODO source
		Element eleStruct = createAndAppendElement("structure", general);
		createAndAppendElement("value",eleStruct,QName.createQName(CCConstants.LOM_PROP_GENERAL_STRUCTURE));
		
		Element eleAggregationLevel = createAndAppendElement("aggregationLevel", general);
		createAndAppendElement("value",eleAggregationLevel,QName.createQName(CCConstants.LOM_PROP_GENERAL_AGGREGATIONLEVEL));
		
		//lifecycle
		Element lifeCycle = createAndAppendElement("lifeCycle", lom);
		createAndAppendElement("version", lifeCycle,QName.createQName(CCConstants.LOM_PROP_LIFECYCLE_VERSION));
		Element status = createAndAppendElement("status", lifeCycle);
		createAndAppendElement("value", status,QName.createQName(CCConstants.LOM_PROP_LIFECYCLE_STATUS));
		
		//@todo autor englisch?, role source
		addContributer(lifeCycle,QName.createQName(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_AUTHOR),"Author");
		
		addContributer(lifeCycle,QName.createQName(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_CONTENT_PROVIDER),"content provider");
		addContributer(lifeCycle,QName.createQName(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_EDITOR),"editor");
		addContributer(lifeCycle,QName.createQName(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_EDUCATIONAL_VALIDATOR),"educational validator");
		addContributer(lifeCycle,QName.createQName(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_GRAPHICAL_DESIGNER),"graphical designer");
		addContributer(lifeCycle,QName.createQName(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_INITIATOR),"initiator");
		addContributer(lifeCycle,QName.createQName(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_INSTRUCTIONAL_DESIGNER),"instructional designer");
		addContributer(lifeCycle,QName.createQName(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_PUBLISHER),"publisher");
		addContributer(lifeCycle,QName.createQName(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_SCRIPT_WRITER),"script writer");
		addContributer(lifeCycle,QName.createQName(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_SUBJECT_MATTER_EXPERT),"subject matter expert");
		addContributer(lifeCycle,QName.createQName(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_TECHNICAL_IMPLEMENTER),"technical implementer");
		addContributer(lifeCycle,QName.createQName(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_TECHNICAL_VALIDATOR),"technical validator");
		addContributer(lifeCycle,QName.createQName(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_TERMINATOR),"terminator");
		addContributer(lifeCycle,QName.createQName(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_UNKNOWN),"unknown");
		addContributer(lifeCycle,QName.createQName(CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_VALIDATOR),"validator");
				
		//metametadata
		Element metaMetadata = createAndAppendElement("metaMetadata", lom);
		addContributer(metaMetadata,QName.createQName(CCConstants.CCM_PROP_IO_REPL_METADATACONTRIBUTER_CREATOR),"creator");
		addContributer(metaMetadata,QName.createQName(CCConstants.CCM_PROP_IO_REPL_METADATACONTRIBUTER_PROVIDER),"provider");
		addContributer(metaMetadata,QName.createQName(CCConstants.CCM_PROP_IO_REPL_METADATACONTRIBUTER_VALIDATOR),"validator");
		
		//technical
		Element technical = createAndAppendElement("technical", lom);
		createAndAppendElement("format",technical,QName.createQName(CCConstants.LOM_PROP_TECHNICAL_FORMAT));
		createAndAppendElement("location",technical,QName.createQName(CCConstants.LOM_PROP_TECHNICAL_LOCATION));
		
		//educational
		Element educational = createAndAppendElement("educational", lom);
		Element lrt = createAndAppendElement("learningResourceType", educational);
		createAndAppendElement("value",lrt,QName.createQName(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_LEARNINGRESSOURCETYPE));
		
		List<String> educationalContext = (List<String>)nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_CONTEXT));
		if(educationalContext != null){
			for(String context : educationalContext){
				Element eduContext = createAndAppendElement("context", educational);
				createAndAppendElement("value", eduContext,context);	
			}
		}
		
		//@todo when its available
		String tarFrom = (String)nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_TYPICALAGERANGEFROM));
		String tarTo = (String)nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_TYPICALAGERANGETO));
		
		if(tarFrom != null && tarTo != null){
			String tar = tarFrom + "-" + tarTo;
			Element typicalAgeRange = createAndAppendElement("typicalAgeRange", educational);
			Element eleString = createAndAppendElement("string", typicalAgeRange,tar);
			eleString.setAttribute("language", "en");
		}
		
		//createAndAppendElement("typicalAgeRange", educational, QName.createQName(CCConstants.LOM_PROP_EDUCATIONAL_TYPICALAGERANGE));
		
		Element rights = createAndAppendElement("rights", lom);
		Element copyrightAndOtherRestrictions = createAndAppendElement("copyrightAndOtherRestrictions", rights);
		List<String> commonLicenceKeyList = (List<String>)nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY)); 
		String commonLicenceKey = (commonLicenceKeyList != null && commonLicenceKeyList.size() > 0) ? commonLicenceKeyList.get(0) : null;
		String commonLicenseVersion = (String)nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_IO_COMMONLICENSE_CC_VERSION)); 
		if(commonLicenceKey != null && !commonLicenceKey.trim().equals("")) {
			
			String urlKey = commonLicenceKey.toLowerCase().replaceAll("_", "-").replaceFirst("cc-", "");
			String url = "https://creativecommons.org/licenses/" + urlKey + "/" + commonLicenseVersion;
			
			createAndAppendElement("value",copyrightAndOtherRestrictions,"yes");
			Element description = createAndAppendElement("description",rights);
			Element rightsDescStrEle = createAndAppendElement("string", description,url);
			if(rightsDescStrEle != null) rightsDescStrEle.setAttribute("language", "de");
			createAndAppendElement("cost", rights,"no");
		
		} else {
			
			createAndAppendElement("value",copyrightAndOtherRestrictions,QName.createQName(CCConstants.LOM_PROP_RIGHTS_COPY_RIGHT));
			createAndAppendElement("description", rights,QName.createQName(CCConstants.LOM_PROP_RIGHTS_RIGHTS_DESCRIPTION));
		}
		
		String thumbnailUrl = (String)nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_IO_THUMBNAILURL));
		if(thumbnailUrl != null){
			Element relation = createAndAppendElement("relation", lom);
			Element kind = createAndAppendElement("kind", relation);
			createAndAppendElement("source", kind,"LOM-DEv1.0");
			createAndAppendElement("value", kind,"hasthumbnail");
			Element resource = createAndAppendElement("resource", relation);
			Element description = createAndAppendElement("description", resource);
			Element string = createAndAppendElement("string", description,thumbnailUrl);
			string.setAttribute("language", "en");
		}
		
		List<String> taxonIds = (List<String>)nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_IO_REPL_TAXON_ID));
		if(taxonIds != null){
			Element classification = createAndAppendElement("classification", lom);
			Element purpose = createAndAppendElement("purpose", classification);
			createAndAppendElement("source", purpose,"LOMv1.0");
			createAndAppendElement("value", purpose,"discipline");
			
			Element taxonPath = createAndAppendElement("taxonPath", classification);
			Element tpSource = createAndAppendElement("source", taxonPath);
			Element tpSourceString = createAndAppendElement("string", tpSource,"EAF Thesaurus");
			tpSourceString.setAttribute("language", "x-t-eaf");
			try{
				List<MetadataSetValueKatalog> mdsValueKata = new MetadataReader().getValuespace("/org/edu_sharing/metadataset/valuespace_eaf_discipline.xml", "org.edu_sharing.metadataset.valuespaces_i18n", null, "{http://www.campuscontent.de/model/1.0}taxonid");
				for(String taxonId:taxonIds){
					Element taxon = createAndAppendElement("taxon", taxonPath);
					createAndAppendElement("id", taxon,taxonId);
					//ask eaf kataolog todo allow other kataloges
					
					for(MetadataSetValueKatalog cata : mdsValueKata){
						if(cata.getKey().equals(taxonId)){
							Element entry = createAndAppendElement("entry", taxon);
							Element string = createAndAppendElement("string", entry,cata.getCaption());
							//<string language="de">Sachkunde</string>
							string.setAttribute("language", "de");
						}
					}
							
				}
			}catch(Throwable e){
				logger.error(e.getMessage(), e);
			}
		}
		
		//formatted output
		try{
			String sourceId = nodeRef.getId();
			
			File f = new File(outputDir + "/" + sourceId +".xml");
			FileOutputStream os = new FileOutputStream(f);
			
			XMLSerializer serializer = new XMLSerializer(os, new OutputFormat(doc,"UTF-8", true));
			serializer.serialize(doc);
		}catch(IOException e){
			logger.error(e.getMessage(),e);
		}

	}
	
	/**
	 * @param eleParent "lifeCycle" or "metaMetadata"
	 * @param contributerProp
	 * @param role
	 */
	public void addContributer(Element eleParent,QName contributerProp,String role){
		Serializable contributer = nodeService.getProperty(nodeRef, contributerProp);
		if(contributer != null && contributer instanceof List){
			
			//sometimes there are empty values in list
			List<String> contrib = (List)contributer;
			boolean hasValidEls = false;
			for(String cont : contrib){
				if(cont != null && !cont.trim().equals("")){
					hasValidEls = true;
				}
			}
			
			if(hasValidEls){
				Element eleContribute = createAndAppendElement("contribute",eleParent);
				Element eleRole = createAndAppendElement("role",eleContribute);
				createAndAppendElement("value",eleRole,role);
				Element eleEntity = createAndAppendElement("entity",eleContribute,contributerProp,true);
			}
			
		}
	}

	public Element createAndAppendElement(String elementName, Element parent) {
		return this.createAndAppendElement(elementName, parent, (String) null);
	}
	
	public Element createAndAppendElement(String elementName, Element parent, QName property){
		return createAndAppendElement(elementName, parent, property,false);
	}

	public Element createAndAppendElement(String elementName, Element parent, QName property, boolean cdata) {
		
		
		if (property != null) {
			Serializable repoValue = nodeService.getProperty(nodeRef, property);
			return this.createAndAppendElement(elementName, parent, repoValue,cdata);
		}
		return null;
	}
	
	/**
	 * returns null when repoValue instanceof List
	 * @param elementName
	 * @param parent
	 * @param repoValue
	 * @return
	 */
	public Element createAndAppendElement(String elementName, Element parent, Serializable repoValue, boolean cdata){
		
		if(repoValue == null){
			return null;
		}
		
		if(repoValue instanceof MLText){
			MLText mlText = (MLText)repoValue;
			Element ele = this.createAndAppendElement(elementName, parent);
			appendMLText(mlText, ele);
			return ele;
		}
		
		
		if(repoValue instanceof List){
			Element ele = null;
			for(Object lval:(List)repoValue){
				ele = this.createAndAppendElement(elementName, parent, (Serializable)lval,cdata);
			}
			return ele;
		}
		
		String value = null;
		if(repoValue instanceof Number){
			value = ((Number)repoValue).toString();
		}else if(repoValue instanceof Date){
			value =  ISO8601DateFormat.format((Date)repoValue);
		}else if(repoValue instanceof String){
			value = (String)repoValue;
		}else if(repoValue instanceof Boolean){
			value = ((Boolean)repoValue).toString();
		}else{
			logger.warn("unknown value type:"+repoValue.getClass().getName());
			value = repoValue.toString();
		}
		
		return this.createAndAppendElement(elementName, parent,value,cdata);
	}
	
	public void appendMLText(MLText mlText, Element mlElement){
		for(Map.Entry<Locale,String> entry : mlText.entrySet()){
			Element langEle = this.createAndAppendElement("string", mlElement,entry.getValue());
			langEle.setAttribute("language", entry.getKey().getLanguage());
		}
	}
	
	public Element createAndAppendElement(String elementName, Element parent, String textContent){
		return this.createAndAppendElement(elementName, parent, textContent,false);
	}

	public Element createAndAppendElement(String elementName, Element parent, String textContent, boolean cdata) {
		Element element = doc.createElement(elementName);
		
		if (textContent != null) {
			textContent = textContent.trim();
			
			if (cdata) {
				element.appendChild(doc.createCDATASection(textContent));
			} else {
				element.setTextContent(textContent);
			}
		}
		parent.appendChild(element);
		return element;
	}
}
