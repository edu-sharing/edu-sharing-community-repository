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
package org.edu_sharing.repository.server.tools;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XMLTool {

	public String getTextContent(Node node) {
		String result = null;
		NodeList nodeList = node.getChildNodes();
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node child = nodeList.item(i);

			if (child.getNodeType() == Node.TEXT_NODE) {
				result = child.getTextContent();
			}
		}
		return result;
	}
}
