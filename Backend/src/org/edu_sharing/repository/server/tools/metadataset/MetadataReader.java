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
package org.edu_sharing.repository.server.tools.metadataset;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSet;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetBaseProperty;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetFormsForm;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetFormsPanel;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetFormsProperty;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetList;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetListProperty;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetModelChild;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetModelProperty;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetModelType;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetQueries;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetQuery;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetQueryProperty;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetValue;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetValueKatalog;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetView;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetViewProperty;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSets;
import org.edu_sharing.repository.client.rpc.metadataset.Validator;
import org.edu_sharing.repository.client.rpc.metadataset.ValidatorIntField;
import org.edu_sharing.repository.client.rpc.metadataset.ValidatorMandatoryField;
import org.edu_sharing.repository.client.rpc.metadataset.ValidatorMinChars;
import org.edu_sharing.repository.client.rpc.metadataset.ValidatorTitleField;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.XMLTool;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author rudolph
 */
public class MetadataReader {

	private static Log logger = LogFactory.getLog(MetadataReader.class);
	String metadatasetsFile = CCConstants.metadatasetsdefault;

	XPathFactory pfactory = XPathFactory.newInstance();
	XPath xpath = pfactory.newXPath();

	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

	public MetadataSets getMetadataSets() {
		return getMetadataSets(metadatasetsFile,ApplicationInfoList.getHomeRepository().getAppId());
	}

	public MetadataSets getMetadataSets(String file, String repositoryId) {
		logger.info("loading metadatasets: " + file);
		try {
			DocumentBuilder builder = factory.newDocumentBuilder();

			URL url = MetadataReader.class.getResource(file);
			Document docMdSets = builder.parse(url.openStream());

			NodeList nodeList = (NodeList) xpath.evaluate("/metadatasets/metadataset", docMdSets,
					XPathConstants.NODESET);

			List<MetadataSet> metaDataSetList = new ArrayList<MetadataSet>();

			for (int mdsetIdx = 0; mdsetIdx < nodeList.getLength(); mdsetIdx++) {
				Node nodeMetadataSet = nodeList.item(mdsetIdx);

				String mdsfile = new XMLTool().getTextContent(nodeMetadataSet);
				MetadataSet mdset = getMetadataSet(mdsfile, repositoryId);
				if (mdset != null) {
					metaDataSetList.add(mdset);
				}
			}
			MetadataSets mdSets = new MetadataSets();
			mdSets.setMetadataSets(metaDataSetList);
			return mdSets;

		} catch (Throwable e) {
			logger.error(e.getMessage(), e);
			e.printStackTrace();
		}

		return null;
	}

	public MetadataSet getMetadataSet(String file) throws Throwable {
		return this.getMetadataSet(file, ApplicationInfoList.getHomeRepository().getAppId());
	}
	
	public MetadataSet getMetadataSet(String file, String repositoryId) throws Throwable {
		URL url = MetadataReader.class.getResource(file);
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse(url.openStream());

		MetadataSet metadataSet = new MetadataSet();

		Node nodeMetadataSet = (Node) xpath.evaluate("/metadataset", doc, XPathConstants.NODE);
		String id = nodeMetadataSet.getAttributes().getNamedItem("id").getNodeValue();
		String label = nodeMetadataSet.getAttributes().getNamedItem("label").getNodeValue();

		String hidden = (String)xpath.evaluate("/metadataset/@hidden", doc, XPathConstants.STRING);
		if(hidden == null) metadataSet.setHidden(false); else metadataSet.setHidden(new Boolean(hidden));
		
		metadataSet.setId(id);
		metadataSet.setLabel(label);

		// model
		List<MetadataSetModelType> metadataSetModelList = getMetadataModelList(doc);
		if (metadataSetModelList != null) {
			metadataSet.setMetadataSetModelTypes(metadataSetModelList);
		}

		// forms
		List<MetadataSetFormsForm> metadataSetGuiList = getMetadataSetGuiList(doc,repositoryId,id);
		if (metadataSetGuiList != null) {
			metadataSet.setMetadataSetForms(metadataSetGuiList);
		}

		// list
		List<MetadataSetList> metadataSetList = getMetadataSetListList(doc,repositoryId,id);
		if (metadataSetList != null) {
			metadataSet.setMetadataSetLists(metadataSetList);
		}

		// view
		List<MetadataSetView> metadataSetViewList = getMetadataSetViewList(doc,repositoryId,id);
		if (metadataSetViewList != null) {
			metadataSet.setMetadataSetViews(metadataSetViewList);
		}
		
		MetadataSetQueries metadataSetQueries = new MetadataSetQueries();
													
		String statementsearchword =  (String)xpath.evaluate("/metadataset/queries/statementsearchword", doc, XPathConstants.STRING);
		if(statementsearchword != null && !statementsearchword.equals("")){
			metadataSetQueries.setStatementsearchword(statementsearchword);
		}
		
		String basequery = (String)xpath.evaluate("/metadataset/queries/basequery", doc, XPathConstants.STRING);
		if(basequery != null && !basequery.trim().equals("")){
			metadataSetQueries.setBasequery(basequery);
		}
		
		List<MetadataSetQuery> metadataSetQueryList = getMetadataSetQueryList(doc,repositoryId,id);
		if(metadataSetQueryList != null){		
			metadataSetQueries.setMetadataSetQueries(metadataSetQueryList);
		}
		
		String allowSearchWithoutCriteria = (String)xpath.evaluate("/metadataset/queries/allow_search_without_criteria", doc, XPathConstants.STRING);
		metadataSetQueries.setAllowSearchWithoutCriteria(allowSearchWithoutCriteria);
		metadataSet.setMetadataSetQueries(metadataSetQueries);
		
		return metadataSet;
	}

