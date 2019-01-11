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

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

public class ApplicationInfo implements Comparable<ApplicationInfo>{

	public static final long DEFAULT_OFFSET_MS = 10000;

	/**
	 * property file keys
	 * 
	 */
	public static final String KEY_HOST = "host";
	
	public static final String KEY_DOMAIN = "domain";
	
	public static final String KEY_PORT = "port";
	
	public static final String KEY_PROTOCOL = "protocol";
	
	public static final String KEY_CLIENTPORT = "clientport";
	
	public static final String KEY_CLIENTPROTOCOL = "clientprotocol";
	
	public static final String KEY_TYPE = "type";
	
	public static final String KEY_REPOSITORY_TYPE = "repositorytype";
	
	public static final String KEY_TRUSTEDCLIENT = "trustedclient";
	
	public static final String KEY_SEARCHABLE = "searchable";
	
	public static final String KEY_APPCAPTION = "appcaption";
	
	public static final String KEY_APPID = "appid";
	
	public static final String KEY_USERNAME = "username";
	
	public static final String KEY_PASSWORD = "password";
	
	public static final String KEY_LOGO = "logo";
	
	public static final String KEY_ICON = "icon";
	
	public static final String KEY_AUTHENTICATIONWEBSERVICE = "authenticationwebservice";
	
	public static final String KEY_WEBAPPNAME = "webappname";
	
	public static final String KEY_WSPATH = "wspath";
	
	public static final String KEY_WSHOTPATH = "wshotpath";
	
	public static final String KEY_ALFRESCOCONTEXT = "alfrescocontext";
	
	public static final String KEY_SEARCHCLASS = "searchclass";
	
	public static final String KEY_AUTHENTICATIONTOOLCLASS = "authenticationtoolclass";
	
	public static final String KEY_CONTENTURL = "contenturl";

	public static final String KEY_PREVIEWURL = "previewurl";
	
	public static final String KEY_IS_HOME_NODE = "is_home_node";
	
	public static final String KEY_CUSTOM_HTML_HEADERS = "custom_html_headers";
	
	public static final String KEY_METADATASETS = "metadatasets";
	public static final String KEY_METADATASETS_V2 = "metadatasetsV2";
	
	public static final String KEY_PUBLIC_KEY = "public_key";
	public static final String KEY_PRIVATE_KEY = "private_key";
	
	public static final String KEY_MESSAGE_OFFSET_MILLISECONDS = "message_offset_ms";
	public static final String KEY_MESSAGE_SEND_OFFSET_MILLISECONDS = "message_send_offset_ms";
	
	public static final String KEY_LOGOUT_URL = "logouturl";
	
	public static final String KEY_API_KEY = "apikey";
	
	public static final String KEY_ORDER = "order";

	
	/**
	 * property file vals
	 */
	public static final String TYPE_REPOSITORY = "REPOSITORY";
	public static final String TYPE_LMS = "LMS";
	
	/**
	 * SERVICE = renderservice -> reserved, no other system can use this
	 */
	public static final String TYPE_RENDERSERVICE = "SERVICE";
	
	public static final String TYPE_CONNECTOR = "CONNECTOR";
	
	public static final String REPOSITORY_TYPE_ALFRESCO = "ALFRESCO"; // a remote alfresco repository
	
	public static final String REPOSITORY_TYPE_LOCAL = "LOCAL"; // the local alfreso repository
	
	public static final String REPOSITORY_TYPE_EDUNEX = "EDUNEX";
	
	public static final String REPOSITORY_TYPE_YOUTUBE = "YOUTUBE";
	
	public static final String REPOSITORY_TYPE_DDB = "DDB";
	
	public static final String REPOSITORY_TYPE_WIKIMEDIA = "WIKIMEDIA";

	public static final String AUTHBYAPP_USERNAME_PROP_MAIL = "MAIL";
	
	public static final String AUTHBYAPP_USERNAME_PROP_USERNAME = "USERNAME";
	
	public static final String WEBSITEPREVIEWRENDERSERVICE = "websitepreviewrenderservice";

	public static final String REPOSITORY_TYPE_MEMUCHO = "MEMUCHO";

	private String host = null;
	
	/**
	 * an , separated List of Hosts, maybe ip ranges in future
	 */
	private String hostAliases = null;
	
	/**
	 * used for previewurls or content urls, usefull when we got an proxy
	 */
	private String domain = null;
	
	private String alfrescocontext = null;
	
	private String port = null;
	
