package org.edu_sharing.repository.server.tools.mailtemplates;

import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.Mail;
import org.edu_sharing.repository.server.tools.URLTool;
import org.edu_sharing.service.config.ConfigServiceFactory;
import org.edu_sharing.service.mime.MimeTypesV2;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.servlet.ServletContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

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
	public static void sendMail(String receiver, String templateId, Map<String, String> replace) throws Exception {
		Mail mail = new Mail();
		ServletContext context = Context.getCurrentInstance().getRequest().getSession().getServletContext();
		String currentLocale = new AuthenticationToolAPI().getCurrentLocale();
		mail.sendMailHtml(
				context,
				receiver,
				MailTemplate.getSubject(templateId,currentLocale),
				MailTemplate.getContent(templateId,currentLocale, true),
				replace);
	}
	public static String generateContentLink(ApplicationInfo appInfo,String nodeId) throws Throwable{
		NodeService nodeService=NodeServiceFactory.getNodeService(appInfo.getAppId());
		String mime=MimeTypesV2.getMimeType(nodeService.getProperties(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), nodeId));
		if(MimeTypesV2.MIME_DIRECTORY.equals(mime)){
			if(nodeService.hasAspect(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), nodeId,CCConstants.CCM_ASPECT_COLLECTION)){
				return URLTool.getNgComponentsUrl()+"collections/?id="+nodeId;
			}
			return URLTool.getNgComponentsUrl()+"workspace/?id="+nodeId;
		}
		return 	URLTool.getNgComponentsUrl()+"render/"+nodeId+"?closeOnBack=true";
	}
	private static Map<TemplateDescription, Node> getTemplates(String locale) throws Exception {
		Document base=getXML(locale,false);
		NodeList templatesBase = (NodeList) xpath.evaluate("/templates/template", base, XPathConstants.NODESET);
		Map<TemplateDescription,Node> result = new HashMap<>();
		for(int i=0;i<templatesBase.getLength();i++){
			TemplateDescription desc = TemplateDescription.fromNode(templatesBase.item(i));
			if(desc!=null)
				result.put(desc,templatesBase.item(i));
		}
		try{
			Document override=getXML(locale,true);
			if(override!=null){
				NodeList templatesOverride = (NodeList) xpath.evaluate("/templates/template", override, XPathConstants.NODESET);

				for(int i=0;i<templatesOverride.getLength();i++) {
					TemplateDescription desc = TemplateDescription.fromNode(templatesOverride.item(i));
					if(desc!=null)
						result.put(desc,templatesOverride.item(i));
				}
			}
		}catch(Throwable t){
			logger.warn("Error overriding mail template for locale "+locale,t);
		}
		return result;

	}
	private static String getChildContent(String locale,String template, String name) throws Exception {
	    Map<TemplateDescription, Node> nodes = getTemplates(locale);
		String content=getChildContent(nodes,template,name);
		if(content!=null)
			return content;
		nodes = getTemplates(null);
		return getChildContent(nodes,template,name);
	}
	private static String getChildContent(Map<TemplateDescription, Node> nodes, String template, String name) throws XPathExpressionException {
		Node node = nodes.get(new TemplateDescription(template, ConfigServiceFactory.getCurrentContextId()));
		// no context specific template found, fallback to default
		if(node==null)
			node=nodes.get(new TemplateDescription(template,null));
		if(node==null){
			logger.info("No mail template found for id "+template);
			return null;
		}
		NodeList childs = node.getChildNodes();
		for(int j=0;j<childs.getLength();j++){
			if(childs.item(j).getNodeName().equals(name))
				return childs.item(j).getTextContent();
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