	public List<MetadataSetFormsForm> getMetadataSetGuiList(Document doc, String repositoryId, String metadatasetId) throws Throwable {

		List<MetadataSetFormsForm> metadataSetGuiFormList = new ArrayList<MetadataSetFormsForm>();

		Node i18nNode = (Node) xpath.evaluate("/metadataset/i18n", doc, XPathConstants.NODE);
		String i18nRessource = new XMLTool().getTextContent(i18nNode);

		NodeList nodeListForm = (NodeList) xpath.evaluate("/metadataset/forms/form", doc, XPathConstants.NODESET);

		for (int formIdx = 0; formIdx < nodeListForm.getLength(); formIdx++) {
			Node nodeForm = nodeListForm.item(formIdx);

			MetadataSetFormsForm mdsgForm = new MetadataSetFormsForm();
			
			// id (it's not a real id, the combination of association and id would be one)
			Node attFormId = nodeForm.getAttributes().getNamedItem("id");
			if (attFormId != null) {
				String formId = attFormId.getNodeValue();
				mdsgForm.setId(formId);
			}
			// childassociation
			Node attFormChildassoc = nodeForm.getAttributes().getNamedItem("childassoc");
			if (attFormChildassoc != null) {
				mdsgForm.setChildAssociation(attFormChildassoc.getNodeValue());
			}
			metadataSetGuiFormList.add(mdsgForm);

			NodeList nodeListPanel = (NodeList) xpath.evaluate("panel", nodeForm, XPathConstants.NODESET);

			List<MetadataSetFormsPanel> metadataSetGuiPanelList = new ArrayList<MetadataSetFormsPanel>();

			for (int panelIdx = 0; panelIdx < nodeListPanel.getLength(); panelIdx++) {
				MetadataSetFormsPanel metadataSetGuiPanel = new MetadataSetFormsPanel();
				metadataSetGuiPanelList.add(metadataSetGuiPanel);

				Node nodePanel = nodeListPanel.item(panelIdx);
				String panelName = nodePanel.getAttributes().getNamedItem("name").getNodeValue();
				
				Node multiUploadNode = nodePanel.getAttributes().getNamedItem("multiupload");
				if(multiUploadNode != null){
					String multiUpload = multiUploadNode.getNodeValue();
					metadataSetGuiPanel.setMultiupload(new Boolean(multiUpload));
				}
				
				Node nodeStyleName = nodePanel.getAttributes().getNamedItem("styleName");
				if(nodeStyleName != null){
					String styleName = nodeStyleName.getNodeValue();
					if(styleName != null && !styleName.trim().equals("")){
						metadataSetGuiPanel.setStyleName(styleName);
					}
				}
				
				Node nodeLayoutpath = nodePanel.getAttributes().getNamedItem("layoutPath");
				if(nodeLayoutpath != null){
					String layoutPath = nodeLayoutpath.getNodeValue();
					if(layoutPath != null && !layoutPath.trim().equals("")){
						metadataSetGuiPanel.setLayout(layoutPath);
					}
				}
				
				String order = (String) xpath.evaluate("order", nodePanel, XPathConstants.STRING);
				if (order != null) {
					metadataSetGuiPanel.setOrder(order);
				}
				metadataSetGuiPanel.setName(panelName);

				// panel label
				Node nodeLabelKey = (Node) xpath.evaluate("labelkey", nodePanel, XPathConstants.NODE);
				if (nodeLabelKey != null) {
					String labelKey = new XMLTool().getTextContent(nodeLabelKey);
					if (labelKey != null) {
						MetadataSetValue label = new MetadataSetValue();
						label.setKey(labelKey);
						label.setI18n(getI18nMap(i18nRessource, labelKey));
						metadataSetGuiPanel.setLabel(label);
					}
				}
				
				String oncreate = (String) xpath.evaluate("oncreate", nodePanel, XPathConstants.STRING);
				if (oncreate != null && !oncreate.equals(""))
					metadataSetGuiPanel.setOncreate(new Boolean(oncreate));

				String onupdate = (String) xpath.evaluate("onupdate", nodePanel, XPathConstants.STRING);
				if (onupdate != null && !onupdate.equals(""))
					metadataSetGuiPanel.setOnupdate(new Boolean(onupdate));
				
				String onmultiupload = (String) xpath.evaluate("onmultiupload", nodePanel, XPathConstants.STRING);
				if (onmultiupload != null && !onmultiupload.equals(""))
					metadataSetGuiPanel.setOnmultiupload(new Boolean(onmultiupload));
				
				String onsingleupload = (String) xpath.evaluate("onsingleupload", nodePanel, XPathConstants.STRING);
				if (onsingleupload != null && !onsingleupload.equals(""))
					metadataSetGuiPanel.setOnsingleupload(new Boolean(onsingleupload));
				

				NodeList nodeListProperties = (NodeList) xpath.evaluate("properties/property", nodePanel,
						XPathConstants.NODESET);
				List<MetadataSetFormsProperty> metadataSetGuiPropertyList = new ArrayList<MetadataSetFormsProperty>();
				metadataSetGuiPanel.setProperties(metadataSetGuiPropertyList);
				for (int propIdx = 0; propIdx < nodeListProperties.getLength(); propIdx++) {
					MetadataSetFormsProperty metadataSetGuiProperty = new MetadataSetFormsProperty();
					metadataSetGuiPropertyList.add(metadataSetGuiProperty);
					Node nodeProperty = nodeListProperties.item(propIdx);
					String propName = nodeProperty.getAttributes().getNamedItem("name").getNodeValue();
					logger.info("Panel:" + panelName + " property:" + propName);
					metadataSetGuiProperty.setName(propName);
					metadataSetGuiProperty.setParent(metadataSetGuiPanel);
					
					//put property in cache
					int propertyId = createPropertyId(repositoryId, metadatasetId, metadataSetGuiPanel.getClass().getSimpleName(), panelIdx, propName, propIdx);
					metadataSetGuiProperty.setId(new Integer(propertyId));
					MetadataCache.add(metadataSetGuiProperty);

					NodeList propChildren = nodeProperty.getChildNodes();
					String valuespace = null;
					String valuespace18n = null;
					String valuespace_i18n_prefix = null;
					String valuespace_key = null;
					List<Validator> validators = null;

					for (int propChildIdx = 0; propChildIdx < propChildren.getLength(); propChildIdx++) {
						Node nodePropChild = propChildren.item(propChildIdx);
						String xmlEleName = nodePropChild.getNodeName();
						String textContent = new XMLTool().getTextContent(nodePropChild);
						// type widget valuespace valuespace_i18n
						if (xmlEleName != null && xmlEleName.equals("type") && textContent != null) {
							metadataSetGuiProperty.setType(textContent);

						}
						if (xmlEleName != null && xmlEleName.equals("widget") && textContent != null) {
							metadataSetGuiProperty.setWidget(textContent);
						}
						
						if (xmlEleName != null && xmlEleName.equals("widget_title") && textContent != null) {
							MetadataSetValue widget_titleMdsv = new MetadataSetValue();
							widget_titleMdsv.setKey(textContent);
							widget_titleMdsv.setI18n(getI18nMap(i18nRessource, textContent));
							metadataSetGuiProperty.setWidgetTitle(widget_titleMdsv);
						}
						
						if (xmlEleName != null && xmlEleName.equals("multiple") && textContent != null) {
							metadataSetGuiProperty.setMultiple(new Boolean(textContent));
						}
						
						if (xmlEleName != null && xmlEleName.equals("stylename") && textContent != null) {
							metadataSetGuiProperty.setStyleName(textContent);
						}
						
						if (xmlEleName != null && xmlEleName.equals("stylenamelabel") && textContent != null) {
							metadataSetGuiProperty.setStyleNameLabel(textContent);
						}
						
						if (xmlEleName != null && xmlEleName.equals("defaultvalues") && textContent != null) {
							metadataSetGuiProperty.setDefaultValues(textContent.split(","));
						}
						
						// valuespace
						if (xmlEleName != null && xmlEleName.equals("valuespace") && textContent != null) {
							valuespace = textContent;
						}
						if (xmlEleName != null && xmlEleName.equals("valuespace_i18n") && textContent != null) {
							valuespace18n = textContent;
						}
						if (xmlEleName != null && xmlEleName.equals("valuespace_i18n_prefix") && textContent != null) {
							valuespace_i18n_prefix = textContent;
						}
						if (xmlEleName != null && xmlEleName.equals("valuespace_key") && textContent != null) {
							valuespace_key = textContent;
						}

						if (xmlEleName != null && xmlEleName.equals("formlength") && textContent != null) {
							metadataSetGuiProperty.setFormlength(textContent);
						}

						if (xmlEleName != null && xmlEleName.equals("formheight") && textContent != null) {
							metadataSetGuiProperty.setFormheight(textContent);
						}

						if (xmlEleName != null && xmlEleName.equals("copyfrom") && textContent != null) {
							metadataSetGuiProperty.setCopyfrom(textContent);
						}

						if (xmlEleName != null && xmlEleName.equals("labelkey") && textContent != null) {
							MetadataSetValue metadataSetValue = new MetadataSetValue();
							metadataSetValue.setKey(textContent);
							metadataSetValue.setI18n(getI18nMap(i18nRessource, textContent));
							metadataSetGuiProperty.setLabel(metadataSetValue);
						}
						
						if (xmlEleName != null && xmlEleName.equals("labelhint") && textContent != null) {
							MetadataSetValue metadataSetValue = new MetadataSetValue();
							metadataSetValue.setKey(textContent);
							metadataSetValue.setI18n(getI18nMap(i18nRessource, textContent));
							metadataSetGuiProperty.setLabelHint(metadataSetValue);
						}
						
						if (xmlEleName != null && xmlEleName.equals("placeholder") && textContent != null) {
							MetadataSetValue metadataSetValue = new MetadataSetValue();
							metadataSetValue.setKey(textContent);
							metadataSetValue.setI18n(getI18nMap(i18nRessource, textContent));
							metadataSetGuiProperty.setPlaceHolder(metadataSetValue);
						}
						
						//sets the display none by default. toggling is done by the widget it is placed in.
						if (xmlEleName != null && xmlEleName.equals("toggle") && textContent != null) {
							metadataSetGuiProperty.setToggle(textContent);
						}
						
						// validators
						if (xmlEleName != null && xmlEleName.equals("validators")) {
							validators = getValidators(nodePropChild);
						}
					}
					if (valuespace != null) {

						valuespace_key = (valuespace_key == null) ? propName : valuespace_key;

						valuespace18n = (valuespace18n == null) ? i18nRessource : valuespace18n;

						List<MetadataSetValueKatalog> valuespaceList = this.getValuespace(valuespace, valuespace18n,
								valuespace_i18n_prefix, valuespace_key);

						if (valuespaceList != null) {
							metadataSetGuiProperty.setValuespace(valuespaceList);
						}
					}
					if (validators != null && validators.size() > 0) {
						metadataSetGuiProperty.setValidators(validators);
					}
					
					this.setPropertyParams(nodeProperty, metadataSetGuiProperty);
					
				}

			}
			mdsgForm.setPanels(metadataSetGuiPanelList);
		}

		return metadataSetGuiFormList;
	}
	