	private String protocol = null;
	
	private String clientport = null;
	
	private String clientprotocol = null;
	
	private String wspath = null;
	
	private String wshotpath = null;
	
	private String webappname = null;
	
	private String path = null;

	private String appFile = null;
	
	private String ishomeNode = null;
	
	private String username = null;
	
	private String password = null;
	
	private String logo = null;
	
	private String icon = null;

	private String guest_username = null;
	
	private String guest_password = null;
	
	private String appCaption = null;
	
	private String appId = null;
	
	private String trustedclient = null;
	
	private String type = null;
	
	private String authenticationwebservice = null;
	
	private String permissionwebservice = null;
	
	private String subtype = null;
	
	private String publicfolderid = null;

	private String searchclass = null;
	
	private String searchService = null;
	
	private String permissionService = null;
	
	private String nodeService = null;
	
	private String authenticationtoolclass = null;
	
	private String repositoryType = null;
	
	/**
	 * this is a property used to redirect to content deliverd by repositories renderservice
	 */
	private String contentUrl = null;

	/**
	 * this is a property used redirect to preview deliverd by repositories renderservice
	 */
	private String previewUrl = null;
	
	private String customHtmlHeaders = null;

	private String logoutUrl = null;

	private String nodeIdKey = null;
	
	private String availableSearchCriterias = null;
	
	private String websitepreviewrenderservice;
	
	private String searchable = "true";
	
	//file that contains metadatasets for the repository
	private String metadatsets = null;
	private String metadatsetsV2 = null;
	
	//devmode metadatasets will be parsed every time and not cached in RepoFactory
	private String devmode = null;
	
	//recommend objects search
	private String recommend_objects_query = null;
	
	
	//for lms: if this mail is configured and the user authenticates with this mail than we authenticate without sending an confirmation mail
	private String trustedEmailAddress = null;
	
	//allow to switch from email as username to username like the app. the static vars AUTHBYAPP_USERNAME_PROP_MAIL, AUTHBYAPP_USERNAME_PROP_USERNAME are valid values 
	private String authByAppUsernameProp = null;
	
	//allow login from applications without sending an confirmation mail
	private boolean authByAppSendMail = false;
	
	private String publicKey = null;
	
	private String privateKey = null;
	
	private String messageOffsetMs = null;
	
	private String messageSendOffsetMs = null;
	
	
	/**
	 * allow the mapping from the username send as an authByApp param to an userDirectory Attribute
	 * the mapped value will be taken as the repositoryusername
	 * 
	 * authByAppUsernameMappingDirectoryUsername: the property which is used as a key to ask the userDirectory
	 * 
	 * auth_by_app_usernamemapping_dir_username
	 * 
	 * authByAppUsernameMappingRepositoryUsername: the userdirectory property which value is used as respository username 
	 * 
	 * auth_by_app_usernamemapping_rep_username
	 */
	private String authByAppUsernameMappingDirectoryUsername = null;
	
	private String authByAppUsernameMappingRepositoryUsername = null;
	
	//an "," separated String with authentication Types that need to be explicit activated. i.e. valid value is "shibboleth"
	private String allowedAuthenticationTypes = null;
	
	//for lms or other connected systems: if they call the gwt app with the css_appid param then the custom css file is included in the html
	private String customCss = null;
	
	private int trackingBufferSize = 0;
	
	private String apiKey;
	
	private int order;

	Logger logger = Logger.getLogger(ApplicationInfo.class);

