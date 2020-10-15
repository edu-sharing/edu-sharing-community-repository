package org.edu_sharing.repository.server;

import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;

public class MetadataServlet extends HttpServlet {

	
	public static final String FORMAT_REPOSITORY = "repository";
	public static final String FORMAT_RENDER = "render";
	public static final String FORMAT_LMS = "lms";
	
	public static final String[] formats ={FORMAT_REPOSITORY,FORMAT_RENDER,FORMAT_LMS};
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		try{
			boolean deliver = LightbendConfigLoader.get().getBoolean("repository.exportMetadata");
			if(!deliver){
				resp.getOutputStream().println("repository.exportMetadata=false");
				return;
			}
			
		}catch(Exception e){
			e.printStackTrace();
			resp.getOutputStream().println(e.getMessage());
		}
		
		String paramFormat = req.getParameter("format");
		
		boolean external = new Boolean(req.getParameter("external"));
		
		if(paramFormat == null || !Arrays.asList(formats).contains(paramFormat)){
			
			resp.getOutputStream().println("usage: http://{hostname}/edu-sharing/metadata?format={format}&external={external}");
			resp.getOutputStream().println(" format=[repository,render,lms]");
			resp.getOutputStream().println(" external=[false,true] false: webservice urls with protocol,host,port true: webserviceurls with clientprotocol,domain,clientport");
			resp.getOutputStream().println(" example: http://localhost/edu-sharing/metadata?format=lms&external=1");
			return; 
		}
		
		
		ApplicationInfo appInfo =  ApplicationInfoList.getHomeRepository();
		
		Properties props = new Properties();
		if(paramFormat.equals(FORMAT_REPOSITORY)){
			props.put(ApplicationInfo.KEY_TRUSTEDCLIENT, "true");
			props.put(ApplicationInfo.KEY_SEARCHABLE, "true");
			props.put(ApplicationInfo.KEY_TYPE,ApplicationInfo.TYPE_REPOSITORY);
			props.put(ApplicationInfo.KEY_REPOSITORY_TYPE,ApplicationInfo.REPOSITORY_TYPE_ALFRESCO);
			props.put(ApplicationInfo.KEY_AUTHENTICATIONWEBSERVICE,appInfo.getAuthenticationwebservice());
			props.put(ApplicationInfo.KEY_HOST,appInfo.getHost());
			props.put(ApplicationInfo.KEY_DOMAIN,appInfo.getDomain());
			props.put(ApplicationInfo.KEY_PORT,appInfo.getPort());
			props.put(ApplicationInfo.KEY_PROTOCOL,appInfo.getProtocol());
			props.put(ApplicationInfo.KEY_CLIENTPORT,appInfo.getClientport());
			props.put(ApplicationInfo.KEY_CLIENTPROTOCOL,appInfo.getClientprotocol());
			props.put(ApplicationInfo.KEY_WEBAPPNAME,appInfo.getWebappname());
			props.put(ApplicationInfo.KEY_WSPATH,appInfo.getWspath());
			props.put(ApplicationInfo.KEY_WSHOTPATH,appInfo.getWshotpath());
			props.put(ApplicationInfo.KEY_ALFRESCOCONTEXT,appInfo.getAlfrescocontext());
			props.put(ApplicationInfo.KEY_SEARCHCLASS,"org.edu_sharing.repository.server.MCAlfrescoAPIClient");
			props.put(ApplicationInfo.KEY_AUTHENTICATIONTOOLCLASS,"org.edu_sharing.repository.server.AuthenticationToolAPI");
			props.put(ApplicationInfo.KEY_CONTENTURL,"");
			props.put(ApplicationInfo.KEY_IS_HOME_NODE,"false");
			props.put(ApplicationInfo.KEY_APPCAPTION,appInfo.getAppCaption());
			props.put(ApplicationInfo.KEY_APPID,appInfo.getAppId());
			props.put(ApplicationInfo.KEY_USERNAME,"");
			props.put(ApplicationInfo.KEY_PASSWORD,"");
			props.put(ApplicationInfo.KEY_METADATASETS_V2,"mds");
			props.put(ApplicationInfo.KEY_PUBLIC_KEY,appInfo.getPublicKey());
			
		}
		