	public  List<Validator> getValidators(Node nodeValidators) throws XPathExpressionException{
		List<Validator> validators = new ArrayList<Validator>();
		NodeList nodeListValidators = (NodeList) xpath.evaluate("validator", nodeValidators, XPathConstants.NODESET);
		for (int i = 0; i < nodeListValidators.getLength(); i++) {
			Node nodeValidator = nodeListValidators.item(i);
			String validatorType = nodeValidator.getAttributes().getNamedItem("type")
					.getNodeValue();
			if (validatorType != null) {
				// TODO available Validators somewhere else,
				// and more generic its late today
				if (validatorType.equals(Validator.MANDATORY)) {
					validators.add(new ValidatorMandatoryField());
				}

				if (validatorType.equals(Validator.INT)) {
					validators.add(new ValidatorIntField());
				}
				
				if(validatorType.equals(Validator.MINCHARS)){
					validators.add(new ValidatorMinChars());
				}
				
				if (validatorType.equals(Validator.MANDATORYTITLE)) {
					validators.add(new ValidatorTitleField());
				}
			}
		}
		
		return validators;
	}

	public List<MetadataSetModelType> getMetadataModelList(Document doc) throws Throwable {

		NodeList nodeList = (NodeList) xpath.evaluate("/metadataset/model/type", doc, XPathConstants.NODESET);
		List<MetadataSetModelType> metadataModelList = new ArrayList<MetadataSetModelType>();

		// model types
		for (int typeIdx = 0; typeIdx < nodeList.getLength(); typeIdx++) {
			MetadataSetModelType metadataSetModel = new MetadataSetModelType();
			metadataModelList.add(metadataSetModel);

			Node typeNode = nodeList.item(typeIdx);

			metadataSetModel.setType(typeNode.getAttributes().getNamedItem("name").getNodeValue());

			NodeList propertiesList = (NodeList) xpath
					.evaluate("properties/property", typeNode, XPathConstants.NODESET);

			List<MetadataSetModelProperty> metaDataSetPropList = new ArrayList<MetadataSetModelProperty>();

			// model type properties
			for (int propIdx = 0; propIdx < propertiesList.getLength(); propIdx++) {
				Node propertyNode = propertiesList.item(propIdx);

				Node attName = propertyNode.getAttributes().getNamedItem("name");
				if (attName != null) {
					MetadataSetModelProperty metadataSetModelProperty = new MetadataSetModelProperty();
					String propertyName = attName.getNodeValue();

					metadataSetModelProperty.setName(propertyName);

					NodeList propChildren = propertyNode.getChildNodes();
					for (int propChildIdx = 0; propChildIdx < propChildren.getLength(); propChildIdx++) {
						Node propChild = propChildren.item(propChildIdx);
						String xmlEleName = propChild.getNodeName();

						String textContent = new XMLTool().getTextContent(propChild);

						if (xmlEleName != null && xmlEleName.equals("datatype") && textContent != null) {
							metadataSetModelProperty.setDatatype(textContent);
						}
						if (xmlEleName != null && xmlEleName.equals("processtype") && textContent != null) {
							metadataSetModelProperty.setProcesstype(textContent);
						}
						if (xmlEleName != null && xmlEleName.equals("processtype") && textContent != null) {
							metadataSetModelProperty.setProcesstype(textContent);
						}
						if (xmlEleName != null && xmlEleName.equals("copyfrom") && textContent != null) {
							metadataSetModelProperty.setCopyprop(textContent);
						}
						if (xmlEleName != null && xmlEleName.equals("defaultvalue") && textContent != null) {
							metadataSetModelProperty.setDefaultValue(textContent);
						}
						if (xmlEleName != null && xmlEleName.equals("concatewithtype") && textContent != null) {
							metadataSetModelProperty.setConcatewithtype(new Boolean(textContent));
						}
						if (xmlEleName != null && xmlEleName.equals("oncreate") && textContent != null) {
							metadataSetModelProperty.setOncreate(new Boolean(textContent));
						}
						if (xmlEleName != null && xmlEleName.equals("onupdate") && textContent != null) {
							metadataSetModelProperty.setOnupdate(new Boolean(textContent));
						}
						if (xmlEleName != null && xmlEleName.equals("key_contenturl") && textContent != null) {
							metadataSetModelProperty.setKeyContenturl(textContent);
						}
						if (xmlEleName != null && xmlEleName.equals("multiple") && textContent != null) {
							metadataSetModelProperty.setMultiple(new Boolean(textContent));
						}
						if (xmlEleName != null && xmlEleName.equals("multilang") && textContent != null) {
							metadataSetModelProperty.setMultilang(new Boolean(textContent));
						}

						if (xmlEleName != null && xmlEleName.equals("assoctype")) {
							metadataSetModelProperty.setAssocType(textContent);
						}

					}
					metaDataSetPropList.add(metadataSetModelProperty);
				} else {
					logger.error("property name is missing");
				}
			}

			metadataSetModel.setProperties(metaDataSetPropList);

			NodeList childList = (NodeList) xpath.evaluate("children/child", typeNode, XPathConstants.NODESET);

			List<MetadataSetModelChild> metadataSetModelChildList = new ArrayList<MetadataSetModelChild>();

			// model type children
			for (int childIdx = 0; childIdx < childList.getLength(); childIdx++) {
				Node childNode = childList.item(childIdx);
				Node childNameAtt = childNode.getAttributes().getNamedItem("name");
				if (childNameAtt != null) {
					MetadataSetModelChild metadataSetModelChild = new MetadataSetModelChild();
					String childName = childNameAtt.getNodeValue();
					metadataSetModelChild.setName(childName);

					NodeList childPropNodeList = childNode.getChildNodes();
					for (int childPropIdx = 0; childPropIdx < childPropNodeList.getLength(); childPropIdx++) {
						Node childPropNode = childPropNodeList.item(childPropIdx);
						String xmlEleName = childPropNode.getNodeName();
						String textContent = new XMLTool().getTextContent(childPropNode);

						if (xmlEleName != null && xmlEleName.equals("childassoc") && textContent != null) {
							metadataSetModelChild.setChildassoc(textContent);
						}
					}
					metadataSetModelChildList.add(metadataSetModelChild);
				}
			}
			metadataSetModel.setChildren(metadataSetModelChildList);
		}

		return metadataModelList;
	}

