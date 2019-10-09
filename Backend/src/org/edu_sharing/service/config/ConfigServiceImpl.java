package org.edu_sharing.service.config;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.camel.util.FileUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.metadataset.v2.MetadataReaderV2;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.config.model.Config;
import org.edu_sharing.service.config.model.Context;
import org.edu_sharing.service.config.model.KeyValuePair;
import org.edu_sharing.service.config.model.Language;
import org.edu_sharing.service.config.model.Values;
import org.edu_sharing.service.config.model.Variables;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;



public class ConfigServiceImpl implements ConfigService{
	private static Logger logger=Logger.getLogger(ConfigServiceImpl.class);
	// Cached config
	private static Config currentConfig=null;
	private static final Unmarshaller jaxbUnmarshaller;

	static{
		Unmarshaller jaxbUnmarshaller1;
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(Config.class);
			jaxbUnmarshaller1 = jaxbContext.createUnmarshaller();
		} catch (JAXBException e) {
			jaxbUnmarshaller1 = null;
			logger.error(e.getMessage(),e);
		}
		jaxbUnmarshaller = jaxbUnmarshaller1;
	}

	/*
	XPathFactory pfactory = XPathFactory.newInstance();
	XPath xpath = pfactory.newXPath();
	private Document doc;
	private DocumentBuilder builder;
	private JSONObject json;
	*/
	
	public ConfigServiceImpl(){
		/*
		json = XML.toJSONObject(FileUtils.readFileToString(CONFIG_XML,"UTF-8"));
		builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		doc = builder.parse(CONFIG_XML);
		*/
	}
	/*
	public JSONObject getAsJson(String contextId) throws Exception {
		JSONObject copy = new JSONObject(json, JSONObject.getNames(json));
		copy.getJSONObject("config").remove("contexts");
		System.out.println(copy.toString());

		NodeList nodeMetadataSet = (NodeList) xpath.evaluate("/config/contexts/context", doc, XPathConstants.NODESET);
		for(int i=0;i<nodeMetadataSet.getLength();i++) {
			Node node=nodeMetadataSet.item(i);
			NodeList childs=node.getChildNodes();
			Node config=null;
			for(int j=0;j<childs.getLength();j++) {
				String name=childs.item(j).getNodeName();
				if(name.equals("id")){
					
				}
				if(name.equals("config")) {
					config=childs.item(j);
				}
			}
			if(config!=null) {
				overrideJson(copy,config);
			}
		}
		
		return copy;
	}
	*/
	@Override
	public Config getConfig() throws Exception {
	    if(!"true".equalsIgnoreCase(ApplicationInfoList.getHomeRepository().getDevmode()) && currentConfig!=null) {
	    	// Deep copy to prevent override cache data from contexts
			return SerializationUtils.clone(currentConfig);
		}
		InputStream is = getConfigInputStream();
		if(is==null)
			throw new IOException("client.config.xml file missing");
		Config config;
		synchronized (jaxbUnmarshaller) {
            config = (Config) jaxbUnmarshaller.unmarshal(is);
        }
        is.close();
		currentConfig = config;
        return config;

		
	}
	private InputStream getConfigInputStream() {
		ClassLoader classLoader = Thread.currentThread()
				.getContextClassLoader();
		return classLoader.getResourceAsStream("/org/edu_sharing/service/config/client.config.xml");	
	}
	@Override
	public Config getConfigByDomain(String domain) throws Exception {
		Config config=getConfig();
		if(config.contexts!=null && config.contexts.context!=null) {
			for(Context context : config.contexts.context) {
				if(context.domain==null)
					continue;
				for(String cd : context.domain) {
					if(cd.equals(domain)) {
						overrideValues(config.values,context.values);
						if(context.language!=null)
							config.language=overrideLanguage(config.language,context.language);
						if(context.variables!=null)
							config.variables=overrideVariables(config.variables,context.variables);
						return config;
					}
				}
			}
		}
		throw new IllegalArgumentException("Context with domain "+domain+" does not exists");

	}
	private Variables overrideVariables(Variables values, Variables override) {
		if(values==null)
			return override;
		if(override==null)
			return values;
		overrideList(values.variable,override.variable);	
		return values;
	}
	private void overrideList(List<KeyValuePair> list, List<KeyValuePair> override) {
		for(KeyValuePair obj : override) {
			if(list.contains(obj)) {
				list.remove(obj);
			}
			list.add(obj);
		}
	}
	private List<Language> overrideLanguage(List<Language> values, List<Language> override) {
		if(values==null)
			return override;
		if(override==null)
			return values;
		for(Language language : override) {
			for(Language language2 : values) {
				if(language.language.equals(language2.language)) {
					overrideList(language2.string,language.string);
				}
			}
		}
		return values;
	}
	private void overrideValues(Values values, Values override) throws IllegalArgumentException, IllegalAccessException {
		Class<?> c = override.getClass();
		Field[] fields = c.getDeclaredFields();
		for(Field field : fields) {
			if(field.get(override)!=null)
				field.set(values, field.get(override));
		}
		
	}

	@Override
	public void refresh() {
		currentConfig = null;
		try {
			getConfig();
		} catch (Exception e) {
			logger.error("error refreshing client config: "+e.getMessage(),e);
		}
	}


	/**
	 * override the default json with all the json from the xml
	 * @param copy
	 * @param config
	 * @throws TransformerException 
	 * @throws JSONException 
	 */
	/*
	private void overrideJson(JSONObject copy, Node config) throws Exception {
		JSONObject jsonOverride = XML.toJSONObject(nodeToString(config));
		String[] names=JSONObject.getNames(jsonOverride);
		for(String name : names) {
			System.out.println(name);
			copy.put(name, jsonOverride.get(name));
		}
	}
	
	// https://stackoverflow.com/questions/6534182/java-geting-all-content-of-a-xml-node-as-string
	private static String nodeToString(Node node) throws TransformerException {
		StringWriter sw = new StringWriter();
	    Transformer t = TransformerFactory.newInstance().newTransformer();
	    //t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
	    t.transform(new DOMSource(node), new StreamResult(sw));
		 
		return sw.toString();
	}
	*/
}
