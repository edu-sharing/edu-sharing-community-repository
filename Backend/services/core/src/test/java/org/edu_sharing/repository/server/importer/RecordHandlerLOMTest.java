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
package org.edu_sharing.repository.server.importer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class RecordHandlerLOMTest implements RecordHandlerInterface {

	Log logger = LogFactory.getLog(RecordHandlerLOMTest.class);
	XPathFactory pfactory = XPathFactory.newInstance();
	XPath xpath = pfactory.newXPath();

	private PersistentHandlerInterface persistentHandler = new PersistentHandlerDB();
	
	ArrayList<String> keys = new ArrayList<String>();
	
	ArrayList<HashMap<String,String>> allDS = new ArrayList<HashMap<String,String>>();

	@Override
	public void handleRecord(Node nodeRecord, String cursor, String set) throws Throwable {
		// TODO Auto-generated method stub

		HashMap<String, String> toSafe = new HashMap<String, String>();
		
		Node node = (Node) xpath.evaluate("metadata/lom", nodeRecord, XPathConstants.NODE);
		this.handleNode(node, "", toSafe);
		
		
		
		
		for(Map.Entry<String,String> entry:toSafe.entrySet()){
			//System.out.println(entry.getKey()+"   "+entry.getValue());
			if(!keys.contains(entry.getKey())){
				keys.add(entry.getKey());
				System.out.println("ADDING KEY:"+entry.getKey()+" VALUE:"+entry.getValue() +" SIZE:"+allDS.size() +" set:"+set+" cursor:"+cursor);
			}
		}
		
		allDS.add(toSafe);
		//System.out.println("#############################################################");
		//persistentHandler.safe(toSafe, cursor, set);

	}

	private void handleNode(Node nodeRecord, String path, HashMap<String, String> toSafe) throws Exception {
		NodeList metadataNodeList = (NodeList) xpath.evaluate("*", nodeRecord, XPathConstants.NODESET);
		for (int i = 0; i < metadataNodeList.getLength(); i++) {
			Node child = metadataNodeList.item(i);
			String nodeName = child.getNodeName();
			NodeList metadataNodeChild = (NodeList) xpath.evaluate("*", child, XPathConstants.NODESET);
			if (metadataNodeChild.getLength() > 0) {
				String newpath = (path.equals("")) ? nodeName : path+"_"+ nodeName;
				//System.out.println(nodeName+" hasChildren path:"+newpath);
				handleNode(child, newpath, toSafe);
			} else {
				//System.out.println(nodeName+" NO Children");
				String childVal = (String) xpath.evaluate(".", child, XPathConstants.STRING);
				
				while(toSafe.keySet().contains(path)){
					path+="_";
				}
				toSafe.put(path, childVal);
			}
		}

	}
	
	@Override
	public HashMap<String, Object> getProperties() {
		// TODO Auto-generated method stub
		return null;
	}
}