	public List<MetadataSetQuery> getMetadataSetQueryList(Document doc, String repositoryId, String metadataSetId) throws Throwable {
		Node i18nNode = (Node) xpath.evaluate("/metadataset/i18n", doc, XPathConstants.NODE);
		String i18nRessource = new XMLTool().getTextContent(i18nNode);
		NodeList nodeList = (NodeList) xpath.evaluate("/metadataset/queries/query", doc, XPathConstants.NODESET);
		
		List<MetadataSetQuery> metadataSetQueryList = new ArrayList<MetadataSetQuery>();
		for (int i = 0; i < nodeList.getLength(); i++) {
			MetadataSetQuery mdsq = new MetadataSetQuery();
			metadataSetQueryList.add(mdsq);
			Node node = nodeList.item(i);
			Node joinAtt = node.getAttributes().getNamedItem("join");
			if (joinAtt != null) {
				mdsq.setJoin(joinAtt.getNodeValue());
			}
			
			Node criteriaboxidAtt = node.getAttributes().getNamedItem("criteriaboxid");
			if(criteriaboxidAtt != null){
				mdsq.setCriteriaboxid(criteriaboxidAtt.getNodeValue());
			}
			
			String qstatement =  (String)xpath.evaluate("statement", node, XPathConstants.STRING);
			if(qstatement != null && !qstatement.equals("")){
				mdsq.setStatement(qstatement);
			}
			
			String layout = (String) xpath.evaluate("layout", node, XPathConstants.STRING);
			if(layout != null && !layout.equals("")){
				mdsq.setLayout(layout);
			}
			
			String handlerclass = (String) xpath.evaluate("handlerclass", node, XPathConstants.STRING);
			if(handlerclass != null && !handlerclass.equals("")){
				mdsq.setHandlerclass(handlerclass);
			}
			String css = (String) xpath.evaluate("stylename", node, XPathConstants.STRING);
			if(css != null && !css.equals("")){
				mdsq.setStylename(css);
			}
			String widget = (String) xpath.evaluate("widget", node, XPathConstants.STRING);
			if(widget != null && !widget.equals("")){
				mdsq.setWidget(widget);
			}
			
			Node labelkeyAtt = node.getAttributes().getNamedItem("labelkey");
			if (labelkeyAtt != null) {
				
				MetadataSetValue label = new MetadataSetValue();
				label.setKey(labelkeyAtt.getNodeValue());
				label.setI18n(getI18nMap(i18nRessource, labelkeyAtt.getNodeValue()));
				mdsq.setLabel(label);
			}
			
			NodeList propList = (NodeList) xpath.evaluate("property", node, XPathConstants.NODESET);
			List<MetadataSetQueryProperty> properties = new ArrayList<MetadataSetQueryProperty>();
			for (int propIdx = 0; propIdx < propList.getLength(); propIdx++) {
				MetadataSetQueryProperty prop = new MetadataSetQueryProperty();
				properties.add(prop);
				prop.setParent(mdsq);
				
				Node propNode = propList.item(propIdx);
				Node propNameAtt = propNode.getAttributes().getNamedItem("name");
				String propName = propNameAtt.getNodeValue();
				prop.setName(propName);
				
				//put property in cache
				int propertyId = createPropertyId(repositoryId, metadataSetId, mdsq.getClass().getSimpleName(), i, propName, propIdx);
				prop.setId(new Integer(propertyId));
				MetadataCache.add(prop);
				
				NodeList propChildren = propNode.getChildNodes();
				if (propChildren != null) {

					String valuespace = null;
					String valuespace18n = null;
					String valuespace_i18n_prefix = null;
					String valuespace_key = null;
					
					String valuespace_provider = null;
					
					List<Validator> validators = null;

					for (int propChildIdx = 0; propChildIdx < propChildren.getLength(); propChildIdx++) {
						Node propChild = propChildren.item(propChildIdx);
						String xmlEleName = propChild.getNodeName();

						String textContent = new XMLTool().getTextContent(propChild);

						if (xmlEleName != null && xmlEleName.equals("labelkey") && textContent != null) {
							MetadataSetValue metadataSetValue = new MetadataSetValue();
							metadataSetValue.setKey(textContent);
							metadataSetValue.setI18n(getI18nMap(i18nRessource, textContent));
							prop.setLabel(metadataSetValue);
						}
						
						if (xmlEleName != null && xmlEleName.equals("placeholder") && textContent != null) {
							MetadataSetValue metadataSetValue = new MetadataSetValue();
							metadataSetValue.setKey(textContent);
							metadataSetValue.setI18n(getI18nMap(i18nRessource, textContent));
							prop.setPlaceHolder(metadataSetValue);
						}
						
						if (xmlEleName != null && xmlEleName.equals("widget") && textContent != null) {
							prop.setWidget(textContent);
						}
						
						if (xmlEleName != null && xmlEleName.equals("widget_title") && textContent != null) {
							MetadataSetValue widget_titleMdsv = new MetadataSetValue();
							widget_titleMdsv.setKey(textContent);
							widget_titleMdsv.setI18n(getI18nMap(i18nRessource, textContent));
							prop.setWidgetTitle(widget_titleMdsv);
						}

						// query specials
						if (xmlEleName != null && xmlEleName.equals("statement") && textContent != null) {
							prop.setStatement(textContent);
						}
						if (xmlEleName != null && xmlEleName.equals("multiple") && textContent != null) {
							prop.setMultiple(textContent);
						}
						if (xmlEleName != null && xmlEleName.equals("multiplejoin") && textContent != null) {
							prop.setMultiplejoin(textContent);
						}
						if (xmlEleName != null && xmlEleName.equals("formlength") && textContent != null) {
							prop.setFormlength(textContent);
						}
						if (xmlEleName != null && xmlEleName.equals("formheight") && textContent != null) {
							prop.setFormheight(textContent);
						}
						
						if (xmlEleName != null && xmlEleName.equals("stylename") && textContent != null) {
							prop.setStyleName(textContent);
						}
						
						if (xmlEleName != null && xmlEleName.equals("stylenamelabel") && textContent != null) {
							prop.setStyleNameLabel(textContent);
						}
						
						//sets the display none by default. toggling is done by the widget it is placed in.
						if (xmlEleName != null && xmlEleName.equals("toggle") && textContent != null) {
							prop.setToggle(textContent);
						}
						
						if (xmlEleName != null && xmlEleName.equals("escape") && textContent != null) {
							prop.setEscape(textContent);
						}
						
						if (xmlEleName != null && xmlEleName.equals("defaultvalues") && textContent != null) {
							prop.setDefaultValues(textContent.split(","));
						}
						
						if (xmlEleName != null && xmlEleName.equals("init_by_get_param") && textContent != null) {
							prop.setInit_by_get_param(textContent);
						}
						
						// valuespace
						if (xmlEleName != null && xmlEleName.equals("valuespace") && textContent != null) {
							valuespace = textContent;
						}
						if (xmlEleName != null && xmlEleName.equals("valuespace_i18n") && textContent != null) {
							valuespace18n = textContent;
						}
						if (xmlEleName != null && xmlEleName.equals("valuespace_i18n_prefix") && textContent != null) {
							valuespace_i18n_prefix = textContent;
						}
						if (xmlEleName != null && xmlEleName.equals("valuespace_key") && textContent != null) {
							valuespace_key = textContent;
						}
						
						if (xmlEleName != null && xmlEleName.equals("valuespace_provider") && textContent != null) {
							valuespace_provider = textContent;
						}
						
						// validators
						if (xmlEleName != null && xmlEleName.equals("validators")) {
							validators = getValidators(propChild);
						}

					}

					if (valuespace != null) {

						valuespace_key = (valuespace_key == null) ? propName : valuespace_key;
						valuespace18n = (valuespace18n == null) ? i18nRessource : valuespace18n;
						List<MetadataSetValueKatalog> valuespaceList = this.getValuespace(valuespace, valuespace18n,
								valuespace_i18n_prefix, valuespace_key);

						if (valuespaceList != null) {
							prop.setValuespace(valuespaceList);
						}
					}
					
					
					if(valuespace_provider != null){					
						prop.setValuespaceProvider(valuespace_provider);
					}
					
					if(validators != null && validators.size() > 0){
						prop.setValidators(validators);
					}

				}
				this.setPropertyParams(propNode, prop);
			}
			mdsq.setProperties(properties);
		}
		return metadataSetQueryList;
	}
	
