package org.edu_sharing.repository.server.exporter;

import java.io.*;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.MLText;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.metadataset.v2.MetadataKey;
import org.edu_sharing.metadataset.v2.MetadataWidget;
import org.edu_sharing.metadataset.v2.tools.MetadataHelper;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.URLTool;
import org.edu_sharing.repository.server.tools.VCardConverter;
import org.edu_sharing.service.permission.PermissionServiceHelper;
import org.springframework.context.ApplicationContext;
import org.springframework.extensions.surf.util.ISO8601DateFormat;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class OAILOMExporter {

	Logger logger = Logger.getLogger(OAILOMExporter.class);

	ServiceRegistry serviceRegistry = null;

	NodeService nodeService = null;

	NodeRef nodeRef = null;

	Locale nodeLanguage = null;

	Document doc;

	protected String xmlLanguageAttribute = "language";

	public static String configCatalog = "exporter.oai.lom.identifier.catalog";
	protected String lomIdentifierCatalog = (LightbendConfigLoader.get().hasPath(configCatalog)) ? LightbendConfigLoader.get().getString(configCatalog) : ApplicationInfoList.getHomeRepository().getAppId();

	public OAILOMExporter() throws ParserConfigurationException {
		ApplicationContext context = AlfAppContextGate.getApplicationContext();
		serviceRegistry = (ServiceRegistry) context.getBean(ServiceRegistry.SERVICE_REGISTRY);
		nodeService = serviceRegistry.getNodeService();


		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

		// root element record
		doc = docBuilder.newDocument();
	}
	public void export(String outputDir, String ioId) {
		nodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, ioId);
		try {
			String sourceId = nodeRef.getId();

			File f = new File(outputDir + "/" + sourceId + ".xml");
			FileOutputStream os=new FileOutputStream(f);
			write(os,ioId);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
	}
	public void write(OutputStream os,String ioId) {
		nodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, ioId);

		List<String> language = (List<String>)nodeService.getProperty(nodeRef,QName.createQName(CCConstants.LOM_PROP_GENERAL_LANGUAGE));
		nodeLanguage = (language != null && language.size() > 0) ? Locale.forLanguageTag(language.get(0).trim()) : Locale.getDefault();
		if(nodeLanguage == null) nodeLanguage = Locale.getDefault();

		QName type = nodeService.getType(nodeRef);

		if (!type.equals(QName.createQName(CCConstants.CCM_TYPE_IO))) {
			logger.error("this was not an io");
			return;
		}
		
		Element lom = doc.createElement("lom");
		doc.appendChild(lom);

		createNameSpace(lom);

		construction(lom);

		//formatted output
		try{
			XMLSerializer serializer = new XMLSerializer(os, new OutputFormat(doc,"UTF-8", true));
			serializer.serialize(doc);
		}catch(IOException e){
			logger.error(e.getMessage(),e);
		}

	}

	public void createNameSpace(Element lom) {
		lom.setAttribute("xmlns", "http://ltsc.ieee.org/xsd/LOM");
		lom.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
		lom.setAttribute("xsi:schemaLocation", "http://www.w3.org/2001/XMLSchema-instance");
		lom.setAttribute("xsi:schemaLocation", "http://ltsc.ieee.org/xsd/LOM  http://ltsc.ieee.org/xsd/lomv1.0/lom.xsd");
	}

	public void createClassification(Element lom) {
		List<String> taxonIds = (List<String>)nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_IO_REPL_TAXON_ID));
		List<String> classificationKeyword = (List<String>)nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_IO_REPL_CLASSIFICATION_KEYWORD));
		if(taxonIds != null || classificationKeyword != null){
			Element classification = createAndAppendElement("classification", lom);
			Element purpose = createAndAppendElement("purpose", classification);
			createAndAppendElement("source", purpose,"LOMv1.0");
			createAndAppendElement("value", purpose,"discipline");

			if(classificationKeyword != null) {
				for(String kw : classificationKeyword) {
					Element keyword = createAndAppendElement("keyword", classification);
					Element kwStrEle = createAndAppendElement("string", keyword,kw);
					if(kwStrEle != null)kwStrEle.setAttribute(xmlLanguageAttribute, "de");
				}
			}

			if(taxonIds != null) {
				Element taxonPath = createAndAppendElement("taxonPath", classification);
				Element tpSource = createAndAppendElement("source", taxonPath);
				Element tpSourceString = createAndAppendElement("string", tpSource,"EAF Thesaurus");
				tpSourceString.setAttribute(xmlLanguageAttribute, "x-t-eaf");
				try{
					MetadataWidget widget = MetadataHelper.getLocalDefaultMetadataset().findWidget("ccm:taxonid");
					Map<String, MetadataKey> values = widget.getValuesAsMap();
					if(values!=null){
						for(String taxonId:taxonIds){
							Element taxon = createAndAppendElement("taxon", taxonPath);
							createAndAppendElement("id", taxon,taxonId);
							//ask eaf kataolog todo allow other kataloges

							for(Map.Entry<String,MetadataKey> cata : values.entrySet()){
								if(cata.getKey().equals(taxonId)){
									Element entry = createAndAppendElement("entry", taxon);
									Element string = createAndAppendElement("string", entry,cata.getValue().getCaption());
									//<string language="de">Sachkunde</string>
									string.setAttribute(xmlLanguageAttribute, "de");
								}
							}

						}
					}
				}catch(Throwable e){
					logger.error(e.getMessage(), e);
				}
			}
		}
	}

	public void createThumbnail(Element lom) {
		//String thumbnailUrl = (String)nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_IO_THUMBNAILURL));
		String thumbnailUrl = URLTool.getPreviewServletUrl(nodeRef);
		if(thumbnailUrl != null){
			Element relation = createAndAppendElement("relation", lom);
			Element kind = createAndAppendElement("kind", relation);
			createAndAppendElement("source", kind,"LOM-DEv1.0");
			createAndAppendElement("value", kind,"hasthumbnail");
			Element resource = createAndAppendElement("resource", relation);
			Element description = createAndAppendElement("description", resource);
			Element string = createAndAppendElement("string", description,thumbnailUrl);
			string.setAttribute(xmlLanguageAttribute, "en");
		}
	}

	public void createRights(Element lom) {
		Element rights = createAndAppendElement("rights", lom);
		Element copyrightAndOtherRestrictions = createAndAppendElement("copyrightAndOtherRestrictions", rights);
		List<String> commonLicenceKeyList = (List<String>)nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY));
		String commonLicenceKey = (commonLicenceKeyList != null && commonLicenceKeyList.size() > 0) ? commonLicenceKeyList.get(0) : null;
		String commonLicenseVersion = (String)nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_IO_COMMONLICENSE_CC_VERSION));
		if(commonLicenceKey != null && !commonLicenceKey.trim().equals("")) {

			String urlKey = commonLicenceKey.toLowerCase().replaceAll("_", "-").replaceFirst("cc-", "");
			String url = "https://creativecommons.org/licenses/" + urlKey + "/" + commonLicenseVersion;

			if(commonLicenceKey.equals(CCConstants.COMMON_LICENSE_CC_ZERO)) {
				url = CCConstants.COMMON_LICENSE_CC_ZERO_LINK.replace("deed.${locale}", "legalcode");
			}

			createAndAppendElement("value",copyrightAndOtherRestrictions,"yes");
			Element description = createAndAppendElement("description",rights);
			Element rightsDescStrEle = createAndAppendElement("string", description,url);
			if(rightsDescStrEle != null) rightsDescStrEle.setAttribute(xmlLanguageAttribute, "de");
			createAndAppendElement("cost", rights,"no");

		} else {

			createAndAppendElement("value",copyrightAndOtherRestrictions,QName.createQName(CCConstants.LOM_PROP_RIGHTS_COPY_RIGHT));
			createAndAppendElement("description", rights,QName.createQName(CCConstants.LOM_PROP_RIGHTS_RIGHTS_DESCRIPTION));
		}
	}

	public void construction(Element lom) {
		Element general = createGeneral(lom);
		createTitle(general);
		createLanguage(general);
		createDescription(general);
		createKeyword(general);
		createStructure(general);
		createAggregationLevel(general);
		createLifecycle(lom);
		createMetadata(lom);
		createTechnical(lom);
		createEducational(lom);
		createRights(lom);
		createThumbnail(lom);
		createClassification(lom);
	}

	public void createStructure(Element general) {
		Element eleStruct = createAndAppendElement("structure", general);
		createAndAppendElement("value",eleStruct, QName.createQName(CCConstants.LOM_PROP_GENERAL_STRUCTURE));
	}

	public Element createGeneral(Element lom) {
		//general
		Element general = createAndAppendElement("general", lom);
		createIdentifier(general);
		createHandle(general);
		return general;
	}

	public Element createIdentifier(Element general){
		Element identifier = createAndAppendElement("identifier", general);
		createAndAppendElement("catalog",identifier,lomIdentifierCatalog, false);
		createAndAppendElement("entry",identifier, QName.createQName(CCConstants.SYS_PROP_NODE_UID));
		return identifier;
	}

	public void createEducational(Element lom) {
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

		Element ieur = createAndAppendElement("intendedEndUserRole", educational);
		createAndAppendElement("value",ieur,QName.createQName(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_INTENDEDENDUSERROLE));

		//@todo when its available
		Integer tarFrom = (Integer)nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_TYPICALAGERANGEFROM));
		Integer tarTo = (Integer)nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_TYPICALAGERANGETO));

		if(tarFrom != null && tarTo != null){
			String tar = tarFrom + "-" + tarTo;
			Element typicalAgeRange = createAndAppendElement("typicalAgeRange", educational);
			Element eleString = createAndAppendElement("string", typicalAgeRange,tar);
			eleString.setAttribute(xmlLanguageAttribute, nodeLanguage.getLanguage());
		}

		//createAndAppendElement("typicalAgeRange", educational, QName.createQName(CCConstants.LOM_PROP_EDUCATIONAL_TYPICALAGERANGE));
	}

	public void createTechnical(Element lom) {
		//technical -> first is the "real" binary data
		Element technical;
		Serializable format = nodeService.getProperty(nodeRef, QName.createQName(CCConstants.LOM_PROP_TECHNICAL_FORMAT));
		if(format!=null) {
			technical = createAndAppendElement("technical", lom);
			createAndAppendElement("format", technical, QName.createQName(CCConstants.LOM_PROP_TECHNICAL_FORMAT));
			createAndAppendElement("location", technical, URLTool.getDownloadServletUrl(nodeRef.getId(), null, false));
		}
		// second is text/html for rendering
		technical = createAndAppendElement("technical", lom);
		createAndAppendElement("format",technical,"text/html");
		createAndAppendElement("location",technical,URLTool.getNgRenderNodeUrl(nodeRef.getId(),null));
	}

	public void createMetadata(Element lom) {
		//metametadata
		Element metaMetadata = createAndAppendElement("metaMetadata", lom);
		addContributer(metaMetadata,QName.createQName(CCConstants.CCM_PROP_IO_REPL_METADATACONTRIBUTER_CREATOR),"creator");
		addContributer(metaMetadata,QName.createQName(CCConstants.CCM_PROP_IO_REPL_METADATACONTRIBUTER_PROVIDER),"provider");
		addContributer(metaMetadata,QName.createQName(CCConstants.CCM_PROP_IO_REPL_METADATACONTRIBUTER_VALIDATOR),"validator");
	}

	public void createLifecycle(Element lom) {
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
	}

	public void createAggregationLevel(Element general) {
		Element eleAggregationLevel = createAndAppendElement("aggregationLevel", general);
		createAndAppendElement("value",eleAggregationLevel, QName.createQName(CCConstants.LOM_PROP_GENERAL_AGGREGATIONLEVEL));
	}

	public void createKeyword(Element general) {
		Element keywordEle = createAndAppendElement("keyword", general);
		Element keywordStrEle = createAndAppendElement("string", keywordEle, QName.createQName(CCConstants.LOM_PROP_GENERAL_KEYWORD));
		if(keywordStrEle != null)keywordStrEle.setAttribute(xmlLanguageAttribute, nodeLanguage.getLanguage());
	}

	public void createDescription(Element general) {
		Element descriptionEle = createAndAppendElement("description", general);
		Element descriptionStrEle = createAndAppendElement("string", descriptionEle, QName.createQName(CCConstants.LOM_PROP_GENERAL_DESCRIPTION));
		if(descriptionStrEle != null)descriptionStrEle.setAttribute(xmlLanguageAttribute, nodeLanguage.getLanguage());
	}

	public void createLanguage(Element general) {
		createAndAppendElement(xmlLanguageAttribute, general,QName.createQName(CCConstants.LOM_PROP_GENERAL_LANGUAGE));
	}

	public void createTitle(Element general) {
		Element titleEle = createAndAppendElement("title", general);
		String title= (String) nodeService.getProperty(nodeRef,QName.createQName(CCConstants.LOM_PROP_GENERAL_TITLE));
		if(title==null || title.isEmpty()){
			title= (String) nodeService.getProperty(nodeRef,QName.createQName(CCConstants.CM_NAME));
		}
		Element titleStrEle = createAndAppendElement("string", titleEle,title);
		if(titleStrEle != null)titleStrEle.setAttribute(xmlLanguageAttribute, nodeLanguage.getLanguage());
	}


	public String cleanupVCardEMail(String vCard){
		if(LightbendConfigLoader.get().getBoolean("repository.privacy.filterVCardEmail") &&
				!PermissionServiceHelper.hasPermission(nodeRef, CCConstants.PERMISSION_WRITE)) {
			return VCardConverter.removeEMails(vCard);
		}
		return vCard;
	}

	protected List<String> prepareContributer(List<String> contrib){
		if(contrib instanceof List) {
			return contrib.stream().
					//sometimes there are empty values in list
							filter((c) -> c != null && !c.trim().isEmpty()).
					// validate email ppolicy
							map(this::cleanupVCardEMail)
					.collect(Collectors.toList());
		}
		return new ArrayList<>();
	}

	/**
	 * @param eleParent "lifeCycle" or "metaMetadata"
	 * @param contributerProp
	 * @param role
	 */
	public Element addContributer(Element eleParent,QName contributerProp,String role){
		Serializable contributer = nodeService.getProperty(nodeRef, contributerProp);

		Element eleContribute = null;
		if(contributer != null && contributer instanceof List){
			List contributerClean = prepareContributer((List) contributer);
			
			if(contributerClean.size() > 0){
				eleContribute = createAndAppendElement("contribute",eleParent);
				Element eleRole = createAndAppendElement("role",eleContribute);
				createAndAppendElement("value",eleRole,role);
				createAndAppendElement("entity", eleContribute, (Serializable) contributerClean,true);
			}
			
		}
		return eleContribute;
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
     *
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
            for (Object lval : (List) repoValue)
				ele = this.createAndAppendElement(elementName, parent, (Serializable)lval,cdata);
			return ele;
		}
		
		String value = null;
		if(repoValue instanceof Number){
			value = ((Number)repoValue).toString();
		}else if(repoValue instanceof Date){
			value =  ISO8601DateFormat.format((Date)repoValue);
        } else if (repoValue instanceof Duration) {
            long durSecs = ((Duration) repoValue).getSeconds();
            value = String.format("%02d:%02d:%02d", (durSecs / 3600) % 24, (durSecs / 60) % 60, durSecs % 60);
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
			if(entry.getKey() != null){
				langEle.setAttribute(xmlLanguageAttribute, entry.getKey().getLanguage());
			}
		}
	}

	
	public Element createAndAppendElement(String elementName, Element parent, String textContent){
		return this.createAndAppendElement(elementName, parent, textContent,false);
	}

	public Element createAndAppendElement(String elementName, Element parent, String textContent, boolean cdata) {
        if (elementName != null && !elementName.isEmpty()) {
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
        return null;
    }

    public Element createAndAppendElementLangStr(String elementName, Element parent, QName textProp, Locale locale) {
        List<MLText> deTextLst = new ArrayList<>();
        Serializable textLst = nodeService.getProperty(nodeRef, textProp);
        if (textLst != null) {


            if (textLst instanceof List) {
                for (String deText : (List<String>) textLst)
                    if (!deText.isEmpty())
                        deTextLst.add(new MLText(locale, deText));
            } else if (textLst instanceof String)
                deTextLst.add(new MLText(locale, (String) textLst));
            return this.createAndAppendElement(elementName, parent, (Serializable) deTextLst, false);
        }
        return null;
    }

   


	/**
	 * the concrete ones
	 */
	public Element createAndAppendElementSrcVal(String elementName, Element parent, QName property, String src) {
		if (property != null) {
			Serializable repoValue = nodeService.getProperty(nodeRef, property);
			return this.createAndAppendElementSrcVal(elementName, parent, repoValue, src);
		}
		return null;
	}

	public Element createAndAppendElementSrcVal(String elementName, Element parent, Serializable repoValue, String src) {
		if (repoValue != null) {
			Element langEle = null;

			if (repoValue instanceof List) {
				Element ele = null;
				for (Object lval : (List) repoValue)
					ele = this.createAndAppendElementSrcVal(elementName, parent, (Serializable) lval, src);
				return ele;
			}

			Element element = createAndAppendElement(elementName, parent);
			if (src != null) {
				Element srcEle = createAndAppendElement("source", element);
				langEle = createAndAppendElement("langstring", srcEle, src, false);
				if (langEle != null)
					langEle.setAttribute(xmlLanguageAttribute, "x-none");
			}
			Element valEle = createAndAppendElement("value", element);
			if (valEle != null) {
				langEle = createAndAppendElement("langstring", valEle, repoValue, false);
				if (langEle != null)
					langEle.setAttribute(xmlLanguageAttribute, "x-none");
			}
			return element;
		}
		return null;
	}

	public Element addDateTime(String elementName, Element eleParent, String dateCC, MLText desc) {
		Element ele = null;
		if (eleParent != null && !elementName.isEmpty() && !dateCC.isEmpty()) {
			ele = doc.createElement(elementName);

			Serializable repoValue = nodeService.getProperty(nodeRef, QName.createQName(dateCC));
			if (repoValue != null && repoValue instanceof String) {
				if (((String) repoValue).startsWith("P"))
					repoValue = Duration.parse((String) repoValue);
			}

			Element dtEle = createAndAppendElement("datetime", ele, repoValue, false);
			if (dtEle != null) {
				if (desc != null && !desc.isEmpty())
					createAndAppendElement("description", ele, desc, false);
				eleParent.appendChild(ele);
			}
		}
		return ele;
	}

	/**
	 * @param eleParent - the parent taxonPath (top level) or taxon element
	 * @param cata      - the catalog value with id and name for the taxon
	 * @return the created taxon element
	 * Recursively builds the taxonpath hierarchy.
	 */
	public Element createTaxon(Element eleParent, MetadataKey cata, List<MetadataKey> valueSpace) {
		MetadataKey cataPar = getParent(cata,valueSpace);
		if (cataPar != null)
			createTaxon(eleParent, cataPar,valueSpace);
		Element taxonEle = createAndAppendElement("taxon", eleParent);
		createAndAppendElement("id", taxonEle, cata.getKey());
		// add taxon name as langstring
		MLText desc = new MLText(Locale.ROOT, cata.getCaption());
		createAndAppendElement("entry", taxonEle, desc, false);
		return taxonEle;
	}

	public Element createHandle(Element eleParent){
		Element identifierHandle = null;
		String handleId = (String)nodeService.getProperty(nodeRef,QName.createQName(CCConstants.CCM_PROP_PUBLISHED_HANDLE_ID));
		if(handleId != null && !handleId.trim().equals("")){
			identifierHandle = createAndAppendElement("identifier", eleParent);
			// fixed values: 'HDL' - Handle, 'DOI' or 'URN'
			createAndAppendElement("catalog",identifierHandle, "HDL", false);
			createAndAppendElement("entry",identifierHandle, QName.createQName(CCConstants.CCM_PROP_PUBLISHED_HANDLE_ID));
		}
		return identifierHandle;
	}

	private MetadataKey getParent(MetadataKey cata, List<MetadataKey> valueSpace){
		for(MetadataKey mdk : valueSpace){
			if(mdk.getKey().equals(cata.getParent())){
				return mdk;
			}
		}
		return null;
	}

}
