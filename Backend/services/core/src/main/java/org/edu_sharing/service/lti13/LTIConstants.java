package org.edu_sharing.service.lti13;

public class LTIConstants {
    public static final String LTI3ERROR = "lti3Error";
    public static final String ERROR = "Error";
    public static final String ERROR_DEPLOYMENT_NOT_FOUND = "no tool deployment found for ";

    public static final String LTI_PARAM_ISS = "iss";
    public static final String LTI_PARAM_TARGET_LINK_URI = "target_link_uri";
    public static final String LTI_PARAM_CLIENT_ID = "client_id";
    public static final String LTI_PARAM_LOGIN_HINT = "login_hint";
    public static final String LTI_PARAM_MESSAGE_HINT = "lti_message_hint";
    public static final String LTI_PARAM_DEPLOYMENT_ID = "lti_deployment_id";
    public static final String NONE = "none";


    public static final String LTI_TOOL_REDIRECTURL_PATH = "lti13";

    public static final String LTI_TOOL_SESS_ATT_NONCE = "lti_nonce";
    public static final String LTI_TOOL_SESS_ATT_STATE = "lti_state";


    public static final String FORM_POST = "form_post";
    public static final String ID_TOKEN = "id_token";
    public static final String OPEN_ID = "openid";


    public static final String MOODLE_AUTHENTICATION_REQUEST_URL_PATH = "/mod/lti/auth.php";
    public static final String MOODLE_KEYSET_URL_PATH = "/mod/lti/certs.php";
    public static final String MOODLE_AUTH_TOKEN_URL_PATH = "/mod/lti/token.php";

    /**
     * LTI State
     */
    public static final String LTI_STATE_DEPLOYMENT_ID = "ltiDeploymentId";


    /**
     * LTI Messages
     */
    public static final String LTI_MESSAGE_TYPE = "https://purl.imsglobal.org/spec/lti/claim/message_type";
    public static final String LTI_MESSAGE_TYPE_DEEP_LINKING_RESPONSE = "LtiDeepLinkingResponse";
    public static final String LTI_MESSAGE_TYPE_RESOURCE_LINK = "LtiResourceLinkRequest";
    public static final String LTI_MESSAGE_TYPE_DEEP_LINKING = "LtiDeepLinkingRequest";

    public static final String LTI_VERSION = "https://purl.imsglobal.org/spec/lti/claim/version";
    public static final String LTI_DEPLOYMENT_ID = "https://purl.imsglobal.org/spec/lti/claim/deployment_id";
    public static final String LTI_DATA = "https://purl.imsglobal.org/spec/lti-dl/claim/data";
    public static final String LTI_CONTENT_ITEMS = "https://purl.imsglobal.org/spec/lti-dl/claim/content_items";

    public static final String LTI_VERSION_3 = "1.3.0";
    public static final String LTI_TARGET_LINK_URI = "https://purl.imsglobal.org/spec/lti/claim/target_link_uri";

    public static final String DEEP_LINKING_SETTINGS = "https://purl.imsglobal.org/spec/lti-dl/claim/deep_linking_settings";

    public static final String LTI_CLAIM_RESOURCE_LINK = "https://purl.imsglobal.org/spec/lti/claim/resource_link";
    public static final String DEEP_LINK_RETURN_URL = "deep_link_return_url";
    public static final String DEEP_LINK_DATA = "data";
    public static final String DEEP_LINK_ACCEPT_TYPES = "accept_types";
    public static final String DEEP_LINK_ACCEPT_MEDIA_TYPES = "accept_media_types";
    public static final String DEEP_LINK_DOCUMENT_TARGETS = "accept_presentation_document_targets";
    public static final String DEEP_LINK_ACCEPT_MULTIPLE = "accept_multiple";
    public static final String DEEP_LINK_AUTO_CREATE = "auto_create";
    public static final String DEEP_LINK_CAN_CONFIRM = "can_confirm";
    public static final String DEEP_LINK_TITLE = "title";
    public static final String DEEP_LINK_TEXT = "text";
    public static final String DEEP_LINK_LTIRESOURCELINK = "ltiResourceLink";
    public static final String DEEP_LINK_TYPE = "type";
    public static final String DEEP_LINK_URL = "url";

    public static final String DEEP_LINK_CONTEXT = "https://purl.imsglobal.org/spec/lti/claim/context";

    public static final String LTI_LAUNCH_PRESENTATION = "https://purl.imsglobal.org/spec/lti/claim/launch_presentation";

    public static final String LTI_REGISTRATION_TOOL_CONFIGURATION = "https://purl.imsglobal.org/spec/lti-tool-configuration";

    public static final String LTI_TOOL_PLATFORM = "https://purl.imsglobal.org/spec/lti/claim/tool_platform";

    public static final String LTI_REGISTRATION_SCOPE_NEW = "NEW";

    public static final String LTI_REGISTRATION_SCOPE_UPDATE = "UPDATE";



    /**
     * user information
     */
    public static final String LTI_NAME = "name";
    public static final String LTI_GIVEN_NAME = "given_name";
    public static final String LTI_FAMILY_NAME = "family_name";
    public static final String LTI_MIDDLE_NAME = "middle_name";
    public static final String LTI_EMAIL = "email";
    public static final String LTI_AZP = "azp";


    /**
     *
     */
    public static final String KID = "kid";
    public static final String ALG = "alg";
    public static final String TYP = "typ";
    public static final String JWT = "JWT";
    public static final String RS256 = "RS256";

    public static final String LTI_NONCE = "nonce";

}