	private void setPropertyParams(Node propertyNode, MetadataSetBaseProperty propertyObject) throws XPathExpressionException{
		NodeList paramList = (NodeList)xpath.evaluate("params/param", propertyNode, XPathConstants.NODESET);
		if(paramList != null){
			for(int i = 0; i < paramList.getLength();i++){
				Node param = paramList.item(i);
				String key = (String)xpath.evaluate("key", param, XPathConstants.STRING);
				String value = (String)xpath.evaluate("value", param, XPathConstants.STRING);
				propertyObject.setParam(key, value);
			}
		}
	}
	
	public int createPropertyId(String repositoryId, String metadataSetId, String parentEntity, int parentIdx, String propertyName, int propertyIdx){
		String concate = repositoryId + metadataSetId + parentEntity + parentIdx + propertyName + propertyIdx;
		return concate.hashCode();
	}

	public List<MetadataSetView> getMetadataSetViewList(Document doc, String repositoryId, String metadatasetId) throws Throwable {

		Node i18nNode = (Node) xpath.evaluate("/metadataset/i18n", doc, XPathConstants.NODE);
		String i18nRessource = new XMLTool().getTextContent(i18nNode);
		NodeList nodeList = (NodeList) xpath.evaluate("/metadataset/views/view", doc, XPathConstants.NODESET);
		List<MetadataSetView> metadataViewList = new ArrayList<MetadataSetView>();

		for (int i = 0; i < nodeList.getLength(); i++) {
			MetadataSetView mdsv = new MetadataSetView();
			metadataViewList.add(mdsv);
			Node node = nodeList.item(i);
			Node idAtt = node.getAttributes().getNamedItem("id");
			if (idAtt != null) {
				mdsv.setId(idAtt.getNodeValue());
			}

			NodeList viewList = (NodeList) xpath.evaluate("property", node, XPathConstants.NODESET);
			List<MetadataSetViewProperty> properties = new ArrayList<MetadataSetViewProperty>();
			for (int pIdx = 0; pIdx < viewList.getLength(); pIdx++) {

				MetadataSetViewProperty viewProp = new MetadataSetViewProperty();
				properties.add(viewProp);
				Node propNode = viewList.item(pIdx);
				Node propNameAtt = propNode.getAttributes().getNamedItem("name");

				String propName = propNameAtt.getNodeValue();
				viewProp.setName(propName);
				
				//put property in cache
				int propertyId = createPropertyId(repositoryId, metadatasetId, mdsv.getClass().getSimpleName(), i, propName, pIdx);
				viewProp.setId(new Integer(propertyId));
				MetadataCache.add(viewProp);

				NodeList propChildren = propNode.getChildNodes();
				if (propChildren != null) {

					String valuespace = null;
					String valuespace18n = null;
					String valuespace_i18n_prefix = null;
					String valuespace_key = null;

					for (int propChildIdx = 0; propChildIdx < propChildren.getLength(); propChildIdx++) {
						Node propChild = propChildren.item(propChildIdx);
						String xmlEleName = propChild.getNodeName();

						String textContent = new XMLTool().getTextContent(propChild);

						if (xmlEleName != null && xmlEleName.equals("labelkey") && textContent != null) {
							MetadataSetValue metadataSetValue = new MetadataSetValue();
							metadataSetValue.setKey(textContent);
							metadataSetValue.setI18n(getI18nMap(i18nRessource, textContent));
							viewProp.setLabel(metadataSetValue);
						}
						if (xmlEleName != null && xmlEleName.equals("widget") && textContent != null) {
							viewProp.setWidget(textContent);
						}
						if (xmlEleName != null && xmlEleName.equals("type") && textContent != null) {
							viewProp.setType(textContent);
						}
						
						if (xmlEleName != null && xmlEleName.equals("multiple") && textContent != null) {
							viewProp.setMultiple(new Boolean(textContent));
						}

						// valuespace
						if (xmlEleName != null && xmlEleName.equals("valuespace") && textContent != null) {
							valuespace = textContent;
						}
						if (xmlEleName != null && xmlEleName.equals("valuespace_i18n") && textContent != null) {
							valuespace18n = textContent;
						}
						if (xmlEleName != null && xmlEleName.equals("valuespace_i18n_prefix") && textContent != null) {
							valuespace_i18n_prefix = textContent;
						}
						if (xmlEleName != null && xmlEleName.equals("valuespace_key") && textContent != null) {
							valuespace_key = textContent;
						}

					}

					if (valuespace != null) {

						valuespace_key = (valuespace_key == null) ? propName : valuespace_key;
						valuespace18n = (valuespace18n == null) ? i18nRessource : valuespace18n;
						List<MetadataSetValueKatalog> valuespaceList = this.getValuespace(valuespace, valuespace18n,
								valuespace_i18n_prefix, valuespace_key);

						if (valuespaceList != null) {
							viewProp.setValuespace(valuespaceList);
						}
					}
				}
				
				this.setPropertyParams(propNode, viewProp);
			}

			mdsv.setProperties(properties);
		}
		return metadataViewList;
	}

