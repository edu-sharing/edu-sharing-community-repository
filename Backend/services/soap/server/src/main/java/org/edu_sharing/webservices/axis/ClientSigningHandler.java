package org.edu_sharing.webservices.axis;

import java.util.Iterator;

import javax.xml.soap.SOAPBody;

import org.apache.axis.AxisFault;
import org.apache.axis.MessageContext;
import org.apache.axis.handlers.BasicHandler;
import org.apache.axis.message.SOAPHeaderElement;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.security.Signing;

public class ClientSigningHandler extends BasicHandler{
	
	Logger logger = Logger.getLogger(ClientSigningHandler.class);
	
	@Override
	public void invoke(MessageContext context) throws AxisFault {
		logger.debug("called");
		try{
	
			
			//only sign the request not the response
			if(context.getPastPivot() == false){
				
			
				
				Signing signing = new Signing();
				
				String toSign = context.getMessage().getSOAPBody().toString();
				
				//replace namespaceprefixes
				toSign = toSign.replaceAll("<[a-zA-Z0-9]*:", "<");
				toSign = toSign.replaceAll("</[a-zA-Z0-9]*:", "<");
				toSign = toSign.replaceAll("xmlns:[a-zA-Z0-9]*=", "xmlns:signed=");
				
				
				
				String timeStamp = new Long(System.currentTimeMillis()).toString();
				toSign += timeStamp;
				
				logger.debug("toSign:"+toSign +" appId:"+ApplicationInfoList.getHomeRepository().getAppId());
				
				byte[] signature = signing.sign(signing.getPemPrivateKey(ApplicationInfoList.getHomeRepository().getPrivateKey(), CCConstants.SECURITY_KEY_ALGORITHM),toSign , CCConstants.SECURITY_SIGN_ALGORITHM);
				signature = new Base64().encode(signature);
				
				context.getRequestMessage().getSOAPHeader().addChildElement(new SOAPHeaderElement("http://webservices.edu_sharing.org","timestamp",timeStamp));
				context.getRequestMessage().getSOAPHeader().addChildElement(new SOAPHeaderElement("http://webservices.edu_sharing.org","appId",ApplicationInfoList.getHomeRepository().getAppId()));
				context.getRequestMessage().getSOAPHeader().addChildElement(new SOAPHeaderElement("http://webservices.edu_sharing.org","signature",new String(signature)));
				
			
			
				logger.debug("toSignTest:" + context.getRequestMessage().getSOAPBody().toString());
			}
		}catch(Exception e){
			logger.error(e.getMessage(), e);
		}
		
	}
}