	private String xml;

	
	public ApplicationInfo(String _appFile) throws Exception{
		if(_appFile == null) throw new Exception("Application Filename was null!");
		appFile = _appFile;
		
		ClassLoader classLoader = Thread.currentThread()
				.getContextClassLoader();
		URL url = classLoader.getResource(appFile);
		xml = new String(Files.readAllBytes(Paths.get(url.toURI())));
		
		//test if file exists if not exception is thrown
		//PropertiesHelper.getProperty("is_home_node", appFile, PropertiesHelper.XML);
		
		host = PropertiesHelper.getProperty(KEY_HOST, appFile, PropertiesHelper.XML);
				
		hostAliases = PropertiesHelper.getProperty("host_aliases", appFile, PropertiesHelper.XML);	
		
		domain = PropertiesHelper.getProperty(KEY_DOMAIN, appFile, PropertiesHelper.XML);	
		
		port = PropertiesHelper.getProperty(KEY_PORT, appFile, PropertiesHelper.XML);
		if(port == null || port.trim().equals("")){
			port="80";
		}
		
		protocol = PropertiesHelper.getProperty(KEY_PROTOCOL, appFile, PropertiesHelper.XML);
		if(protocol == null || protocol.trim().equals("")){
			protocol = "http";
		}
		
		clientport =  PropertiesHelper.getProperty(KEY_CLIENTPORT, appFile, PropertiesHelper.XML);
		clientprotocol =  PropertiesHelper.getProperty(KEY_CLIENTPROTOCOL, appFile, PropertiesHelper.XML);
		
		wspath = PropertiesHelper.getProperty(KEY_WSPATH, appFile, PropertiesHelper.XML);
		ishomeNode = PropertiesHelper.getProperty(KEY_IS_HOME_NODE, appFile, PropertiesHelper.XML);
		if(ishomeNode == null){
			ishomeNode = "false";
		}
		wshotpath = PropertiesHelper.getProperty(KEY_WSHOTPATH, appFile, PropertiesHelper.XML);
		
		webappname = PropertiesHelper.getProperty(KEY_WEBAPPNAME, appFile, PropertiesHelper.XML);
		if(webappname == null) webappname = "edu-sharing";
		
		username = PropertiesHelper.getProperty(KEY_USERNAME, appFile, PropertiesHelper.XML);
		
		password = PropertiesHelper.getProperty(KEY_PASSWORD, appFile, PropertiesHelper.XML);
		
		
		
		guest_username = PropertiesHelper.getProperty("guest_username", appFile, PropertiesHelper.XML);
		
		guest_password = PropertiesHelper.getProperty("guest_password", appFile, PropertiesHelper.XML);
		
		logo = PropertiesHelper.getProperty(KEY_LOGO, appFile, PropertiesHelper.XML);
		
		icon = PropertiesHelper.getProperty(KEY_ICON, appFile, PropertiesHelper.XML);

		appCaption = PropertiesHelper.getProperty(KEY_APPCAPTION, appFile, PropertiesHelper.XML);
		
		appId = PropertiesHelper.getProperty(KEY_APPID, appFile, PropertiesHelper.XML);
		if(appId == null || appId.trim().equals("")){
			logger.error("missing appid in file:"+appFile);
		}
		
		trustedclient = PropertiesHelper.getProperty(KEY_TRUSTEDCLIENT, appFile, PropertiesHelper.XML);
		
		type = PropertiesHelper.getProperty(KEY_TYPE, appFile, PropertiesHelper.XML); 
		
		authenticationwebservice = PropertiesHelper.getProperty("authenticationwebservice", appFile, PropertiesHelper.XML); 
		
		permissionwebservice = PropertiesHelper.getProperty("permissionwebservice", appFile, PropertiesHelper.XML);
		
		subtype  = PropertiesHelper.getProperty("subtype", appFile, PropertiesHelper.XML);
	
		publicfolderid = PropertiesHelper.getProperty("publicfolderid", appFile, PropertiesHelper.XML);

		searchclass = PropertiesHelper.getProperty(KEY_SEARCHCLASS, appFile, PropertiesHelper.XML);	
		
		searchService = PropertiesHelper.getProperty("searchService", appFile, PropertiesHelper.XML);	
		
		permissionService = PropertiesHelper.getProperty("permissionService", appFile, PropertiesHelper.XML);	
		
		nodeService = PropertiesHelper.getProperty("nodeService", appFile, PropertiesHelper.XML);	
		
		authenticationtoolclass = PropertiesHelper.getProperty("authenticationtoolclass", appFile, PropertiesHelper.XML);
		
		repositoryType = PropertiesHelper.getProperty(KEY_REPOSITORY_TYPE, appFile, PropertiesHelper.XML);
		
		contentUrl = PropertiesHelper.getProperty(KEY_CONTENTURL, appFile, PropertiesHelper.XML);

		previewUrl = PropertiesHelper.getProperty(KEY_PREVIEWURL, appFile, PropertiesHelper.XML);
		
		customHtmlHeaders = PropertiesHelper.getProperty(KEY_CUSTOM_HTML_HEADERS, appFile, PropertiesHelper.XML);

		logoutUrl = PropertiesHelper.getProperty(KEY_LOGOUT_URL, appFile, PropertiesHelper.XML);
		
		nodeIdKey = PropertiesHelper.getProperty("nodeid_key", appFile, PropertiesHelper.XML);
		
		availableSearchCriterias = PropertiesHelper.getProperty("availablesearchcriterias", appFile, PropertiesHelper.XML);
		
		searchable = PropertiesHelper.getProperty(KEY_SEARCHABLE, appFile, PropertiesHelper.XML);
		
		if(searchable == null) searchable = "true";
		
		path = PropertiesHelper.getProperty("path", appFile, PropertiesHelper.XML);
		
		metadatsets = PropertiesHelper.getProperty(KEY_METADATASETS, appFile, PropertiesHelper.XML);
		
		metadatsetsV2 = PropertiesHelper.getProperty(KEY_METADATASETS_V2, appFile, PropertiesHelper.XML);
		
		devmode = PropertiesHelper.getProperty("devmode", appFile, PropertiesHelper.XML);
		
		devmode = (devmode == null)? "false": devmode;
		
		alfrescocontext = PropertiesHelper.getProperty(KEY_ALFRESCOCONTEXT, appFile, PropertiesHelper.XML);
		
		recommend_objects_query = PropertiesHelper.getProperty("recommend_objects_query", appFile, PropertiesHelper.XML);
		
		trustedEmailAddress = PropertiesHelper.getProperty("trusted_emailaddress", appFile, PropertiesHelper.XML);
		
		authByAppUsernameProp = PropertiesHelper.getProperty("auth_by_app_username_prop", appFile, PropertiesHelper.XML);
		authByAppUsernameProp = (authByAppUsernameProp == null)? authByAppUsernameProp = ApplicationInfo.AUTHBYAPP_USERNAME_PROP_USERNAME : authByAppUsernameProp;
		
		String tmpAuthByAppSendmail = PropertiesHelper.getProperty("auth_by_app_sendmail", appFile, PropertiesHelper.XML);
		authByAppSendMail = (tmpAuthByAppSendmail == null)? true : new Boolean(tmpAuthByAppSendmail);
		
		authByAppUsernameMappingDirectoryUsername = PropertiesHelper.getProperty("auth_by_app_usernamemapping_dir_username", appFile, PropertiesHelper.XML);
		
		authByAppUsernameMappingRepositoryUsername = PropertiesHelper.getProperty("auth_by_app_usernamemapping_rep_username", appFile, PropertiesHelper.XML);
		
		allowedAuthenticationTypes = PropertiesHelper.getProperty("allowed_authentication_types", appFile, PropertiesHelper.XML);
		
		customCss = PropertiesHelper.getProperty("custom_css", appFile, PropertiesHelper.XML);
		
		String tmpTrackingBufferSize = PropertiesHelper.getProperty("trackingBufferSize", appFile, PropertiesHelper.XML);
		trackingBufferSize = (tmpTrackingBufferSize != null ? Integer.parseInt(tmpTrackingBufferSize) : 0);
		
		publicKey = PropertiesHelper.getProperty(KEY_PUBLIC_KEY, appFile, PropertiesHelper.XML);
		
		privateKey = PropertiesHelper.getProperty(KEY_PRIVATE_KEY, appFile, PropertiesHelper.XML);
		
		messageOffsetMs = PropertiesHelper.getProperty(KEY_MESSAGE_OFFSET_MILLISECONDS, appFile, PropertiesHelper.XML);
		
		messageSendOffsetMs = PropertiesHelper.getProperty(KEY_MESSAGE_SEND_OFFSET_MILLISECONDS, appFile, PropertiesHelper.XML);
		
		apiKey = PropertiesHelper.getProperty(KEY_API_KEY, appFile, PropertiesHelper.XML);

		websitepreviewrenderservice = PropertiesHelper.getProperty(WEBSITEPREVIEWRENDERSERVICE, appFile, PropertiesHelper.XML);

		String orderString = PropertiesHelper.getProperty(KEY_ORDER, appFile, PropertiesHelper.XML);
		order = orderString==null ? (ishomeNode() ? 0 : 1) : Integer.parseInt(orderString);

		getWebServiceUrl();
		getWebServerUrl();
		
	}
	
