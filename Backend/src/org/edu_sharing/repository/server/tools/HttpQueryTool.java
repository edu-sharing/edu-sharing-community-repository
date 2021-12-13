package org.edu_sharing.repository.server.tools;

import com.typesafe.config.Config;
import org.alfresco.repo.cache.SimpleCache;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.*;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.*;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.util.EntityUtils;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.alfresco.tools.ProxyConfig;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Map;

public class HttpQueryTool {

	private static String CACHE_KEY = "HTTPCLIENT_PROXY_CACHE";
	private static SimpleCache<String, Serializable> configCache = (AlfAppContextGate.getApplicationContext() != null) ? (SimpleCache<String, Serializable>) AlfAppContextGate.getApplicationContext().getBean("eduSharingConfigCache") : null;


	Log logger = LogFactory.getLog(HttpQueryTool.class);



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
		if(configCache != null && configCache.get(CACHE_KEY) == null){
			try{
				Config root = LightbendConfigLoader.get();
				Config config = root.getConfig("repository.proxy");
				ProxyConfig proxyConfig = new ProxyConfig();
				proxyConfig.setProxyhost(config.getString("proxyhost"));
				proxyConfig.setProxyport(config.getInt("proxyport"));
				if(config.hasPath("host")){ proxyConfig.setHost(config.getString("host"));};
				if(config.hasPath("proxyuser"))proxyConfig.setProxyUsername(config.getString("proxyuser") );
				if(config.hasPath("proxypass"))proxyConfig.setProxyPass(config.getString("proxypass"));
				if(config.hasPath("nonproxyhosts")) proxyConfig.setNonProxyHosts(config.getString("nonproxyhosts"));
				if(root.hasPath("disableSNI4Hosts")) proxyConfig.setDisableSNI4Hosts(root.getStringList("disableSNI4Hosts"));

				configCache.put(CACHE_KEY,proxyConfig);

			}catch(Exception e){
				logger.info("No proxy to use found or invalid proxy config: "+e.getMessage());
				logger.info("If no proxy should be used, you can ignore this message");
			}
		}
	}

	public String query(String url) {
		return this.query(url,null,null,true);
	}

	public String query(HttpUriRequest method){
		return this.query(method.getURI().toString(),null,method,true);
	}

	public String query(String url, Map<String,String> header, HttpUriRequest _method) {
		return query(url,header,_method,true);
	}

	public String query(String url, Map<String,String> header, HttpUriRequest method, boolean followRedirects) {

		return execute(url, header, method, followRedirects, String.class);
	}

	private <T extends Object> T execute(String url, Map<String,String> header, HttpUriRequest method, boolean followRedirects, Class<T> type){
		logger.debug("url:" + url);

		if(method == null){
			method = new HttpGet(url);
		}

		CloseableHttpClient client= null;
		try {
			URL urlObj = new URL(url);
			String urlHost = urlObj.getHost();
			//@todo check for config
            ProxyConfig proxyConf = (configCache != null) ? (ProxyConfig)configCache.get(CACHE_KEY) : null;
            if (proxyConf != null && proxyConf.getDisableSNI4Hosts().contains(urlHost)) {
                client = prepare(url,header,method,followRedirects, false);
            }else{
                client = prepare(url,header,method,followRedirects, true);
            }
			
		} catch (NoSuchAlgorithmException e) {
			logger.error(e.getMessage(),e);
			return null;
		}catch(MalformedURLException e){
			logger.error(e.getMessage(),e);
			return null;
		}

		HttpEntity result;
		try {
			CloseableHttpResponse response = client.execute(method);

			try{
				int returnCode = response.getStatusLine().getStatusCode();
				logger.debug("HttpStatus:"+returnCode);
				if (returnCode == HttpStatus.SC_NOT_IMPLEMENTED) {
					logger.error("The method is not implemented by this URI");
					// still consume the response body
				}
				result = response.getEntity();

				try{
					if(returnCode >= 400){
						String rsStrg = null;
						if (result != null) {
							rsStrg = EntityUtils.toString(result);
						}
						throw new HttpException(returnCode,rsStrg);
					}

					if(type == String.class){
						return (T)EntityUtils.toString(result);
					}else if(type == InputStream.class){
						return (T)result.getContent();
					}else{
						throw new RuntimeException("unsupported return type:"+type);
					}
				}finally {
					response.close();
				}



			}finally {
				response.close();
			}
		}catch (IOException e){
			logger.debug(e.getMessage(),e);
			throw new HttpException(0,e.getMessage());
		}finally {
			try {
				client.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public InputStream getStream(String url) {
		return execute(url, null, null, true, InputStream.class);
	}

	public <T extends Object> T query(String url, Class<T> type){
		return null;
	}


	/**
	 * basic auth, proxy server handling, Content-Type "charset=UTF-8"
	 * @param url
	 * @param method
	 * @return
	 */
	private CloseableHttpClient prepare(String url,
										Map<String,String> header,
										HttpUriRequest method,
										boolean followRedirects,
										boolean sni) throws NoSuchAlgorithmException {

		Header[] headersContentType = method.getHeaders("Content-Type");
		if(headersContentType == null || headersContentType.length == 0){
			method.setHeader("Content-Type", "charset=UTF-8");
		}


		if(header != null){
			for(Map.Entry<String, String> entry : header.entrySet()){
				method.setHeader(entry.getKey(), entry.getValue());
			}
		}

		HttpClientBuilder clientBuilder =
				(sni) ? HttpClientBuilder.create()
						: HttpClients.custom()
						.setSSLSocketFactory(new SSLConnectionSocketFactory(SSLContext.getDefault(), SSLConnectionSocketFactory.getDefaultHostnameVerifier()) {
							@Override
							protected void prepareSocket(SSLSocket socket) throws IOException {
								SSLParameters sslParameters = socket.getSSLParameters();
								sslParameters.setServerNames(new ArrayList<>());
								socket.setSSLParameters(sslParameters);
							}
						});

		if(followRedirects){
			//LaxRedirectStrategy = commons.httpclient method.setFollowRedirects(true);
			clientBuilder.setRedirectStrategy(new LaxRedirectStrategy());
		}

		clientBuilder.setUserAgent("Test Client");

		//basic auth
		CredentialsProvider credentialsProvider = null;
		if(basicAuthUn != null && basicAuthPw != null) {
			credentialsProvider = new BasicCredentialsProvider();
			credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(basicAuthUn, basicAuthPw));
			clientBuilder.setDefaultCredentialsProvider(credentialsProvider);
		}

		//proxy
		try {

			//get host of url to check if its an nonproxy host
			URL urlObj = new URL(url);
			String urlHost = urlObj.getHost();
			logger.debug("current Host:" + urlHost);

			ProxyConfig proxyConf = (configCache != null) ? (ProxyConfig)configCache.get(CACHE_KEY) : null;
			if (proxyConf != null) {
				logger.debug("nonProxyHosts:" + proxyConf.getNonProxyHosts());

				if (proxyConf.getHost() != null && proxyConf.getProxyhost() != null && proxyConf.getProxyport() != null
						&& !(proxyConf.getNonProxyHosts() != null && proxyConf.getNonProxyHosts().contains(urlHost))) {
					logger.debug("using  proxy proxyhost:" + proxyConf.getProxyhost() + " proxyport:" + proxyConf.getProxyport() + " host" + proxyConf.getHost());

					HttpHost proxy = new HttpHost(proxyConf.getProxyhost(), proxyConf.getProxyport());


					if(proxyConf.getHost() != null && !proxyConf.getHost().trim().equals("")){
						logger.warn("proxyConf.host is not longer supported");
					}

					DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
					clientBuilder.setRoutePlanner(routePlanner);
					//clientBuilder.setProxy(proxy)
					if (proxyConf.getProxyUsername() != null && proxyConf.getProxyPass() != null) {

						if(credentialsProvider == null){
							credentialsProvider = new BasicCredentialsProvider();
						}
						credentialsProvider.setCredentials(new AuthScope(proxyConf.getProxyhost(),proxyConf.getProxyport()),
								new UsernamePasswordCredentials(basicAuthUn, basicAuthPw));
						clientBuilder.setDefaultCredentialsProvider(credentialsProvider);
						clientBuilder.setProxyAuthenticationStrategy(new ProxyAuthenticationStrategy());
					}

				}
			}


		} catch (Exception e) {
			logger.error(e.getMessage(),e);
		}

		return clientBuilder.build();
	}

	public static void invalidateProxySettings(){
		configCache.remove(CACHE_KEY);
	}
}
