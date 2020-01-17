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
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.typesafe.config.Config;
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
import org.edu_sharing.lightbend.LightbendConfigLoader;

public class HttpQueryTool {

	Log logger = LogFactory.getLog(HttpQueryTool.class);
	
	public static boolean initFinished = false;
	
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
				Config config = LightbendConfigLoader.get().getConfig("repository.config");
				host = config.getString("host");
				proxyhost = config.getString("proxyhost");
				proxyUsername = config.getString("proxyuser");
				proxyPass = config.getString("proxypass");
				proxyport = config.getInt("proxyport");
				nonProxyHosts = config.getString("nonproxyhosts");
			}catch(Exception e){
				logger.info("No proxy to use found or invalid proxy config: "+e.getMessage());
				logger.info("If no proxy should be used, you can ignore this message");
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
		logger.debug("url:" + url);
		HttpClient client = new HttpClient();

		client.getParams().setParameter("http.useragent", "Test Client");

		HttpMethodBase method;
		if(_method == null) {
			method = new GetMethod(url);
			method.setFollowRedirects(true);
		}
		else{
			method = _method;
		}
		
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
	public InputStream getStream(HttpMethodBase method) {
		HttpClient client = prepareClient(method);
		try {

			int returnCode = client.executeMethod(method);
			if(returnCode==200){
				return method.getResponseBodyAsStream();
			}
			if(returnCode >= 400){
				throw new HttpException(returnCode,method.getResponseBodyAsString());
			}
		} catch (IOException e) {
			throw new HttpException(0,e.getMessage());
		}
		return null;
	}
	public String query(HttpMethodBase method) {
		HttpClient client = prepareClient(method);

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

	private HttpClient prepareClient(HttpMethodBase method) {
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
		return client;
	}

}
