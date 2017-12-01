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
package org.edu_sharing.repository.client.tools.forms;

import com.google.gwt.http.client.URL;
import com.google.gwt.xml.client.Element;
import com.google.gwt.xml.client.XMLParser;

public class CrudResult {

	
	public static final String ELE_ROOT = "result";
	
	public static final String ELE_NODEID = "nodeid";
	
	public static final String ELE_STATUS = "status";
	
	public static final String ELE_STATUS_MESSAGE = "statusmessage";
	
	public static final String STATUS_SUCCESS = "success";
	
	public static final String STATUS_ERROR = "error";
	
	public static final String STATUS_ERROR_QUOTA = "error_quota";
	
	String nodeId;
	String status;
	String statusMessage;
	
	public CrudResult(String servletResult) {
		servletResult = URL.decodeQueryString(servletResult);
			
		//make <PRE> to <pre>
		servletResult = servletResult.toLowerCase();
		servletResult = servletResult.replaceAll("(.*&lt;pre&gt;|&lt;/pre&gt;.*|.*&lt;pre>|&lt;/pre>.*|.*<pre[ a-zA-Z=\"-:;()]*>|</pre>.*)", "");
				
		Element rootEle = XMLParser.parse(servletResult).getDocumentElement();
		if(rootEle != null){
			com.google.gwt.xml.client.Node xmlStatus = rootEle.getElementsByTagName(ELE_STATUS).item(0);
			status =  xmlStatus.getFirstChild().getNodeValue();
			
			com.google.gwt.xml.client.Node xmlStatusMsg = rootEle.getElementsByTagName(ELE_STATUS_MESSAGE).item(0);
			if (xmlStatusMsg.hasChildNodes()) statusMessage =  xmlStatusMsg.getFirstChild().getNodeValue();
			
			com.google.gwt.xml.client.Node xmlNodeNodeId = rootEle.getElementsByTagName(ELE_NODEID).item(0);
			if (xmlNodeNodeId.hasChildNodes()) nodeId = xmlNodeNodeId.getFirstChild().getNodeValue();
			
		}else{
		}
	}
	
	
	public boolean isError(){
		if(!status.equals(STATUS_SUCCESS)){
			return true;
		}
		return false;
	}
	
	public String getNodeId() {
		return nodeId;
	}
	
	public String getStatus() {
		return status;
	}
	
	public String getStatusMessage() {
		return statusMessage;
	}
	
}
