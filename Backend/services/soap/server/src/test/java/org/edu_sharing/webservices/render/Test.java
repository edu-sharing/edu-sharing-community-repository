package org.edu_sharing.webservices.render;

import java.net.URL;

import org.apache.axis.client.Stub;
import org.apache.axis.message.SOAPHeaderElement;
import org.apache.commons.codec.binary.Base64;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.security.Signing;
import org.edu_sharing.webservices.types.KeyValue;

public class Test {

	public static void main(String[] args) {
		try{
		RenderInfo ri = new RenderInfoServiceLocator().getRenderInfo(new URL("http://127.0.0.1:8080/edu-sharing/services/RenderInfo?wsdl"));
		
		
		String timestamp = ""+System.currentTimeMillis();
		
		String signData = "1234"+timestamp;
		
		Signing signing = new Signing();
		
		String algorithm = "SHA1withRSA";
		
		
		
		ApplicationInfo homeRep = new ApplicationInfo("homeApplication.properties.xml");
		
		System.out.println(homeRep.getPrivateKey());
		
		String privateKey = homeRep.getPrivateKey();
		
		
			
			
		
		byte[] signedBytes = signing.sign(signing.getPemPrivateKey(privateKey,CCConstants.SECURITY_KEY_ALGORITHM), signData, algorithm);
		
		signedBytes = new Base64().encode(signedBytes);
		
		((Stub)ri).setHeader(new SOAPHeaderElement("http://render.webservices.edu_sharing.org","timestamp",timestamp));
		((Stub)ri).setHeader(new SOAPHeaderElement("http://render.webservices.edu_sharing.org","appId","local"));
		((Stub)ri).setHeader(new SOAPHeaderElement("http://render.webservices.edu_sharing.org","signature",new String(signedBytes)));
		((Stub)ri).setHeader(new SOAPHeaderElement("http://render.webservices.edu_sharing.org","signed",signData));
		((Stub)ri).setHeader(new SOAPHeaderElement("http://render.webservices.edu_sharing.org","locale","de_DE"));
		
		RenderInfoResult result = ri.getRenderInfoRepo("admin","071f2abf-0000-4f47-8fe5-2cc4e52f0f70", null);
		
		
		System.out.println(result.getContentHash());
		
		System.out.println(result.getPreviewUrl());
		System.out.println(result.getMimeTypeUrl());
		System.out.println(result.getAspects()[0]);
		
		
		for(KeyValue kv : result.getProperties()){
			
			System.out.println("key:" + getLabel(kv.getKey(),result) + " " + kv.getValue());
		}
		
		System.out.println(result.getMdsTemplate());
													
		}catch(Exception e){
			e.printStackTrace();
		}
		

	}
	
	
	static String getLabel(String key, RenderInfoResult result){
		for(KeyValue kv : result.getLabels()){
			
			if(kv.getKey().equals(key)){
				return kv.getValue();
			}
		}
		return key;
	}

}