	public String getXml() {
		return xml;
	}

	public int getOrder() {
		return order;
	}

	public void setXml(String xml) {
		this.xml = xml;
	}

	public String getHost() {
		return host;
	}

	public String getDomain() {

		if(domain == null) {
			return host;
		}
		
		return domain;
	}

	public String getPort() {
		return port;
	}

	public String getWspath() {
		return wspath;
	}
	public String getWebServiceUrl(){
		
		String port = ""; 
		if(!this.getPort().equals("80") && !this.getPort().equals("443")){
			port = ":" + this.getPort();
		}
		
		return this.getProtocol()+"://" + this.getHost() + port + this.getWspath();
	}
	
	public String getWebServiceHotUrl(){
		return getWebServiceHotUrl(false);
	}
	
	public String getWebServiceHotUrl(boolean external){
		String port = ""; 
		if(external){
			if(!this.getClientport().equals("80") && !this.getClientport().equals("443")){
				port = ":" + this.getClientport();
			}
			return this.getClientprotocol()+"://" + this.getDomain() + port + this.getWshotpath();
		}else{
			if(!this.getPort().equals("80") && !this.getPort().equals("443")){
				port = ":" + this.getPort();
			}
			return this.getProtocol()+"://" + this.getHost() + port + this.getWshotpath();
		}
	}
	
		
	public String getWebServerUrl(){
		String port = ""; 
		if(!this.getPort().equals("80") && !this.getPort().equals("443")){
			port = ":" + this.getPort();
		}
		
		return this.getProtocol()+"://"+this.getHost()+port;
	}
	
