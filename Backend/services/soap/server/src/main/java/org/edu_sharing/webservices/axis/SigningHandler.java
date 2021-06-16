package org.edu_sharing.webservices.axis;

import javax.servlet.http.HttpServletResponse;
import javax.xml.soap.SOAPException;

import org.apache.axis.AxisFault;
import org.apache.axis.MessageContext;
import org.apache.axis.handlers.BasicHandler;
import org.apache.axis.message.SOAPHeaderElement;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.server.tools.security.SignatureVerifier;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class SigningHandler extends BasicHandler{

	Logger log = Logger.getLogger(SigningHandler.class);
	
	@Override
	public void invoke(MessageContext msgContext) throws AxisFault {
		
		try{
			
			String appId = getHeaderValue("appId", msgContext);
			String timestamp = getHeaderValue("timestamp", msgContext);
			String signature = getHeaderValue("signature", msgContext);
			String signed = getHeaderValue("signed", msgContext);
			
			SignatureVerifier sv = new SignatureVerifier();
			
			SignatureVerifier.Result result = sv.verify(appId, signature, signed, timestamp);
			
			if(result.getStatuscode() != HttpServletResponse.SC_OK){
				log.error("StatusCode: " + appId+ " " + result.getStatuscode()+" "+result.getMessage());
				throw new AxisFault(result.getMessage());
			}
			
			
		}catch(SOAPException e){
			e.printStackTrace();
		}
	}
	
	
	String getHeaderValue(String key, MessageContext msgContext) throws SOAPException, AxisFault{
		NodeList  list = msgContext.getMessage().getSOAPHeader().getElementsByTagName(key);
		
		for(int i = 0; i < list.getLength(); i++){
			Node node = list.item(i);
			
			SOAPHeaderElement ele =  (SOAPHeaderElement)node;
			
			//System.out.println(node.getNodeName()+" "+ node.getNodeValue()+ " node.getLocalName(): "+node.getLocalName()+ " she:"+ele.getValue());
			
			
			
			if(key.equals(node.getLocalName())){
				return ele.getValue();
			}
			
		}
		return null;
	}

}
