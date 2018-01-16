package org.edu_sharing.repository.server.tools.mailtemplates;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.alfresco.service.cmr.repository.StoreRef;
import org.edu_sharing.metadataset.v2.Filetype;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.URLTool;
import org.edu_sharing.service.mime.MimeTypesV2;
import org.edu_sharing.service.model.NodeRef;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class MailTemplate {
	static XPathFactory pfactory = XPathFactory.newInstance();
	static XPath xpath = pfactory.newXPath();
	public static String getSubject(String template,String locale) throws Exception{
	    return getChildContent(locale,template,"subject");
	}
	
	public static String getContent(String template,String locale,boolean addFooter) throws Exception{
		String data="<style>";
		data += getChildContent(getXML(null), "stylesheet", "style");
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
	private static String getChildContent(String locale,String template, String name) throws XPathExpressionException, SAXException, IOException, ParserConfigurationException {
	    Document document = getXML(locale);
		String content=getChildContent(document,template,name);
		if(content!=null)
			return content;
		document = getXML(null);
		return getChildContent(document,template,name);
	}
	private static String getChildContent(Document document, String template, String name) throws XPathExpressionException {
		NodeList templates = (NodeList) xpath.evaluate("/templates/template", document, XPathConstants.NODESET);
		for(int i=0;i<templates.getLength();i++){
			NamedNodeMap attributes = templates.item(i).getAttributes();
			if(attributes!=null && attributes.getNamedItem("name").getTextContent().equals(template)){
				NodeList childs = templates.item(i).getChildNodes();
				for(int j=0;j<childs.getLength();j++){
			    	if(childs.item(j).getNodeName().equals(name))
			    		return childs.item(j).getTextContent();
			    }
			}
		}
	    return null;
	}

	private static Document getXML(String locale) throws SAXException, IOException, ParserConfigurationException {
		InputStream in = null;
		try{
			in = MailTemplate.class.getResourceAsStream("/org/edu_sharing/repository/server/tools/mailtemplates/templates_"+locale + ".xml");
			if(in==null)
				throw new Exception();
		}
		catch(Throwable t){
			in = MailTemplate.class.getResourceAsStream("/org/edu_sharing/repository/server/tools/mailtemplates/templates.xml");
		}
		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		return builder.parse(in);
	}
}