	public String getClientBaseUrl(){
		String result = this.getClientprotocol() + "://"+ ((this.getDomain() == null) ? this.getHost() : this.getDomain()) +":"+ this.getClientport() + "/"+getWebappname();
		return result;
	}

	public boolean ishomeNode() {
		if(ishomeNode.equals("true")){
			return true;
		}else{
			return false;
		}
	}


	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public String getGuest_username() {
		return guest_username;
	}

	public String getGuest_password() {
		return guest_password;
	}

	public String getAppCaption() {
		return appCaption;
	}

	public String getAppFile() {
		return appFile;
	}

	public String getAppId() {
		return appId;
	}

	public String getTrustedclient() {
		return trustedclient;
	}

	public String getType() {
		return type;
	}

	public String getAuthenticationwebservice() {
		
		return authenticationwebservice;
	}

	public String getPermissionwebservice() {
		
		return permissionwebservice;
	}

	public String getSubtype() {
		return subtype;
	}

	public String getPublicfolderid() {
		return publicfolderid;
	}

	/**
	 * @return the searchclass
	 */
	public String getSearchclass() {
		return searchclass;
	}
	
	public String getSearchService() {
		return searchService;
	}
	
	public String getPermissionService() {
		return permissionService;
	}
	
	public String getNodeService() {
		return nodeService;
	}
	
	public String getAuthenticationtoolclass() {
		return authenticationtoolclass;
	}

	/**
	 * @return the repositoryType
	 */
	public String getRepositoryType() {
		
		return repositoryType;
	}
	
	
	/**
	 *this is a property used redirect to content deliverd by repositories renderservice
	 * 
	 * @return the renderServiceUrl
	 */
	public String getContentUrl() {
		
		return contentUrl;
	}
	
	/**
	 * this is a property used redirect to preview deliverd by repositories renderservice
	 * 
	 * @return the previewUrl
	 */
	public String getPreviewUrl() {
		
		return previewUrl;
	}
	
	public String getCustomHtmlHeaders() {
		return customHtmlHeaders;
	}

	public String getLogoutUrl() {
		
		if(logoutUrl == null || logoutUrl.trim().equals("")){
			return "/edu-sharing/logout";
		}
		
		return logoutUrl;
	}
	
	/**
	 * @return the nodeIdKey
	 */
	public String getNodeIdKey() {
		
		return nodeIdKey;
	}
	
	
	
	/**
	 * @return the availableSearchCriterias
	 */
	public String getAvailableSearchCriterias() {
		
		
		return availableSearchCriterias;
	}
	
	/**
	 * @return the searchable
	 */
	public boolean getSearchable() {
		
		
		if(searchable != null && searchable.trim().equals("true")){
			return true;
		}else{
			return false;
		}
		
	}
	
	/**
	 * @return the path
	 */
	public String getPath() {
		
		
		return path;
	}

	/**
	 * @return the metadatsets
	 */
	public String getMetadatsets() {
		
		return metadatsets;
	}
	/**
	 * @return the metadatsetsV2
	 */
	public String[] getMetadatsetsV2() {
		if(metadatsetsV2==null)
			return new String[]{"mds"};
		return metadatsetsV2.split(",");
	}

	/**
	 * @return the devmode
	 */
	public String getDevmode() {
		
		return devmode;
	}

	public String getWshotpath() {
		
		return wshotpath;
	}