	List<MetadataSetList> getMetadataSetListList(Document doc, String repositoryId, String metadatasetId) throws Throwable {

		Node i18nNode = (Node) xpath.evaluate("/metadataset/i18n", doc, XPathConstants.NODE);
		String i18nRessource = new XMLTool().getTextContent(i18nNode);
		NodeList nodeList = (NodeList) xpath.evaluate("/metadataset/lists/list", doc, XPathConstants.NODESET);
		List<MetadataSetList> metadataListList = new ArrayList<MetadataSetList>();

		for (int i = 0; i < nodeList.getLength(); i++) {
			MetadataSetList mdsv = new MetadataSetList();

			Node node = nodeList.item(i);
			Node idAtt = node.getAttributes().getNamedItem("id");
			if (idAtt != null) {
				mdsv.setId(idAtt.getNodeValue());
			}

			Node childAssocAtt = node.getAttributes().getNamedItem("childassoc");
			if (childAssocAtt != null) {
				mdsv.setChildAssoc(childAssocAtt.getNodeValue());
			}

			// label
			Node nodeLabelKey = (Node) xpath.evaluate("labelkey", node, XPathConstants.NODE);
			if (nodeLabelKey != null) {
				String labelKey = new XMLTool().getTextContent(nodeLabelKey);
				if (labelKey != null) {
					MetadataSetValue label = new MetadataSetValue();
					label.setKey(labelKey);
					label.setI18n(getI18nMap(i18nRessource, labelKey));
					mdsv.setLabel(label);
				}
			}

			// properties
			NodeList viewList = (NodeList) xpath.evaluate("property", node, XPathConstants.NODESET);

			List<MetadataSetListProperty> properties = new ArrayList<MetadataSetListProperty>();
			for (int pIdx = 0; pIdx < viewList.getLength(); pIdx++) {

				MetadataSetListProperty viewProp = new MetadataSetListProperty();
				properties.add(viewProp);
				Node propNode = viewList.item(pIdx);
				Node propNameAtt = propNode.getAttributes().getNamedItem("name");

				String propName = propNameAtt.getNodeValue();
				viewProp.setName(propName);
				
				
				//put property in cache
				int propertyId = createPropertyId(repositoryId, metadatasetId, mdsv.getClass().getSimpleName(), i, propName, pIdx);
				viewProp.setId(new Integer(propertyId));
				MetadataCache.add(viewProp);
				

				NodeList propChildren = propNode.getChildNodes();
				if (propChildren != null) {

					String valuespace = null;
					String valuespace18n = null;
					String valuespace_i18n_prefix = null;
					String valuespace_key = null;

					for (int propChildIdx = 0; propChildIdx < propChildren.getLength(); propChildIdx++) {
						Node propChild = propChildren.item(propChildIdx);
						String xmlEleName = propChild.getNodeName();

						String textContent = new XMLTool().getTextContent(propChild);

						if (xmlEleName != null && xmlEleName.equals("type") && textContent != null) {
							viewProp.setType(textContent);
						}
						
						if (xmlEleName != null && xmlEleName.equals("multiple") && textContent != null) {
							viewProp.setMultiple(new Boolean(textContent));
						}

						if (xmlEleName != null && xmlEleName.equals("labelkey") && textContent != null) {

							MetadataSetValue metadataSetValue = new MetadataSetValue();
							metadataSetValue.setKey(textContent);
							metadataSetValue.setI18n(getI18nMap(i18nRessource, textContent));
							viewProp.setLabel(metadataSetValue);
						}
						if (xmlEleName != null && xmlEleName.equals("widget") && textContent != null) {
							viewProp.setWidget(textContent);
						}

						// valuespace
						if (xmlEleName != null && xmlEleName.equals("valuespace") && textContent != null) {
							valuespace = textContent;
						}
						if (xmlEleName != null && xmlEleName.equals("valuespace_i18n") && textContent != null) {
							valuespace18n = textContent;
						}
						if (xmlEleName != null && xmlEleName.equals("valuespace_i18n_prefix") && textContent != null) {
							valuespace_i18n_prefix = textContent;
						}
						if (xmlEleName != null && xmlEleName.equals("valuespace_key") && textContent != null) {
							valuespace_key = textContent;
						}

					}

					if (valuespace != null) {

						valuespace_key = (valuespace_key == null) ? propName : valuespace_key;
						valuespace18n = (valuespace18n == null) ? i18nRessource : valuespace18n;
						List<MetadataSetValueKatalog> valuespaceList = this.getValuespace(valuespace, valuespace18n,
								valuespace_i18n_prefix, valuespace_key);

						if (valuespaceList != null) {
							viewProp.setValuespace(valuespaceList);
						}
					}
				}
				
				this.setPropertyParams(propNode, viewProp);
			}

			mdsv.setProperties(properties);
			metadataListList.add(mdsv);
		}
		return metadataListList;
	}
	
