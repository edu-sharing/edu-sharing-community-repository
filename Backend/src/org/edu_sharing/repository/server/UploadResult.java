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
package org.edu_sharing.repository.server;

import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.edu_sharing.repository.client.tools.forms.CrudResult;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class UploadResult {
	
	public String getResultString(String nodeId, String statusType, String statusMessage){
		//String resultString ="<?xml version="1.0" encoding="UTF-8"?>"
		
		try{
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			DOMImplementation impl = builder.getDOMImplementation();
			
			Document xmldoc = impl.createDocument(null,CrudResult.ELE_ROOT, null);
			
			Element elementNodeId = xmldoc.createElement(CrudResult.ELE_NODEID);
			
			if(nodeId != null){
				elementNodeId.setTextContent(nodeId);
			}
			
			xmldoc.getDocumentElement().appendChild(elementNodeId);
			
			
			Element eleStatus = xmldoc.createElement(CrudResult.ELE_STATUS);
			eleStatus.setTextContent(statusType);
			xmldoc.getDocumentElement().appendChild(eleStatus);
			
			
			
			Element eleStatusMsg = xmldoc.createElement(CrudResult.ELE_STATUS_MESSAGE);
			eleStatusMsg.setTextContent(statusMessage);
			xmldoc.getDocumentElement().appendChild(eleStatusMsg);
			
			
			DOMSource domSource = new DOMSource(xmldoc);
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer serializer = tf.newTransformer();
			serializer.setOutputProperty(OutputKeys.ENCODING,"UTF-8");
			//initialize StreamResult with File object to save to file
			StreamResult result = new StreamResult(new StringWriter());
			serializer.transform(domSource, result);
	
			return result.getWriter().toString();
		}catch(Exception e){
			e.printStackTrace();
			return null;
		}

	}
	
}