	public String getAlfrescocontext() {
		
		return alfrescocontext;
	}

	public String getRecommend_objects_query() {
		
		return recommend_objects_query;
	}

	public String getTrustedEmailAddress() {
		
		return trustedEmailAddress;
	}
	

	public String getAuthByAppUsernameProp() {
		return authByAppUsernameProp;
	}

	public boolean isAuthByAppSendMail() {
		return authByAppSendMail;
	}
	
	
	/**
	 * allow the mapping from the username send as an authByApp param to an userDirectory Attribute
	 * the mapped value will be taken as the repositoryusername
	 * 
	 * in Application File: auth_by_app_usernamemapping_dir_username
	 * 
	 * to use this both auth_by_app_usernamemapping_rep_username and auth_by_app_usernamemapping_dir_username must be defined
	 * and the userdataService property must be configured at CCAuthMethodTrustedRepository
	 * 
	 * @return the property which is used as a key to ask the userDirectory
	 */
	public String getAuthByAppUsernameMappingDirectoryUsername() {
		return authByAppUsernameMappingDirectoryUsername;
	}

	/**
	 * allow the mapping from the username send as an authByApp param to an userDirectory Attribute
	 * the mapped value will be taken as the repositoryusername
	 * 
	 * in Application File: auth_by_app_usernamemapping_rep_username
	 * 
	 * to use this both auth_by_app_usernamemapping_rep_username and auth_by_app_usernamemapping_dir_username must be defined
	 * and the userdataService property must be configured at CCAuthMethodTrustedRepository
	 * 
	 * @return the userdirectory property which value is used as respository username 
	 */
	public String getAuthByAppUsernameMappingRepositoryUsername() {
		return authByAppUsernameMappingRepositoryUsername;
	}

	public String getAllowedAuthenticationTypes() {
		
		return allowedAuthenticationTypes;
		
		
	}

	public String getCustomCss() {
		return customCss;
	}

	public String getHostAliases() {
		return hostAliases;
	}
	
	/**
	 * checks it hostName is configured host or host alias
	 * @param hostName
	 * @return
	 */
	public boolean isTrustedHost(String hostName){
		List<String> hostList = new ArrayList<String>();
		hostList.add(host);
		if(this.getDomain() != null && !this.getDomain().equals("")){
			hostList.add(this.getDomain());
		}
		
		if(this.getHostAliases() != null && !this.getHostAliases().trim().equals("")){
			String[] splitted = this.getHostAliases().split(",");
			hostList.addAll(Arrays.asList(splitted));
		}
		
		return hostList.contains(hostName);
	}

	public String getProtocol() {
		return protocol;
	}

	/**
	 * or building client urls like preview url, if not set port value is returned
	 * @return
	 */
	public String getClientport() {
		String result = clientport;
		
		if(result != null && !result.trim().equals("")){
			return result;
		}else{
			result = getPort();
			if(result != null && !result.trim().equals("")){
				return result;
			}else{
				return "80";
			}
		}
	}
	
	/**
	 * or building client urls like preview url, if not set protocal value is returned
	 * @return
	 */
	public String getClientprotocol() {
		String result = clientprotocol;
		if(result != null && !result.trim().equals("")){
			return result;
		}else{
			result = getProtocol();
			if(result != null && !result.trim().equals("")){
				return result;
			}else{
				return "http";
			}
		}
		
	}

	public String getWebappname() {
		return webappname;
	}
	
	public int getTrackingBufferSize() {
		return trackingBufferSize;
	}
	
	public String getPublicKey() {
		return publicKey;
	}
	
	public String getPrivateKey() {
		return privateKey;
	}
	
	public long getMessageOffsetMs() {
		if(messageOffsetMs!=null && !messageOffsetMs.isEmpty())
			return new Long(messageOffsetMs);
		return DEFAULT_OFFSET_MS;
	}
	
	public long getMessageSendOffsetMs() {
		if(messageSendOffsetMs!=null && !messageSendOffsetMs.isEmpty())
			return new Long(messageSendOffsetMs);
		return DEFAULT_OFFSET_MS;
	}
	
	public String getLogo() {
		return logo;
	}
	
	public String getIcon() {
		return icon;
	}

	public String getApiKey() {
		return apiKey;
	}
	
	public String getWebsitepreviewrenderservice() {
		return websitepreviewrenderservice;
	}	
	
	@Override
	public int compareTo(ApplicationInfo o) {
		return Integer.compare(getOrder(), o.getOrder());
	}
}