	public List<MetadataSetValueKatalog> getValuespace(String valuepaceFile, String valuespaceI18nBundle,
			String valuespace_i18n_prefix, String valuespace_key) throws Throwable {
		
		URL url = MetadataReader.class.getResource(valuepaceFile);
		
		SAXValueSpaceHandler handler =  new SAXValueSpaceHandler(url.openStream() , valuespaceI18nBundle, valuespace_i18n_prefix, valuespace_key);
	    return handler.getResult();
	}

	public List<MetadataSetValueKatalog> getValuespace_OLD(String valuepaceFile, String valuespaceI18nBundle,
			String valuespace_i18n_prefix, String valuespace_key) throws Throwable {
		List<MetadataSetValueKatalog> result = new ArrayList<MetadataSetValueKatalog>();

		System.out.println("called valuepaceFile:" + valuepaceFile + " valuespaceI18nBundle:" + valuespaceI18nBundle
				+ " valuespace_i18n_prefix:" + valuespace_i18n_prefix + " valuespace_key:" + valuespace_key);
		
		valuespace_key = (valuespace_key != null) ?  valuespace_key.trim() : null;
		valuepaceFile = (valuepaceFile != null) ?  valuepaceFile.trim() : null;
		valuespaceI18nBundle = (valuespaceI18nBundle != null) ?  valuespaceI18nBundle.trim() : null;
		valuespace_i18n_prefix = (valuespace_i18n_prefix != null) ?  valuespace_i18n_prefix.trim() : null;

		URL url = MetadataReader.class.getResource(valuepaceFile);
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse(url.openStream());
		NodeList valuespaceKeys = (NodeList) xpath.evaluate("/valuespaces/valuespace[@property='" + valuespace_key
				+ "']/key", doc, XPathConstants.NODESET);
		
		//for performance reasons
		boolean ignoreI18n = new Boolean((String)xpath.evaluate("/valuespaces/valuespace/@ignoreI18n", doc, XPathConstants.STRING));

		HashMap<MetadataSetValueKatalog, String> hasParentMap = new HashMap<MetadataSetValueKatalog, String>();
		for (int i = 0; i < valuespaceKeys.getLength(); i++) {
			Node nodeKey = valuespaceKeys.item(i);

			String key = new XMLTool().getTextContent(nodeKey);
			//not i18n cap
			String caption = (String)xpath.evaluate("@cap", nodeKey, XPathConstants.STRING);
			
			String statement = (String)xpath.evaluate("@statement", nodeKey, XPathConstants.STRING);
			
			// so that also empty values can be used
			if (key == null)
				key = "";
			if (key != null) {
				MetadataSetValueKatalog metadataSetValue = new MetadataSetValueKatalog();
				metadataSetValue.setKey(key);
				if(caption != null)metadataSetValue.setCaption(caption);
				
				//search statement
				if(statement != null)metadataSetValue.setStatement(statement);
				
				//for performance reasons
				if(!ignoreI18n){
					String i18nKey = (valuespace_i18n_prefix != null) ? i18nKey = valuespace_i18n_prefix + key : key;
					HashMap<String, String> i18n = getI18nMap(valuespaceI18nBundle, i18nKey);
					if (i18n.size() > 0) {
						metadataSetValue.setI18n(i18n);
					}
				}else{
					metadataSetValue.setI18n(new HashMap<String, String>());
				}

				// parent stuff
				Node attributeParent = nodeKey.getAttributes().getNamedItem("parent");
				if (attributeParent != null) {
					String parent = attributeParent.getNodeValue();
					if (parent != null && !parent.trim().equals("")) {
						hasParentMap.put(metadataSetValue, parent);
					}
				}

				result.add(metadataSetValue);
			}
		}

		// set parent katalog keys
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
		return result;

	}

