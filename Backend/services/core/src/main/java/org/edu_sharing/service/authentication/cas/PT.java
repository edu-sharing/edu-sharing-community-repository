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
package org.edu_sharing.service.authentication.cas;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;

/**
 * 
 * 
 * This class is used to get an PT for an PGT
 * it request's the proxyUrl and parses the xml result
 */
public class PT {

	String proxyUrl = null;
	Logger logger = Logger.getLogger(PT.class);

	public PT(String _proxyUrl) {
		proxyUrl = _proxyUrl;
	}

	/**
	 * 
	 * @param url
	 * @return
	 */
	private String getPT(String url) {
		logger.info("starting ... url:"+url);
	
		try {
			GetMethod get = new GetMethod(url);
			HttpClient client = new HttpClient();
			client.executeMethod(get);

			String test = get.getResponseBodyAsString();
			System.out.println("To Validate:");
			System.out.println(test);

			InputStream result = get.getResponseBodyAsStream();

			Document doc = new SAXBuilder().build(result);
			Element rootEle = doc.getRootElement();
			if (rootEle != null) {
				System.out.println("rootEle.getName():" + rootEle.getName());
				Namespace ns = rootEle.getNamespace();
				System.out.println("ns.getPrefix():" + ns.getPrefix());
				if (rootEle.getName().equals("serviceResponse")) {
					Element succEle = rootEle.getChild("proxySuccess", ns);

					for (Object obj : rootEle.getChildren()) {
						System.out.println(" childs of root:" + ((Element) obj).getName());
					}
					if (succEle != null) {
						System.out.println(" succEle :" + succEle.getName());
						Element elePT = succEle.getChild("proxyTicket", ns);
						System.out.println("elePT.getName():" + elePT.getName());
						if (elePT != null) {
							logger.info("...ends and returns elePT.getValue()");
							return elePT.getValue();
						}
					}
				}
			}
		} catch (Exception e) {
			logger.error("...ends cause of exception"+e.getMessage());
			e.printStackTrace();
			return null;
		}
		logger.info("...ends and returns null");
		return null;
	}

	public String getProxyTicket(String proxyGrantingTicket) throws HttpException, IOException {
		
		String url = new String(proxyUrl);
		if (url.contains("?")) {
			url = url + "&" + "pgt=" + proxyGrantingTicket;
		} else {
			url = url + "?" + "pgt=" + proxyGrantingTicket;
		}
		return getPT(url);
	}
	
	/**
	 * removes all invalid pgtIou's and their pgt's form PGTStack
	 */
	public void clearPGTStack() {
		logger.info("starting ...");
		try {
			for (String key : PGTStack.getPgtIous()) {

				String pt = getProxyTicket(key);
				if(pt == null){
					PGTStack.remove(key);
				}
			}
		} catch (Exception e) {
			logger.error(e);
			e.printStackTrace();
		}
		logger.info("... ends");
	}
}