		if(paramFormat.equals(FORMAT_LMS)){
			props.put(ApplicationInfo.KEY_APPCAPTION,appInfo.getAppCaption());
			props.put(ApplicationInfo.KEY_APPID,appInfo.getAppId());
			props.put(ApplicationInfo.KEY_TYPE,ApplicationInfo.TYPE_REPOSITORY);
			props.put(ApplicationInfo.KEY_HOST,appInfo.getHost());
			props.put(ApplicationInfo.KEY_DOMAIN,appInfo.getDomain());
			props.put(ApplicationInfo.KEY_PORT,appInfo.getPort());
			props.put(ApplicationInfo.KEY_PROTOCOL,appInfo.getProtocol());
			props.put(ApplicationInfo.KEY_CLIENTPORT,appInfo.getClientport());
			props.put(ApplicationInfo.KEY_CLIENTPROTOCOL,appInfo.getClientprotocol());
			props.put(ApplicationInfo.KEY_AUTHENTICATIONWEBSERVICE,appInfo.getWebServiceHotUrl(external)+"authbyapp");
			props.put(ApplicationInfo.KEY_AUTHENTICATIONWEBSERVICE+"_wsdl",appInfo.getWebServiceHotUrl(external)+"authbyapp"+"?wsdl");
			props.put("usagewebservice",appInfo.getWebServiceHotUrl(external)+"usage2");
			props.put("usagewebservice_wsdl",appInfo.getWebServiceHotUrl(external)+"usage2?wsdl");
			if(appInfo.getContentUrl() !=  null){
				props.put(ApplicationInfo.KEY_CONTENTURL,appInfo.getContentUrl());
			}
			
			String phpFormatedPubKey = appInfo.getPublicKey().replaceAll("(.{64})", "$1\n");
			phpFormatedPubKey = phpFormatedPubKey.replace("-----END PUBLIC KEY-----", "\n-----END PUBLIC KEY-----");
			props.put(ApplicationInfo.KEY_PUBLIC_KEY,phpFormatedPubKey);
		}
		
		
		
		if(paramFormat.equals(FORMAT_RENDER)){
			props.put(ApplicationInfo.KEY_TYPE,ApplicationInfo.TYPE_REPOSITORY);
			props.put(ApplicationInfo.KEY_HOST,appInfo.getHost());
			props.put(ApplicationInfo.KEY_DOMAIN,appInfo.getDomain());
			props.put(ApplicationInfo.KEY_PORT,appInfo.getPort());
			props.put(ApplicationInfo.KEY_PROTOCOL,appInfo.getProtocol());
			props.put(ApplicationInfo.KEY_CLIENTPORT,appInfo.getClientport());
			props.put(ApplicationInfo.KEY_CLIENTPROTOCOL,appInfo.getClientprotocol());
			props.put(ApplicationInfo.KEY_APPCAPTION,appInfo.getAppCaption());
			props.put(ApplicationInfo.KEY_APPID,appInfo.getAppId());
			props.put(ApplicationInfo.KEY_AUTHENTICATIONWEBSERVICE,appInfo.getWebServiceHotUrl(external)+"authbyapp");
			props.put(ApplicationInfo.KEY_AUTHENTICATIONWEBSERVICE+"_wsdl",appInfo.getWebServiceHotUrl(external)+"authbyapp"+"?wsdl");
			props.put("usagewebservice",appInfo.getWebServiceHotUrl(external)+"usage2");
			props.put("usagewebservice_wsdl",appInfo.getWebServiceHotUrl(external)+"usage2?wsdl");
			props.put(ApplicationInfo.KEY_USERNAME,"");
			props.put(ApplicationInfo.KEY_PASSWORD,"");
			String phpFormatedPubKey = appInfo.getPublicKey().replaceAll("(.{64})", "$1\n");
			phpFormatedPubKey = phpFormatedPubKey.replace("-----END PUBLIC KEY-----", "\n-----END PUBLIC KEY-----");
			props.put(ApplicationInfo.KEY_PUBLIC_KEY,phpFormatedPubKey);
			props.put("renderinfowebservice_wsdl",appInfo.getWebServiceHotUrl(external)+"RenderInfo?wsdl");
		}
		
		
		props.storeToXML(resp.getOutputStream(), "repository application file for application type "+paramFormat, "UTF-8");
		
	}
	
}
