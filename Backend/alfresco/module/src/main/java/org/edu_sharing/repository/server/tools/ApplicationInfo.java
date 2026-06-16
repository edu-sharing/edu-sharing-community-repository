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

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.alfresco.policy.NodeCustomizationPolicies;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.repository.server.RequestHelper;

import java.io.Serializable;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class ApplicationInfo implements Comparable<ApplicationInfo>, Serializable {

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

    public static final String KEY_SUBTYPE = "subtype";

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

    public static final String KEY_REMOTE_PROVIDER = "remote_provider";

    public static final String KEY_AUTHENTICATIONTOOLCLASS = "authenticationtoolclass";

    public static final String KEY_CONTENTURL = "contenturl";

    public static final String KEY_URL_DYNAMIC = "url_dynamic";

    public static final String KEY_PREVIEWURL = "previewurl";

    public static final String KEY_IS_HOME_NODE = "is_home_node";

    public static final String KEY_CUSTOM_HTML_HEADERS = "custom_html_headers";

    public static final String KEY_METADATASETS_V2 = "metadatasetsV2";

    // metadataset used when an element from a remote repository is copied into the local one
    // if unset, the main metadataset (KEY_METADATASETS_V2) from the remote repo will be used
    public static final String KEY_IMPORT_METADATASET = "import_metadataset";

    public static final String KEY_PUBLIC_KEY = "public_key";
    public static final String KEY_PRIVATE_KEY = "private_key";

    public static final String KEY_CERTIFICATE = "certificate";

    public static final String KEY_SIGNATURE_ALGORITHM = "signature_algorithm";

    public static final String KEY_KEYSTORE_PW = "keystore_pw";

    public static final String KEY_MESSAGE_OFFSET_MILLISECONDS = "message_offset_ms";
    public static final String KEY_MESSAGE_SEND_OFFSET_MILLISECONDS = "message_send_offset_ms";

    public static final String KEY_LOGOUT_URL = "logouturl";

    public static final String KEY_API_KEY = "apikey";
    public static final String KEY_API_URL = "apiurl";

    public static final String KEY_ORDER = "order";

    public static final String KEY_ALLOW_ORIGIN = "allow_origin";

    public static final String KEY_HOST_ALLOW_INTERNAL_IP = "host_allow_internal_ip";

    public static final String KEY_COOKIE_ATTRIBUTES = "cookie_attributes";

    public static final String KEY_LTI_ISS = "lti_iss";

    public static final String KEY_LTI_CLIENT_ID = "lti_client_id";

    public static final String KEY_LTI_DEPLOYMENT_ID = "lti_deployment_id";

    public static final String KEY_LTI_OIDC_ENDPOINT = "lti_oidc_endpoint";

    public static final String KEY_LTI_AUTH_TOKEN_ENDPOINT = "lti_auth_token_endpoint";

    public static final String KEY_LTI_KEYSET_URL = "lti_keyset_url";

    public static final String KEY_LTI_KID = "lti_kid";

    public static final String KEY_LTI_USAGES_ENABLED = "lti_usages_enabled";

    public static final String KEY_LTI_SYNCREADERS = "lti_sync_readers";

    public static final String KEY_LTI_RESOURCE_TYPE = "lti_resource_type";

    public static final String KEY_LTITOOL_LOGININITIATIONS_URL = "ltitool_initiate_login_uri";

    public static final String KEY_LTITOOL_REDIRECT_URLS = "ltitool_redirect_urls";

    public static final String KEY_LTITOOL_TARGET_LINK_URI = "ltitool_target_link_uri";


    public static final String KEY_LTITOOL_TARGET_LINK_URI_DEEPLINK = "ltitool_target_link_uri_deeplink";

    public static final String KEY_LTITOOL_CUSTOM_PARAMETERS = "ltitool_custom_parameters";

    public static final String KEY_LTITOOL_DESCRIPTION = "ltitool_description";

    //custom allow too to write content to edu-sharing
    public static final String KEY_LTITOOL_CUSTOMCONTENT_OPTION = "ltitool_customcontent_option";

    //used to identify applications by resourcelinks
    public static final String KEY_LTITOOL_URL = "ltitool_url";

    public static final String KEY_LTI_SCOPEUSERNAME = "lti_scope_username";


    /**
     * property file vals
     */
    public static final String TYPE_REPOSITORY = "REPOSITORY";
    public static final String TYPE_LMS = "LMS";
    public static final String TYPE_CMS = "CMS";
    public static final String TYPE_LTIPLATFORM = "lti";
    public static final String TYPE_LTITOOL = "ltitool";


    /**
     * SERVICE = renderservice -> reserved, no other system can use this
     */
    public static final String TYPE_RENDERSERVICE = "SERVICE";

    /**
     * kotlin based rendering service
     */
    public static final String TYPE_RENDERSERVICE_2 = "RENDERINGSERVICE_2";

    /**
     * LEARNING_LOCKER = reserved for a learning locker system
     */
    public static final String TYPE_LEARNING_LOCKER = "LEARNING_LOCKER";

    public static final String TYPE_CONNECTOR = "CONNECTOR";

    /**
     * ROCKETCHAT = reserved for a rocketchat system
     */
    public static final String TYPE_ROCKETCHAT = "ROCKETCHAT";


    public static final String REPOSITORY_TYPE_ALFRESCO = "ALFRESCO"; // a remote alfresco repository

    public static final String REPOSITORY_TYPE_LOCAL = "LOCAL"; // the local alfreso repository

    public static final String REPOSITORY_TYPE_EDUNEX = "EDUNEX";

    public static final String REPOSITORY_TYPE_YOUTUBE = "YOUTUBE";

    public static final String REPOSITORY_TYPE_DDB = "DDB";

    public static final String REPOSITORY_TYPE_WIKIMEDIA = "WIKIMEDIA";

    public static final String AUTHBYAPP_USERNAME_PROP_MAIL = "MAIL";

    public static final String AUTHBYAPP_USERNAME_PROP_USERNAME = "USERNAME";

    public static final String AUTHBYAPP_USER_WHITELIST = "auth_by_app_user_whitelist";

    @Deprecated
    public static final String WEBSITEPREVIEWRENDERSERVICE = "websitepreviewrenderservice";

    public static final String NOTIFY_FETCH_LIMIT = "notify_fetch_limit";

    public static final String REPOSITORY_TYPE_MEMUCHO = "MEMUCHO";

    /**
     * Remote sso userid mapping. Only for remote alfresco repos
     * If it is not set, the one from the edu-sharing-sso-context.xml will be used
     *
     * @TODO make the sso config not influcene remote logins!
     */
    public static final String REMOTE_USERID = "remote_userid";
    public static final String FORCED_USER = "forced_user";

    public static final String PROPERTY_VALIDATOR_REGEX_CM_NAME = "property_validator_regex_cm_name";

    private final Properties properties;
    /**
     * -- GETTER --
     *
     * @return only the file name
     */
    @Getter
    private final String appFileName;
    @Getter
    private final String ltiResourceType;

    @Getter
    private String host;

    /**
     * an , separated List of Hosts, maybe ip ranges in future
     */
    @Getter
    private String hostAliases;

    /**
     * if true, internal/private ip addresses (e.g. loopback, site-local, k8s) are treated as trusted hosts
     */
    @Getter
    private boolean hostAllowInternalIp = false;

    /**
     * used for previewurls or content urls, usefull when we got an proxy
     */
    private final String domain;
    @Getter
    private final String alfrescocontext;
    @Getter
    private String port;
    @Getter
    private String protocol;
    private final String clientport;
    private final String clientprotocol;
    @Getter
    private final String wspath;
    private final String wshotpath;
    @Getter
    private String webappname;
    /**
     * -- GETTER --
     *
     * @return the path
     */
    @Getter
    private final String path;
    /**
     * -- GETTER --
     *
     * @return the full app file path
     */
    @Getter
    private final String appFile;
    private String ishomeNode;
    @Getter
    private final String username;
    @Getter
    private final String password;
    @Getter
    private final String logo;
    @Getter
    private final String icon;
    @Getter
    private final String appCaption;
    @Getter
    private final String appId;
    @Getter
    private final String trustedclient;

    @Getter
    private final String type;
    private final String authenticationwebservice;
    @Getter
    private final String permissionwebservice;
    @Getter
    private final String subtype;
    /**
     * -- GETTER --
     *
     * @return the repositoryType
     */
    @Getter
    private final String repositoryType;

    /**
     * this is a property used to redirect to content deliverd by repositories renderservice
     */
    private final String contentUrl;

    /**
     * this is a property used redirect to preview deliverd by repositories renderservice
     * -- GETTER --
     * this is a property used redirect to preview deliverd by repositories renderservice
     *
     * @return the previewUrl
     */
    @Getter
    private final String previewUrl;
    @Getter
    private final String customHtmlHeaders;
    @Getter
    private String logoutUrl;
    private final String websitepreviewrenderservice;
    private String searchable;

    //file that contains metadatasets for the repository
    private final String metadatsets;
    /**
     * -- GETTER --
     *
     * @return the devmode
     */
    //devmode metadatasets will be parsed every time and not cached in RepoFactory
    @Getter
    private String devmode;

    //recommend objects search
    @Getter
    private final String recommend_objects_query;

    //for lms: if this mail is configured and the user authenticates with this mail than we authenticate without sending an confirmation mail
    @Getter
    private final String trustedEmailAddress;

    //allow to switch from email as username to username like the app. the static vars AUTHBYAPP_USERNAME_PROP_MAIL, AUTHBYAPP_USERNAME_PROP_USERNAME are valid values
    @Getter
    private String authByAppUsernameProp;

    //allow login from applications without sending an confirmation mail
    @Getter
    private final boolean authByAppSendMail;
    @Getter
    @Setter
    private final String authByAppUserWhitelist;
    @Getter
    private final String publicKey;
    @Getter
    private final String privateKey;
    @Getter
    private final String certificate;
    @Getter
    private final String signatureAlgorithm;
    private final String messageOffsetMs;
    private final String messageSendOffsetMs;


    /**
     * allow the mapping from the username send as an authByApp param to an userDirectory Attribute
     * the mapped value will be taken as the repositoryusername
     * <p>
     * authByAppUsernameMappingDirectoryUsername: the property which is used as a key to ask the userDirectory
     * <p>
     * auth_by_app_usernamemapping_dir_username
     * <p>
     * authByAppUsernameMappingRepositoryUsername: the userdirectory property which value is used as respository username
     * <p>
     * auth_by_app_usernamemapping_rep_username
     * -- GETTER --
     *  allow the mapping from the username send as an authByApp param to an userDirectory Attribute
     *  the mapped value will be taken as the repositoryusername
     *  <p>
     *  in Application File: auth_by_app_usernamemapping_dir_username
     *  <p>
     *  to use this both auth_by_app_usernamemapping_rep_username and auth_by_app_usernamemapping_dir_username must be defined
     *  and the userdataService property must be configured at CCAuthMethodTrustedRepository
     *
     * @return the property which is used as a key to ask the userDirectory

     */
    @Getter
    private final String authByAppUsernameMappingDirectoryUsername;
    /**
     * -- GETTER --
     *  allow the mapping from the username send as an authByApp param to an userDirectory Attribute
     *  the mapped value will be taken as the repositoryusername
     *  <p>
     *  in Application File: auth_by_app_usernamemapping_rep_username
     *  <p>
     *  to use this both auth_by_app_usernamemapping_rep_username and auth_by_app_usernamemapping_dir_username must be defined
     *  and the userdataService property must be configured at CCAuthMethodTrustedRepository
     *
     * @return the userdirectory property which value is used as respository username
     */
    @Getter
    private final String authByAppUsernameMappingRepositoryUsername;

    //for lms or other connected systems: if they call the gwt app with the css_appid param then the custom css file is included in the html
    @Getter
    private final String customCss;
    @Getter
    private final int trackingBufferSize;
    @Getter
    private final String apiKey;
    @Getter
    private final int order;
    @Setter
    @Getter
    private final String xml;
    @Getter
    private final String ltiOidc;
    @Getter
    private final String ltiAuthTokenEndpoint;
    @Getter
    private final String ltiClientId;
    @Getter
    private final String ltiDeploymentId;
    @Getter
    private final String ltiIss;
    @Getter
    private final String ltiKeysetUrl;
    @Getter
    private final String ltiKid;
    private final String ltiUsagesEnabled;
    private final String ltiSyncReaders;
    @Getter
    private final String ltitoolLoginInitiationsUrl;
    @Getter
    private final String ltitoolRedirectUrls;
    private final String ltitoolTargetLinkUri;
    @Getter
    private final String ltitoolTargetLinkUriDeepLink;
    @Getter
    private final String ltitoolCustomParameters;
    @Getter
    private final String ltitoolDescription;
    private final String ltitoolCustomContentOption;
    @Getter
    private final String ltitoolUrl;
    @Getter
    private final boolean ltiScopeUsername;


    /**
     * der Anfangsteil des alfresco Intergity Pattern:
     * (.*[\"\*\\\>\<\?\/\:\|]+.*)|(.*[\.]?.*[\.]+$)|(.*[ ]+$)
     * so das nur die kritischen Zeichen matchen und nicht der ganze string
     */
    //default value ([\"\*\\\\\>\<\?\/\:\|'\r\n])
    @Getter
    private String validatorRegexCMName = "([\\\"\\*\\\\\\\\\\>\\<\\?\\/\\:\\|'\\r\\n])";
    @Getter
    private final String cookieAttributes;
    @Getter
    private final Map<CacheKey, Serializable> cache = new HashMap<>();
    @Getter
    private final String keyStorePassword;

    public ApplicationInfo(String _appFile) throws Exception {
        if (_appFile == null) throw new Exception("Application Filename was null!");
        appFileName = _appFile;
        appFile = PropertiesHelper.Config.getPropertyFilePath(_appFile);

        ClassLoader classLoader = Thread.currentThread()
                .getContextClassLoader();
        URL url = classLoader.getResource(appFile);
        xml = url != null ? new String(Files.readAllBytes(Paths.get(url.toURI()))) : null;

        //test if file exists if not exception is thrown
        //properties.getProperty("is_home_node");
        properties = PropertiesHelper.getProperties(appFile, PropertiesHelper.XML);

        host = properties.getProperty(KEY_HOST);

        hostAliases = properties.getProperty("host_aliases");

        hostAllowInternalIp = Boolean.parseBoolean(properties.getProperty(KEY_HOST_ALLOW_INTERNAL_IP));

        domain = properties.getProperty(KEY_DOMAIN);

        port = properties.getProperty(KEY_PORT);
        if (StringUtils.isBlank(port)) {
            port = "80";
        }

        protocol = properties.getProperty(KEY_PROTOCOL);
        if (StringUtils.isBlank(protocol)) {
            protocol = "http";
        }

        clientport = properties.getProperty(KEY_CLIENTPORT);
        clientprotocol = properties.getProperty(KEY_CLIENTPROTOCOL);

        wspath = properties.getProperty(KEY_WSPATH);
        ishomeNode = properties.getProperty(KEY_IS_HOME_NODE);
        if (ishomeNode == null) {
            ishomeNode = "false";
        }
        wshotpath = properties.getProperty(KEY_WSHOTPATH);

        webappname = properties.getProperty(KEY_WEBAPPNAME);
        if (webappname == null) webappname = "edu-sharing";

        username = properties.getProperty(KEY_USERNAME);

        password = properties.getProperty(KEY_PASSWORD);

        logo = properties.getProperty(KEY_LOGO);

        icon = properties.getProperty(KEY_ICON);

        appCaption = properties.getProperty(KEY_APPCAPTION);

        appId = properties.getProperty(KEY_APPID);

        trustedclient = properties.getProperty(KEY_TRUSTEDCLIENT);

        type = properties.getProperty(KEY_TYPE);

        authenticationwebservice = properties.getProperty("authenticationwebservice");

        permissionwebservice = properties.getProperty("permissionwebservice");

        subtype = properties.getProperty(KEY_SUBTYPE);

        repositoryType = properties.getProperty(KEY_REPOSITORY_TYPE);

        contentUrl = properties.getProperty(KEY_CONTENTURL);

        previewUrl = properties.getProperty(KEY_PREVIEWURL);

        customHtmlHeaders = properties.getProperty(KEY_CUSTOM_HTML_HEADERS);

        logoutUrl = properties.getProperty(KEY_LOGOUT_URL);
        if (logoutUrl == null) {
            logoutUrl = "/edu-sharing/logout";
        }

        searchable = properties.getProperty(KEY_SEARCHABLE);

        if (searchable == null) searchable = "true";

        path = properties.getProperty("path");

        metadatsets = properties.getProperty(KEY_METADATASETS_V2);

        devmode = properties.getProperty("devmode");

        devmode = (devmode == null) ? "false" : devmode;

        alfrescocontext = properties.getProperty(KEY_ALFRESCOCONTEXT);

        recommend_objects_query = properties.getProperty("recommend_objects_query");

        trustedEmailAddress = properties.getProperty("trusted_emailaddress");

        authByAppUsernameProp = properties.getProperty("auth_by_app_username_prop");
        authByAppUsernameProp = (authByAppUsernameProp == null) ? ApplicationInfo.AUTHBYAPP_USERNAME_PROP_USERNAME : authByAppUsernameProp;

        String tmpAuthByAppSendmail = properties.getProperty("auth_by_app_sendmail");
        authByAppSendMail = tmpAuthByAppSendmail == null || Boolean.parseBoolean(tmpAuthByAppSendmail);

        authByAppUsernameMappingDirectoryUsername = properties.getProperty("auth_by_app_usernamemapping_dir_username");

        authByAppUsernameMappingRepositoryUsername = properties.getProperty("auth_by_app_usernamemapping_rep_username");

        authByAppUserWhitelist = properties.getProperty(AUTHBYAPP_USER_WHITELIST);

        customCss = properties.getProperty("custom_css");

        String tmpTrackingBufferSize = properties.getProperty("trackingBufferSize");
        trackingBufferSize = (tmpTrackingBufferSize != null ? Integer.parseInt(tmpTrackingBufferSize) : 0);

        publicKey = properties.getProperty(KEY_PUBLIC_KEY);

        privateKey = properties.getProperty(KEY_PRIVATE_KEY);

        certificate = properties.getProperty(KEY_CERTIFICATE);

        signatureAlgorithm = properties.getProperty(KEY_SIGNATURE_ALGORITHM);

        messageOffsetMs = properties.getProperty(KEY_MESSAGE_OFFSET_MILLISECONDS);

        messageSendOffsetMs = properties.getProperty(KEY_MESSAGE_SEND_OFFSET_MILLISECONDS);

        apiKey = properties.getProperty(KEY_API_KEY);

        websitepreviewrenderservice = properties.getProperty(WEBSITEPREVIEWRENDERSERVICE);

        String orderString = properties.getProperty(KEY_ORDER);
        order = orderString == null ? (ishomeNode() ? 0 : 1) : Integer.parseInt(orderString);

        cookieAttributes = properties.getProperty(KEY_COOKIE_ATTRIBUTES);

        getWebServiceUrl();
        getWebServerUrl();

        String cmNameRegex = properties.getProperty(PROPERTY_VALIDATOR_REGEX_CM_NAME);
        if (StringUtils.isNotBlank(cmNameRegex)) {
            validatorRegexCMName = cmNameRegex;
        }

        keyStorePassword = properties.getProperty(KEY_KEYSTORE_PW);

        ltiClientId = properties.getProperty(KEY_LTI_CLIENT_ID);
        ltiIss = properties.getProperty(KEY_LTI_ISS);
        ltiDeploymentId = properties.getProperty(KEY_LTI_DEPLOYMENT_ID);
        ltiOidc = properties.getProperty(KEY_LTI_OIDC_ENDPOINT);
        ltiAuthTokenEndpoint = properties.getProperty(KEY_LTI_AUTH_TOKEN_ENDPOINT);
        ltiKeysetUrl = properties.getProperty(KEY_LTI_KEYSET_URL);
        ltiKid = properties.getProperty(KEY_LTI_KID);
        ltiUsagesEnabled = properties.getProperty(KEY_LTI_USAGES_ENABLED);
        ltiSyncReaders = properties.getProperty(KEY_LTI_SYNCREADERS);
        ltiResourceType = properties.getProperty(KEY_LTI_RESOURCE_TYPE);

        ltitoolRedirectUrls = properties.getProperty(KEY_LTITOOL_REDIRECT_URLS);
        ltitoolLoginInitiationsUrl = properties.getProperty(KEY_LTITOOL_LOGININITIATIONS_URL);
        ltitoolTargetLinkUri = properties.getProperty(KEY_LTITOOL_TARGET_LINK_URI);
        ltitoolTargetLinkUriDeepLink = properties.getProperty(KEY_LTITOOL_TARGET_LINK_URI_DEEPLINK);
        ltitoolCustomParameters = properties.getProperty(KEY_LTITOOL_CUSTOM_PARAMETERS);
        ltitoolDescription = properties.getProperty(KEY_LTITOOL_DESCRIPTION);
        ltitoolCustomContentOption = properties.getProperty(KEY_LTITOOL_CUSTOMCONTENT_OPTION);
        ltitoolUrl = properties.getProperty(KEY_LTITOOL_URL);
        ltiScopeUsername = properties.getProperty(KEY_LTI_SCOPEUSERNAME) == null || Boolean.parseBoolean(properties.getProperty(KEY_LTI_SCOPEUSERNAME));
    }

    public String getDomain() {

        if (domain == null) {
            return host;
        }

        return domain;
    }

    public String getBaseUrl() {
        String port = "";
        if (!this.getPort().equals("80") && !this.getPort().equals("443")) {
            port = ":" + this.getPort();
        }

        return this.getProtocol() + "://" + this.getHost() + port;
    }

    public void getWebServiceUrl() {
        this.getBaseUrl();
    }

    public String getWebServiceHotUrl() {
        return getWebServiceHotUrl(false);
    }

    public String getWebServiceHotUrl(boolean external) {
        String port = "";
        if (external) {
            if (!this.getClientport().equals("80") && !this.getClientport().equals("443")) {
                port = ":" + this.getClientport();
            }
            return this.getClientprotocol() + "://" + this.getDomain() + port + this.getWshotpath();
        } else {
            if (!this.getPort().equals("80") && !this.getPort().equals("443")) {
                port = ":" + this.getPort();
            }
            return this.getProtocol() + "://" + this.getHost() + port + this.getWshotpath();
        }
    }

    public String getString(String key, String defaultValue) {
        return replaceDynamicVariables(properties.getProperty(key, defaultValue));
    }

    private String replaceDynamicVariables(String data) {
        if (data == null) return data;

        String contextDomain = (Context.getCurrentInstance() != null && Context.getCurrentInstance().getRequest() != null)
                ? new RequestHelper(Context.getCurrentInstance().getRequest()).getServerName()
                : null;

        String rootDomain = DomainUtils.getRootDomain(contextDomain);
        Map<String, String> searchReplace = new HashMap<>();
        searchReplace.put("${context.id}", NodeCustomizationPolicies.getEduSharingContext());
        searchReplace.put("${context.domain}", contextDomain);
        searchReplace.put("${context.rootDomain}", rootDomain);
        for (Map.Entry<String, String> entry : searchReplace.entrySet()) {
            data = data.replace(entry.getKey(), entry.getValue() == null ? "" : entry.getValue());
        }
        return data;
    }

    public int getInteger(String key, int defaultValue) {
        String property = properties.getProperty(key);
        if (property == null || property.isEmpty())
            return defaultValue;
        return Integer.parseInt(property);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String property = properties.getProperty(key);
        if (property == null || property.isEmpty())
            return defaultValue;
        return property.equalsIgnoreCase("true");
    }

    public String getWebServerUrl() {
        String port = "";
        if (!this.getPort().equals("80") && !this.getPort().equals("443")) {
            port = ":" + this.getPort();
        }

        return this.getProtocol() + "://" + this.getHost() + port;
    }

    public String getClientBaseUrl() {
        String result = this.getClientprotocol() + "://" + ((this.getDomain() == null) ? this.getHost() : this.getDomain());
        if (this.getClientport().equals("80") || this.getClientport().equals("443")) {
            result += "/" + getWebappname();
        } else {
            result += ":" + this.getClientport() + "/" + getWebappname();
        }
        return result;
    }

    public boolean ishomeNode() {
        return ishomeNode.equals("true");
    }


    public String getAuthenticationwebservice() {
        if (authenticationwebservice == null || authenticationwebservice.isEmpty())
            return getWebServiceHotUrl() + "/authentication";

        return authenticationwebservice;
    }


    /**
     * this is a property used redirect to content deliverd by repositories renderservice
     *
     * @return the renderServiceUrl
     */
    public String getContentUrl() {
        return replaceDynamicVariables(contentUrl);
    }

    /**
     * @return the searchable
     */
    public boolean getSearchable() {
        return searchable != null && searchable.trim().equals("true");
    }

    /**
     * @return the metadatsetsV2
     */
    public String[] getMetadatsets() {
        if (metadatsets == null)
            return new String[]{"mds"};
        return metadatsets.split(",");
    }

    public String getWshotpath() {
        if (wshotpath == null || wshotpath.isEmpty()) {
            return "/edu-sharing/services/";
        }
        return wshotpath;
    }

    /**
     * checks it hostName is configured host or host alias
     *
     */
    public boolean isTrustedHost(String hostName) {
        List<String> hostList = new ArrayList<>();
        hostList.add(host);
        if (StringUtils.isNotBlank(this.getDomain())) {
            hostList.add(this.getDomain());
        }

        if (StringUtils.isNotBlank(this.getHostAliases())) {
            String[] splitted = this.getHostAliases().split(",");
            // add all and trim to fix stuff like "ip1, ip2"
            hostList.addAll(Arrays.stream(splitted).map(String::trim).collect(Collectors.toList()));
        }

        if (hostList.contains("*") || hostList.contains(hostName)) {
            return true;
        }

        return this.isHostAllowInternalIp() && isInternalIp(hostName);
    }

    /**
     * checks if the given hostName resolves to an internal/private ip address
     * (loopback, site-local, link-local, wildcard or IPv6 unique-local fc00::/7)
     *
     * @param hostName host name or ip literal
     * @return true if the address is considered internal/private
     */
    private static boolean isInternalIp(String hostName) {
        if (hostName == null || hostName.trim().isEmpty()) {
            return false;
        }
        try {
            java.net.InetAddress address = java.net.InetAddress.getByName(hostName);
            return address.isLoopbackAddress()
                    || address.isSiteLocalAddress()
                    || address.isLinkLocalAddress()
                    || address.isAnyLocalAddress();
        } catch (java.net.UnknownHostException e) {
            return false;
        }
    }

    /**
     * or building client urls like preview url, if not set port value is returned
     *
     */
    public String getClientport() {
        String result = clientport;

        if (StringUtils.isNotBlank(result)) {
            return result;
        } else {
            result = getPort();
            if (StringUtils.isNotBlank(result)) {
                return result;
            } else {
                return "80";
            }
        }
    }

    /**
     * or building client urls like preview url, if not set protocal value is returned
     *
     */
    public String getClientprotocol() {
        String result = clientprotocol;
        if (StringUtils.isNotBlank(result)) {
            return result;
        } else {
            result = getProtocol();
            if (StringUtils.isNotBlank(result)) {
                return result;
            } else {
                return "http";
            }
        }

    }

    public long getMessageOffsetMs() {
        if (messageOffsetMs != null && !messageOffsetMs.isEmpty())
            return Long.parseLong(messageOffsetMs);
        return DEFAULT_OFFSET_MS;
    }

    public long getMessageSendOffsetMs() {
        if (messageSendOffsetMs != null && !messageSendOffsetMs.isEmpty())
            return Long.parseLong(messageSendOffsetMs);
        return DEFAULT_OFFSET_MS;
    }

    @Deprecated
    public String getWebsitepreviewrenderservice() {
        return websitepreviewrenderservice;
    }

    @Override
    public int compareTo(ApplicationInfo o) {
        return Integer.compare(getOrder(), o.getOrder());
    }

    /**
     * returns true if the given app is describing a remote alfresco (edu-sharing) instance
     */
    public boolean isRemoteAlfresco() {
        return REPOSITORY_TYPE_ALFRESCO.equals(getRepositoryType()) && TYPE_REPOSITORY.equals(getType());
    }

    public enum CacheKey {
        RemoteAlfrescoVersion
    }

    public boolean isLtiUsagesEnabled() {
        if (this.ltiUsagesEnabled != null) {
            return Boolean.parseBoolean(this.ltiUsagesEnabled);
        }
        return true;
    }

    public boolean isLtiSyncReaders() {
        if (this.ltiSyncReaders == null) {
            return false;
        } else return Boolean.parseBoolean(this.ltiSyncReaders);
    }

    public String getLtitoolTargetLinkUri() {
        return ltitoolTargetLinkUri == null ? ltitoolTargetLinkUriDeepLink : ltitoolTargetLinkUri;
    }

    public boolean isLtiTool() {
        return ltitoolLoginInitiationsUrl != null && ltitoolRedirectUrls != null;
    }

    public boolean isLtiPlatform() {
        return TYPE_LTIPLATFORM.equals(getType());
    }

    public boolean hasLtiToolCustomContentOption() {
        return Boolean.parseBoolean(ltitoolCustomContentOption);
    }
}
