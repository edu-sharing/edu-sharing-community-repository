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
     * Authentication response
     */
    public static final String LTI_MESSAGE_TYPE = "https://purl.imsglobal.org/spec/lti/claim/message_type";
    public static final String DEEP_LINKING_SETTINGS = "https://purl.imsglobal.org/spec/lti-dl/claim/deep_linking_settings";
    public static final String DEEP_LINK_RETURN_URL = "deep_link_return_url";


    /**
     * user information
     */
    public static final String LTI_NAME = "name";
    public static final String LTI_GIVEN_NAME = "given_name";
    public static final String LTI_FAMILY_NAME = "family_name";
    public static final String LTI_MIDDLE_NAME = "middle_name";
    public static final String LTI_EMAIL = "email";

}
