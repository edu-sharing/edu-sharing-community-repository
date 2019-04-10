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

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthPolicy;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class HttpQueryTool {

	Log logger = LogFactory.getLog(HttpQueryTool.class);
	
	static boolean initFinished = false;
	
	static String host = null;
	static String proxyhost = null;

	static String proxyUsername = null;
	static String proxyPass = null;

	static Integer proxyport =null;
	
	static String nonProxyHosts = null;
	
	String basicAuthUn = null;
	
	String basicAuthPw = null;
	
	
	public HttpQueryTool() {
		init();
	}
	
	public HttpQueryTool(String basicAuthUn, String basicAuthPw) {
		this();
		this.basicAuthUn = basicAuthUn;
		this.basicAuthPw = basicAuthPw;
	}
	
	private void init(){
		if(!initFinished){
			try{
				host = PropertiesHelper.getProperty("host", "importerproxy.properties", PropertiesHelper.TEXT);
				proxyhost = PropertiesHelper.getProperty("proxyhost", "importerproxy.properties", PropertiesHelper.TEXT);
				proxyUsername = PropertiesHelper.getProperty("proxyuser", "importerproxy.properties", PropertiesHelper.TEXT);
				proxyPass = PropertiesHelper.getProperty("proxypass", "importerproxy.properties", PropertiesHelper.TEXT);
				String proxyportStr = PropertiesHelper.getProperty("proxyport", "importerproxy.properties", PropertiesHelper.TEXT);
				if (proxyportStr!=null) proxyport = new Integer(PropertiesHelper.getProperty("proxyport", "importerproxy.properties", PropertiesHelper.TEXT));
				nonProxyHosts = PropertiesHelper.getProperty("nonproxyhosts", "importerproxy.properties", PropertiesHelper.TEXT);
				
			}catch(Exception e){
				e.printStackTrace();
			}
			initFinished = true;
		}
	}

	
	public String query(String url) {
		return this.query(url,null,null);
	}

	/**
	 * backward compatbility
	 */
	public String query(String url, Map<String,String> header, HttpMethodBase _method) {
		HttpMethodBase method=_method==null ? new GetMethod(url) : _method;
		method.setFollowRedirects(true);
		
		if(basicAuthUn != null && basicAuthPw != null) {
			method.addRequestHeader("Authorization", "Basic " +java.util.Base64.getEncoder().encodeToString((basicAuthUn +":" +basicAuthPw) .getBytes()));
		}
		
		if(header != null){
			for(Map.Entry<String, String> entry : header.entrySet()){
				method.addRequestHeader(entry.getKey(), entry.getValue());
			}
		}
		return query(method);
	}
	public String query(HttpMethodBase method) {
		HttpClient client = new HttpClient();

		client.getParams().setParameter("http.useragent", "Test Client");


		if(basicAuthUn != null && basicAuthPw != null) {
			method.addRequestHeader("Authorization", "Basic " + Base64.encodeBase64String((basicAuthUn +":" +basicAuthPw) .getBytes()));
		}

		try {
			
			//get host of url to check if its an nonproxy host
			URL urlObj = new URL(method.getURI().getURI());
			String urlHost = urlObj.getHost();
			
			logger.debug("nonProxyHosts:"+nonProxyHosts+" current Host:"+urlHost);

			
			if (host != null && proxyhost != null && proxyport != null && !(nonProxyHosts != null && nonProxyHosts.contains(urlHost)) ) {
				logger.debug("using  proxy proxyhost:" + proxyhost + " proxyport:" + proxyport + " host" + host);
				client.getHostConfiguration().setHost(host);
				client.getHostConfiguration().setProxy(proxyhost, proxyport);

				if (proxyUsername != null && proxyPass != null) {

					List authPrefs = new ArrayList(2);
					authPrefs.add(AuthPolicy.DIGEST);
					authPrefs.add(AuthPolicy.BASIC);
					
					client.getParams().setParameter(AuthPolicy.AUTH_SCHEME_PRIORITY, authPrefs);
					client.getState().setProxyCredentials(new AuthScope(proxyhost, proxyport), new UsernamePasswordCredentials(proxyUsername, proxyPass));

				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		method.getParams().setContentCharset("utf-8");

		String result = null;
		try {

			int returnCode = client.executeMethod(method);
			logger.debug("HttpStatus:"+returnCode);
			if (returnCode == HttpStatus.SC_NOT_IMPLEMENTED) {
				System.err.println("The method is not implemented by this URI");
				// still consume the response body
				result = method.getResponseBodyAsString();
			} else {
				result = method.getResponseBodyAsString();
			}
			
			if(returnCode >= 400){
				throw new HttpException(returnCode,result);
			}

		}catch(IOException e){
			throw new HttpException(0,e.getMessage());
		} finally {
			method.releaseConnection();
		}

		return result;
	}
	
}
