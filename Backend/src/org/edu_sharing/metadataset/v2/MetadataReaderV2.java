package org.edu_sharing.metadataset.v2;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class MetadataReaderV2 {
	
	public static final String SUGGESTION_SOURCE_SOLR = "Solr";
	public static final String SUGGESTION_SOURCE_MDS = "Mds";
	public static final String SUGGESTION_SOURCE_SQL = "Sql";
	private static Logger logger = Logger.getLogger(MCAlfrescoAPIClient.class);
	private static Map<String,MetadataSetV2> mdsCache=new HashMap<>();
	XPathFactory pfactory = XPathFactory.newInstance();
	XPath xpath = pfactory.newXPath();
	private Document doc;
	private DocumentBuilder builder;
	private String i18nPath;
	private String locale;
	
	public static String getPath(){
		return "/org/edu_sharing/metadataset/v2/";
	}
	
	public static MetadataSetV2 getMetadataset(ApplicationInfo appId,String mdsSet) throws Exception{
		String locale="default";
		try{
			locale = new AuthenticationToolAPI().getCurrentLocale();
		}catch(Throwable t){}
		return getMetadataset(appId, mdsSet,locale);		
	}
	
	public static MetadataSetV2 getMetadataset(ApplicationInfo appId,String mdsSet,String locale) throws Exception{
		MetadataReaderV2 reader;
		MetadataSetV2 mds;
		String mdsNameDefault="mds";
		if(appId.getMetadatsetsV2()!=null){
			mdsNameDefault=appId.getMetadatsetsV2()[0];
			if(mdsNameDefault.toLowerCase().endsWith(".xml"))
				mdsNameDefault=mdsNameDefault.substring(0,mdsNameDefault.length()-4);
		}
		String mdsName=mdsNameDefault;
		if(!mdsSet.equals("-default-") && !mdsSet.equals(CCConstants.metadatasetdefault_id)){
			if(appId.getMetadatsetsV2()!=null && Arrays.asList(appId.getMetadatsetsV2()).contains(mdsSet)){
				mdsName=mdsSet;
				if(mdsName.toLowerCase().endsWith(".xml"))
					mdsName=mdsName.substring(0,mdsName.length()-4);
			}
			else{
				throw new IllegalArgumentException("Invalid mds set "+mdsSet+", was not found in the list of mds sets of appid "+appId.getAppId());
			}
		}
		String id=appId.getAppId()+mdsName+"_"+locale;
		if(mdsCache.containsKey(id))
			return mdsCache.get(id);
		
		reader=new MetadataReaderV2(mdsNameDefault+".xml",locale);
		mds=reader.getMetadatasetForFile(mdsNameDefault);
		mds.setRepositoryId(appId.getAppId());
		if(mds.getInherit()!=null && !mds.getInherit().isEmpty()) {
			String inheritName=mds.getInherit()+".xml";
			reader=new MetadataReaderV2(inheritName,locale);
			MetadataSetV2 mdsInherit = reader.getMetadatasetForFile(inheritName);
			try{
				reader=new MetadataReaderV2(mds.getInherit()+"_override.xml",locale);
				MetadataSetV2 mdsOverride = reader.getMetadatasetForFile(inheritName);
				mdsInherit.overrideWith(mdsOverride);
			}catch(IOException e){
			}
			mdsInherit.overrideWith(mds);
			mds=mdsInherit;
		}
		if(!mdsName.equals(mdsNameDefault)){
			reader=new MetadataReaderV2(mdsName+".xml",locale);
			MetadataSetV2 mdsOverride = reader.getMetadatasetForFile(mdsName);
			mds.overrideWith(mdsOverride);
		}
		try{
			reader=new MetadataReaderV2(mdsName+"_override.xml",locale);
			MetadataSetV2 mdsOverride = reader.getMetadatasetForFile(mdsName);
			mds.overrideWith(mdsOverride);
		}
		catch(IOException e){
		}
		mdsCache.put(id, mds);
		return mds;
	}
	
	private MetadataQueries getQueries() throws Exception {
		MetadataQueries result=new MetadataQueries();
		Node queryNode = (Node) xpath.evaluate("/metadataset/queries", doc, XPathConstants.NODE);
		if(queryNode==null)
			return null;
		NodeList list=queryNode.getChildNodes();
		for(int i=0;i<list.getLength();i++){
			Node data=list.item(i);
			String name=data.getNodeName();
			String value=data.getTextContent();
			if(name.equals("basequery"))
				result.setBasequery(value);
			if(name.equals("allowSearchWithoutCriteria"))
				result.setAllowSearchWithoutCriteria(value.equalsIgnoreCase("true"));
		}
		NodeList queriesNode = (NodeList) xpath.evaluate("/metadataset/queries/query", doc, XPathConstants.NODESET);
		List<MetadataQuery> queries=new ArrayList<>();
		for(int i=0;i<queriesNode.getLength();i++){
			MetadataQuery query=new MetadataQuery(result);
			Node node=queriesNode.item(i);
			NamedNodeMap nodeMap = node.getAttributes();
			query.setId(nodeMap.getNamedItem("id").getTextContent());
			if(nodeMap.getNamedItem("join")!=null)
				query.setJoin(nodeMap.getNamedItem("join").getTextContent());
			else
				query.setJoin("AND");
			
			if(nodeMap.getNamedItem("applyBasequery")!=null)
				query.setApplyBasequery(nodeMap.getNamedItem("applyBasequery").getTextContent().equals("true"));
			
			List<MetadataQueryParameter> parameters=new ArrayList<>();

			NodeList list2=node.getChildNodes();
			
			for(int j=0;j<list2.getLength();j++){
				Node parameterNode=list2.item(j);
				if(parameterNode.getNodeName().equals("basequery")){
					query.setBasequery(parameterNode.getTextContent());
				}
				MetadataQueryParameter parameter=new MetadataQueryParameter();
				NodeList list3=parameterNode.getChildNodes();
				NamedNodeMap attributes = parameterNode.getAttributes();
				if(attributes==null || attributes.getNamedItem("name")==null)
					continue;
				parameter.setName(attributes.getNamedItem("name").getTextContent());
				Map<String, String> statements = new HashMap<String,String>();
				for(int k=0;k<list3.getLength();k++){
					Node data=list3.item(k);
					String name=data.getNodeName();
					String value=data.getTextContent();
					if(name.equals("statement")) {
						Node key=data.getAttributes().getNamedItem("value");
						statements.put(key==null ? null : key.getTextContent(), value);
					}
					if(name.equals("ignorable"))
						parameter.setIgnorable(Integer.parseInt(value));
					if(name.equals("exactMatching"))
						parameter.setExactMatching(value.equalsIgnoreCase("true"));
					if(name.equals("multiple"))
						parameter.setMultiple(value.equalsIgnoreCase("true"));
					if(name.equals("multiplejoin"))
						parameter.setMultiplejoin(value);
				}
				parameter.setStatements(statements);
				parameters.add(parameter);
			}
			query.setParameters(parameters);
			queries.add(query);
		}
		result.setQueries(queries);
		return result;
	}
	
	private static InputStream getFile(String name,Filetype type) throws IOException{
		String prefix=getPath()+"xml/";
		if(type.equals(Filetype.VALUESPACE))
			prefix+="valuespaces/";
		InputStream is=MetadataReaderV2.class.getResourceAsStream(prefix+name);
		if(is==null)
			throw new IOException("file "+name+" not found");
		return is;
	}
	
	private MetadataReaderV2(String name, String locale) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setIgnoringComments(true);
		builder = factory.newDocumentBuilder();
		InputStream is = getFile(name,Filetype.MDS);
		doc = builder.parse(is);
		is.close();
		this.locale = locale;
	}
	
	private MetadataSetV2 getMetadatasetForFile(String filename) throws Exception{
		MetadataSetV2 mds=new MetadataSetV2();
		Node nodeMetadataSet = (Node) xpath.evaluate("/metadataset", doc, XPathConstants.NODE);
		String id;
		try{
			id = nodeMetadataSet.getAttributes().getNamedItem("id").getNodeValue();
		}catch(NullPointerException e){
			//throw new Exception("Mandatory attribute id is missing for the metadataset "+filename+", add this attribute to the main node");
			id=filename;
		}
		Node name = (Node) xpath.evaluate("/metadataset/name", doc, XPathConstants.NODE);
		String mdsName="";
		if(name!=null)
			mdsName=name.getTextContent();
		Node inherit = (Node) xpath.evaluate("/metadataset/inherit", doc, XPathConstants.NODE);
		String mdsInherit=null;
		if(inherit!=null)
			mdsInherit=inherit.getTextContent();
		Node i18n = (Node) xpath.evaluate("/metadataset/i18n", doc, XPathConstants.NODE);
		
		i18nPath=i18n.getTextContent();
		String label;
		try{
			label = nodeMetadataSet.getAttributes().getNamedItem("label").getNodeValue();

		}catch(NullPointerException e){
			throw new Exception("Mandatory attribute label is missing for the metadataset "+filename+", add this attribute to the main node");
		}

		String hidden = (String)xpath.evaluate("/metadataset/@hidden", doc, XPathConstants.STRING);
		if(hidden == null) mds.setHidden(false); 
		else mds.setHidden(new Boolean(hidden));
		
		mds.setId(id);
		mds.setName(mdsName);
		mds.setInherit(mdsInherit);
		mds.setI18n(i18n.getTextContent());
		mds.setLabel(label);
		
		mds.setWidgets(getWidgets());
		mds.setTemplates(getTemplates());
		mds.setGroups(getGroups());
		mds.setLists(getLists());
		mds.setQueries(getQueries());
		
		return mds;
	}
	
	private List<MetadataWidget> getWidgets() throws Exception {
		List<MetadataWidget> widgets=new ArrayList<>();
		NodeList widgetsNode = (NodeList) xpath.evaluate("/metadataset/widgets/widget", doc, XPathConstants.NODESET);
		for(int i=0;i<widgetsNode.getLength();i++){
			Node widgetNode=widgetsNode.item(i);
			NodeList list2=widgetNode.getChildNodes();
			MetadataWidget widget=new MetadataWidget();
			widget.setI18n(i18nPath);
			String valuespaceI18n=i18nPath;
			String valuespaceI18nPrefix="";
			for(int j=0;j<list2.getLength();j++){
				Node data=list2.item(j);
				String name=data.getNodeName();
				String value=data.getTextContent();
				if(name.equals("id"))
					widget.setId(value);			
				if(name.equals("icon"))
					widget.setIcon(value);			
				if(name.equals("template"))
					widget.setTemplate(value);			
				if(name.equals("caption")){
					//widget.setCaption(value);
					widget.setCaption(getTranslation(widget,value));
				}
				if(name.equals("placeholder")){
					//widget.setPlaceholder(value);
					widget.setPlaceholder(getTranslation(widget,value));
				}
				if(name.equals("bottomCaption")){
					widget.setBottomCaption(getTranslation(widget,value));
				}
				if(name.equals("unit")){
					widget.setUnit(getTranslation(widget,value));
				}
				if(name.equals("defaultvalue"))
					widget.setDefaultvalue(value); 
				if(name.equals("format"))
					widget.setFormat(value); 
				if(name.equals("type"))
					widget.setType(value);
				if(name.equals("condition"))
					widget.setCondition(value);
				if(name.equals("suggestionSource"))
					widget.setSuggestionSource(value);
				if(name.equals("suggestionQuery"))
					widget.setSuggestionQuery(value);
				if(name.equals("required"))
					widget.setRequired(value.equalsIgnoreCase("true"));
				if(name.equals("hideIfEmpty"))
					widget.setHideIfEmpty(value.equalsIgnoreCase("true"));
				if(name.equals("valuespace_i18n")){
					valuespaceI18n=value;
				}
				if(name.equals("valuespace_i18n_prefix")){
					valuespaceI18nPrefix=value;
				}
				if(name.equals("valuespaceClient")){
					widget.setValuespaceClient(value.equalsIgnoreCase("true"));				
				}
				if(name.equals("extended"))
					widget.setExtended(value.equalsIgnoreCase("true"));				
				if(name.equals("min"))
					widget.setMin(Integer.parseInt(value));				
				if(name.equals("max"))
					widget.setMax(Integer.parseInt(value));				
				if(name.equals("default"))
					widget.setDefaultValue(Integer.parseInt(value));				
				if(name.equals("defaultMin"))
					widget.setDefaultMin(Integer.parseInt(value));				
				if(name.equals("defaultMax"))
					widget.setDefaultMax(Integer.parseInt(value));				
				if(name.equals("step"))
					widget.setStep(Integer.parseInt(value));			
				if(name.equals("allowempty"))
					widget.setAllowempty(value.equalsIgnoreCase("true"));				
			}
			for(int j=0;j<list2.getLength();j++){
				Node data=list2.item(j);
				String name=data.getNodeName();
				String value=data.getTextContent();
				if(name.equals("valuespace"))
					widget.setValues(getValuespace(value,widget.getId(),valuespaceI18n,valuespaceI18nPrefix));
				if(name.equals("values"))
					widget.setValues(getValues(data.getChildNodes(),valuespaceI18n,valuespaceI18nPrefix));
			}
			widgets.add(widget);
		}
		return widgets;
	}
	
	private List<MetadataKey> getValuespace(String value,String id, String valuespaceI18n, String valuespaceI18nPrefix) throws Exception {
		Document docValuespace = builder.parse(getFile(value,Filetype.VALUESPACE));
		List<MetadataKey> keys=new ArrayList<>();
		NodeList keysNode=(NodeList)xpath.evaluate("/valuespaces/valuespace[@property='"+id+"']/key",docValuespace, XPathConstants.NODESET);
		if(keysNode.getLength()==0){
			throw new Exception("No valuespace found in file "+value+": Searching for a node named /valuespaces/valuespace[@property='"+id+"']");
		}
		return getValues(keysNode,valuespaceI18n,valuespaceI18nPrefix);
	}
	
	private List<MetadataKey> getValues(NodeList keysNode, String valuespaceI18n, String valuespaceI18nPrefix) throws IOException {
		List<MetadataKey> keys=new ArrayList<>();
		for(int i=0;i<keysNode.getLength();i++){
			Node keyNode=keysNode.item(i);
			NamedNodeMap attributes=keyNode.getAttributes();
			String cap=null;
			String description=null;
			if(attributes!=null && attributes.getNamedItem("cap")!=null)
				cap=attributes.getNamedItem("cap").getTextContent();
			if(attributes!=null && attributes.getNamedItem("description")!=null)
				description=attributes.getNamedItem("description").getTextContent();
			if(cap==null) cap="";
			if(description==null) description="";
			if(keyNode.getTextContent().trim().isEmpty() && (cap.trim().isEmpty())) continue;
			MetadataKey key=new MetadataKey();
			key.setKey(keyNode.getTextContent());
			key.setI18n(valuespaceI18n);
			key.setI18nPrefix(valuespaceI18nPrefix);
			if(attributes!=null && attributes.getNamedItem("parent")!=null)
					key.setParent(attributes.getNamedItem("parent").getTextContent());
			String fallback=null;
			if(!cap.isEmpty()) fallback=cap;
			key.setCaption(getTranslation(key,key.getKey(),fallback));
			key.setDescription(getTranslation(key,description));
			keys.add(key);
		}
		return keys;
	}
	
	private List<MetadataTemplate> getTemplates() throws XPathExpressionException, IOException {
		List<MetadataTemplate> templates=new ArrayList<>();
		NodeList templatesNode = (NodeList) xpath.evaluate("/metadataset/templates/template", doc, XPathConstants.NODESET);
		for(int i=0;i<templatesNode.getLength();i++){
			Node templateNode=templatesNode.item(i);
			NodeList list2=templateNode.getChildNodes();
			MetadataTemplate template=new MetadataTemplate();
			template.setI18n(i18nPath);
			for(int j=0;j<list2.getLength();j++){
				Node data=list2.item(j);
				String name=data.getNodeName();
				String value=data.getTextContent();
				if(name.equals("id")){
					template.setId(value);
				}
				if(name.equals("caption")){
					//template.setCaption(value);
					template.setCaption(getTranslation(template,value));
				}
				if(name.equals("rel"))
					template.setRel(value);
				if(name.equals("icon"))
					template.setIcon(value);
				if(name.equals("html"))
					template.setHtml(translateHtml(i18nPath,value));				
			}
			templates.add(template);

		}
		return templates;
	}
	
	private String translateHtml(String i18nPath, String html) {
		String[] parts=StringUtils.splitByWholeSeparator(html,"{{");
		for(int i=1;i<parts.length;i++){
			String[] key=StringUtils.splitByWholeSeparator(parts[i],"}}");
			String i18nKey=key[0].trim();
			key[0]=getTranslation(i18nPath, i18nKey,null,locale);
			parts[i]=StringUtils.join(key,"");
		}
		return StringUtils.join(parts, "");
	}
	
	private List<MetadataGroup> getGroups() throws XPathExpressionException {
		List<MetadataGroup> groups=new ArrayList<>();
		NodeList groupsNode = (NodeList) xpath.evaluate("/metadataset/groups/group", doc, XPathConstants.NODESET);
		for(int i=0;i<groupsNode.getLength();i++){
			Node groupNode=groupsNode.item(i);
			NodeList list2=groupNode.getChildNodes();
			MetadataGroup group=new MetadataGroup();
			for(int j=0;j<list2.getLength();j++){
				Node data=list2.item(j);
				String name=data.getNodeName();
				String value=data.getTextContent();
				if(name.equals("id"))
					group.setId(value);			
				if(name.equals("views")){
					List<String> views=new ArrayList<>();
					NodeList list3=data.getChildNodes();
					for(int k=0;k<list3.getLength();k++){
						String view=list3.item(k).getTextContent();
						if(!view.trim().isEmpty())
							views.add(view);
					}
					group.setViews(views);
				}
			}
			groups.add(group);
		}
		return groups;
	}
	
	private List<MetadataList> getLists() throws XPathExpressionException {
		List<MetadataList> lists=new ArrayList<>();
		NodeList listsNode = (NodeList) xpath.evaluate("/metadataset/lists/list", doc, XPathConstants.NODESET);
		for(int i=0;i<listsNode.getLength();i++){
			Node listNode=listsNode.item(i);
			NodeList list2=listNode.getChildNodes();
			MetadataList list=new MetadataList();
			for(int j=0;j<list2.getLength();j++){
				Node data=list2.item(j);
				String name=data.getNodeName();
				String value=data.getTextContent();
				if(name.equals("id"))
					list.setId(value);			
				if(name.equals("columns")){
					List<MetadataColumn> columns=new ArrayList<>();
					NodeList list3=data.getChildNodes();
					for(int k=0;k<list3.getLength();k++){
						String column=list3.item(k).getTextContent();
						NamedNodeMap attributes = list3.item(k).getAttributes();
						if(!column.trim().isEmpty()){
							MetadataColumn col=new MetadataColumn();
							col.setId(column);
							if(attributes!=null){
								Node showDefault = attributes.getNamedItem("showDefault");
								if(showDefault!=null)
									col.setShowDefault(showDefault.getTextContent().equals("true"));
								Node format = attributes.getNamedItem("format");
								if(format!=null)
									col.setFormat(format.getTextContent());
							}
							columns.add(col);
						}
					}
					list.setColumns(columns);
				}
			}
			lists.add(list);
		}
		return lists;
	}
	
	private String getTranslation(MetadataTranslatable translatable,String key,String fallback){
		return getTranslation(translatable,key,fallback,locale);
	}
	private String getTranslation(MetadataTranslatable translatable,String key){
		return getTranslation(translatable,key,null);
	}
	private static String getTranslation(MetadataTranslatable translatable,String key,String fallback,String locale){
		try{
			if(key==null)
				return null;
			if(translatable.getI18nPrefix()!=null)
				key=translatable.getI18nPrefix()+key;
			return getTranslation(translatable.getI18n(),key,fallback,locale);
		}catch(Exception e){
			logger.warn(e.toString());
			return key;
		}
	}
	
	private static String getTranslation(String i18n,String key,String fallback,String locale){
		
		String defaultValue=key;
		if(fallback!=null)
			defaultValue=fallback;
		
		InputStream isDefaultOverride=MetadataReaderV2.class.getResourceAsStream(getPath()+"i18n/mds_override.properties");
		InputStream isLocaleOverride=MetadataReaderV2.class.getResourceAsStream(getPath()+"i18n/mds_override_"+locale+".properties");
		InputStream isDefault=MetadataReaderV2.class.getResourceAsStream(getPath()+"i18n/"+i18n+".properties");
		InputStream isLocale=MetadataReaderV2.class.getResourceAsStream(getPath()+"i18n/"+i18n+"_"+locale+".properties");
		
		if(key!=null)
			key=key.replace(" ","_");
		
		try{
			ResourceBundle defaultResourceBundle =  new PropertyResourceBundle(isDefault);
			if(defaultResourceBundle.containsKey(key))
				defaultValue=defaultResourceBundle.getString(key);
			if(isDefaultOverride!=null){
				ResourceBundle resourceBundle =  new PropertyResourceBundle(isDefaultOverride);
				if(resourceBundle!=null && resourceBundle.containsKey(key))
					defaultValue=resourceBundle.getString(key);
			}
			
		}catch(Throwable t){
			logger.warn("No translation file "+i18n+" found while looking for "+key);
		}
		try{
			if(isLocale!=null){
				if(isLocaleOverride!=null){
					ResourceBundle resourceBundle =  new PropertyResourceBundle(isLocaleOverride);
					if(resourceBundle!=null && resourceBundle.containsKey(key))
						return resourceBundle.getString(key);
				}
				ResourceBundle resourceBundle =  new PropertyResourceBundle(isLocale);
				if(resourceBundle!=null && resourceBundle.containsKey(key))
					return resourceBundle.getString(key);
			}
		}catch(IOException e){
			e.printStackTrace();
		}
		return defaultValue;

	}
	
	public static void refresh() {
		mdsCache.clear();
		prepareMetadatasets();
	}
	
	public static void prepareMetadatasets() {
		ApplicationInfo home = ApplicationInfoList.getHomeRepository();
		try{
			getMetadataset(home, "-default-","de_DE");
			getMetadataset(home, "-default-","en_US");
		}catch(Throwable t){}
	}
	
}