	HashMap<String, String> getI18nMap(String ressourceBundle, String key) {
		
		if(key != null){
			// no spaces allowed in i18n
			key = key.replace(" ","_");
			key = key.replace("/","_");
			key = key.replace(":","_");
		}
		
		HashMap<String, String> i18n = new HashMap<String, String>();

		ArrayList<Locale> allowedLocale = new ArrayList<Locale>();
		for (String localeStr : CCConstants.allowedLocale) {
			if (localeStr.contains("_")) {
				String[] splitted = localeStr.split("_");
				allowedLocale.add(new Locale(splitted[0], splitted[1]));
			} else {
				allowedLocale.add(new Locale(localeStr));
			}
		}

		// for(Locale locale: Locale.getAvailableLocales()){
		try {
			//put the default locale
			ResourceBundle defaultResourceBundle =  ResourceBundle.getBundle(ressourceBundle, Locale.ROOT);
			if(defaultResourceBundle != null && defaultResourceBundle.containsKey(key)){
				i18n.put(CCConstants.defaultLocale, defaultResourceBundle.getString(key));
			}
			
			for (Locale locale : allowedLocale) {
				
					ResourceBundle resourceBundle = ResourceBundle.getBundle(ressourceBundle, locale);
					if (resourceBundle != null && locale.equals(resourceBundle.getLocale())) {
	
						if (resourceBundle.containsKey(key)) {
							String value = resourceBundle.getString(key);
							i18n.put(locale.toString(), value);
						}
					}
				
			}
		} catch (MissingResourceException e) {
			logger.error(e.getMessage());
		}
		
		return i18n;
	}
	
	public void initPostWebappLoad(MetadataSet mds){
		for(MetadataSetQuery query: mds.getMetadataSetQueries().getMetadataSetQueries()){
			initPostWebappLoad(query.getProperties());
		}
		
		mds.setLateInitDone(true);
	}
	
	private void initPostWebappLoad(List<? extends MetadataSetBaseProperty> props){
		for(MetadataSetBaseProperty prop : props){
			if(prop.getValuespaceProvider() != null && prop.getValuespace() == null){
				try{
					
					Class clazz = Class.forName(prop.getValuespaceProvider());
					ValuespaceProvider valuespaceProvider = (ValuespaceProvider)clazz.getConstructor(new Class[] { }).newInstance();
					prop.setValuespace(valuespaceProvider.getValuespace());
					
				}catch(ClassNotFoundException e){
					logger.error(e.getMessage(), e);
				}catch(Throwable e){
					logger.error(e.getMessage(), e);
				}
			}
		}
	}
}
