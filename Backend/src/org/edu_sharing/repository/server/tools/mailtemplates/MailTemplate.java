package org.edu_sharing.repository.server.tools.mailtemplates;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.URLTool;
import org.edu_sharing.service.mime.MimeTypesV2;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class MailTemplate {
	static XPathFactory pfactory = XPathFactory.newInstance();
	static XPath xpath = pfactory.newXPath();
	static Logger logger = Logger.getLogger(MailTemplate.class);
	public static String getSubject(String template,String locale) throws Exception{
	    return getChildContent(locale,template,"subject");
	}
	
	public static String getContent(String template,String locale,boolean addFooter) throws Exception{
		String data="<style>";
		data += getChildContent(getTemplates(null), "stylesheet", "style");
		data += "</style>";
	    data += getChildContent(locale,"header","message") + 
	    		"<div class='content'>"+getChildContent(locale,template,"message") + "</div>" + 
	    		(addFooter ? ("<div class='footer'>"+getChildContent(locale,"footer","message")+"</div>") : "");
	    return data;
	}
	public static String generateContentLink(ApplicationInfo appInfo,String nodeId) throws Throwable{
		NodeService nodeService=NodeServiceFactory.getNodeService(appInfo.getAppId());
		String mime=MimeTypesV2.getMimeType(nodeService.getProperties(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), nodeId));
		if(mime.equals(MimeTypesV2.MIME_DIRECTORY)){
			return URLTool.getNgComponentsUrl()+"workspace/?id="+nodeId;
		}
		return 	URLTool.getNgComponentsUrl()+"render/"+nodeId+"?closeOnBack=true";
	}
	private static List<Node> getTemplates(String locale) throws Exception {
		Document base=getXML(locale,false);
		NodeList templatesBase = (NodeList) xpath.evaluate("/templates/template", base, XPathConstants.NODESET);
		List<Node> result = new ArrayList<>();
		for(int i=0;i<templatesBase.getLength();i++){
			result.add(templatesBase.item(i));
		}
		try{
			Document override=getXML(locale,true);
			if(override!=null){
				NodeList templatesOverride = (NodeList) xpath.evaluate("/templates/template", override, XPathConstants.NODESET);

				for(int i=0;i<templatesOverride.getLength();i++) {
					NamedNodeMap attributes = templatesOverride.item(i).getAttributes();
					String search = null;
					if (attributes != null) {
						search = attributes.getNamedItem("name").getTextContent();
					} else {
						continue;
					}
					boolean replaced = false;
					for (int j = 0; j < result.size(); j++) {
						NamedNodeMap attributes2 = result.get(j).getAttributes();
						if (attributes2 != null && attributes2.getNamedItem("name").getTextContent().equals(search)) {
							result.remove(j);
							result.add(j, templatesOverride.item(i));
							replaced = true;
							break;
						}
					}
					if (!replaced)
						throw new IllegalArgumentException("Error while override: The name " + search + " is not known by the main template and can not be override");
				}
			}
		}catch(Throwable t){
			logger.warn("Error overriding mail template for locale "+locale,t);
		}
		return result;

	}
	private static String getChildContent(String locale,String template, String name) throws Exception {
	    List<Node> nodes = getTemplates(locale);
		String content=getChildContent(nodes,template,name);
		if(content!=null)
			return content;
		nodes = getTemplates(null);
		return getChildContent(nodes,template,name);
	}
	private static String getChildContent(List<Node> nodes, String template, String name) throws XPathExpressionException {
		for(Node node : nodes){
			NamedNodeMap attributes = node.getAttributes();
			if(attributes!=null && attributes.getNamedItem("name").getTextContent().equals(template)){
				NodeList childs = node.getChildNodes();
				for(int j=0;j<childs.getLength();j++){
			    	if(childs.item(j).getNodeName().equals(name))
			    		return childs.item(j).getTextContent();
			    }
			}
		}
	    return null;
	}

	private static Document getXML(String locale,boolean override) throws SAXException, IOException, ParserConfigurationException {
		InputStream in = null;
		String overridePostfix="";
		if(override)
			overridePostfix="_override";
		try{
			in = MailTemplate.class.getResourceAsStream("/org/edu_sharing/repository/server/tools/mailtemplates/templates_"+locale + overridePostfix + ".xml");
			if(in==null)
				throw new Exception();
		}
		catch(Throwable t){
			in = MailTemplate.class.getResourceAsStream("/org/edu_sharing/repository/server/tools/mailtemplates/templates"+overridePostfix+".xml");
		}
		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		if(in==null)
			return null;
		return builder.parse(in);
	}
}
