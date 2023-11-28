package org.edu_sharing.repository.server.tools;

import com.typesafe.config.Config;
import org.alfresco.repo.cache.SimpleCache;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.*;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.*;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.util.EntityUtils;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.alfresco.tools.HttpQueryToolConfig;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import java.io.ByteArrayInputStream;
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

	int timeout = -1;

	public HttpQueryTool() {
		init();
	}

	public HttpQueryTool(String basicAuthUn, String basicAuthPw) {
		this();
		this.basicAuthUn = basicAuthUn;
		this.basicAuthPw = basicAuthPw;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	private void init(){
		if(configCache != null && configCache.get(CACHE_KEY) == null){
			try{
				Config root = LightbendConfigLoader.get();
				Config httpClientConfiguration = root.getConfig("repository.httpclient");
				Config proxyConfiguration = httpClientConfiguration.getConfig("proxy");
				HttpQueryToolConfig httpQueryToolConfig = new HttpQueryToolConfig();
				if(proxyConfiguration.hasPath("proxyhost")) httpQueryToolConfig.getProxyConfig().setProxyhost(proxyConfiguration.getString("proxyhost"));
				if(proxyConfiguration.hasPath("proxyport")) httpQueryToolConfig.getProxyConfig().setProxyport(proxyConfiguration.getInt("proxyport"));
				if(proxyConfiguration.hasPath("host")) httpQueryToolConfig.getProxyConfig().setHost(proxyConfiguration.getString("host"));
				if(proxyConfiguration.hasPath("proxyuser")) httpQueryToolConfig.getProxyConfig().setProxyUsername(proxyConfiguration.getString("proxyuser") );
				if(proxyConfiguration.hasPath("proxypass")) httpQueryToolConfig.getProxyConfig().setProxyPass(proxyConfiguration.getString("proxypass"));
				if(proxyConfiguration.hasPath("nonproxyhosts")) httpQueryToolConfig.getProxyConfig().setNonProxyHosts(proxyConfiguration.getString("nonproxyhosts"));
				if(httpClientConfiguration.hasPath("disableSNI4Hosts")) httpQueryToolConfig.setDisableSNI4Hosts(httpClientConfiguration.getStringList("disableSNI4Hosts"));

				configCache.put(CACHE_KEY, httpQueryToolConfig);

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

		return execute(url, header, method, followRedirects, null);
	}

	private String execute(String url, Map<String,String> header, HttpUriRequest method, boolean followRedirects, Callback callback){
		logger.debug("url:" + url);

		if(url == null && method != null){
			url = method.getURI().toString();
		}

		if(method == null){
			method = new HttpGet(url);
		}

		CloseableHttpClient client= null;
		try {
			URL urlObj = new URL(url);
			String urlHost = urlObj.getHost();
			//@todo check for config
            HttpQueryToolConfig proxyConf = (configCache != null) ? (HttpQueryToolConfig)configCache.get(CACHE_KEY) : null;
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
							rsStrg = EntityUtils.toString(result,"UTF-8");
						}
						throw new HttpException(returnCode,rsStrg);
					}

					if(callback == null){
						return EntityUtils.toString(result,"UTF-8");
					}else{
						callback.handle(result.getContent());
						return null;
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

	public void queryStream(String url, Callback callback) {
		execute(url, null, null, true, callback);
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
			method.setHeader("Content-Type", "text/plain; charset=UTF-8");
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
		clientBuilder.setDefaultRequestConfig(RequestConfig.copy(RequestConfig.DEFAULT).
				setConnectTimeout(timeout).setConnectionRequestTimeout(timeout).setSocketTimeout(timeout).build()
		);
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

			HttpQueryToolConfig conf = (configCache != null) ? (HttpQueryToolConfig)configCache.get(CACHE_KEY) : null;
			if (conf != null) {
				logger.debug("nonProxyHosts:" + conf.getProxyConfig().getNonProxyHosts());

				if (conf.getProxyConfig().getProxyhost() != null && conf.getProxyConfig().getProxyport() != null
						&& !(conf.getProxyConfig().getNonProxyHosts() != null && conf.getProxyConfig().getNonProxyHosts().contains(urlHost))) {
					logger.debug("using  proxy proxyhost:" + conf.getProxyConfig().getProxyhost() + " proxyport:" + conf.getProxyConfig().getProxyport() + " host" + conf.getProxyConfig().getHost());

					HttpHost proxy = new HttpHost(conf.getProxyConfig().getProxyhost(), conf.getProxyConfig().getProxyport());


					if(conf.getProxyConfig().getHost() != null && !conf.getProxyConfig().getHost().trim().equals("")){
						logger.warn("proxyConf.host is not longer supported");
					}

					DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
					clientBuilder.setRoutePlanner(routePlanner);
					//clientBuilder.setProxy(proxy)
					if (conf.getProxyConfig().getProxyUsername() != null && conf.getProxyConfig().getProxyPass() != null) {

						if(credentialsProvider == null){
							credentialsProvider = new BasicCredentialsProvider();
						}
						credentialsProvider.setCredentials(new AuthScope(conf.getProxyConfig().getProxyhost(),conf.getProxyConfig().getProxyport()),
								new UsernamePasswordCredentials(conf.getProxyConfig().getProxyUsername(), conf.getProxyConfig().getProxyPass()));
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

	public static abstract class Callback<T extends Object>{
		T result;

		/**
		 * do not store InputStream here cause it would be closed after handle method is called
		 * you can store exceptions or other informations about inputstream handling here
		 * @param result
		 */
		public void setResult(T result){
			this.result = result;
		}

		public T getResult() {
			return result;
		}

		public abstract void handle(InputStream httpResult);
	}
}
