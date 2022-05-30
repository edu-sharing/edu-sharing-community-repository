package org.edu_sharing.service.lti13;

import com.google.common.hash.Hashing;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.UrlTool;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.security.Signing;
import org.edu_sharing.service.lti13.model.LoginInitiationDTO;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LTIOidcUtil {

    Logger logger = Logger.getLogger(LTIOidcUtil.class);

    /**
     * This generates a map with all the information that we need to send to the OIDC Authorization endpoint in the Platform.
     * In this case, we will put this in the model to be used by the thymeleaf template.
     */
    public Map<String, String> generateAuthRequestPayload(ApplicationInfo platformDeployment, LoginInitiationDTO loginInitiationDTO) throws GeneralSecurityException, IOException {

        Map<String, String> authRequestMap = new HashMap<>();
        authRequestMap.put(LTIConstants.LTI_PARAM_CLIENT_ID, platformDeployment.getLtiClientId()); //As it came from the Platform (if it came... if not we should have it configured)
        authRequestMap.put(LTIConstants.LTI_PARAM_LOGIN_HINT, loginInitiationDTO.getLoginHint()); //As it came from the Platform
        authRequestMap.put(LTIConstants.LTI_PARAM_MESSAGE_HINT, loginInitiationDTO.getLtiMessageHint()); //As it came from the Platform
        String nonce = UUID.randomUUID().toString(); // We generate a nonce to allow this auth request to be used only one time.
        String nonceHash = Hashing.sha256()
                .hashString(nonce, StandardCharsets.UTF_8)
                .toString();
        authRequestMap.put("nonce", nonce);  //The nonce
        authRequestMap.put("nonce_hash", nonceHash);  //The hash value of the nonce
        authRequestMap.put("prompt", LTIConstants.NONE);  //Always this value, as specified in the standard.
        authRequestMap.put("redirect_uri", ApplicationInfoList.getHomeRepository().getClientBaseUrl()+"/rest/lti/v13/" + LTIConstants.LTI_TOOL_REDIRECTURL_PATH);  // One of the valids reditect uris.
        authRequestMap.put("response_mode", LTIConstants.FORM_POST); //Always this value, as specified in the standard.
        authRequestMap.put("response_type", LTIConstants.ID_TOKEN); //Always this value, as specified in the standard.
        authRequestMap.put("scope", LTIConstants.OPEN_ID);  //Always this value, as specified in the standard.
        // The state is something that we can create and add anything we want on it.
        // On this case, we have decided to create a JWT token with some information that we will use as additional security. But it is not mandatory.
        String state = new LTIJWTUtil().generateState(platformDeployment, authRequestMap, loginInitiationDTO, platformDeployment.getLtiClientId(), platformDeployment.getLtiDeploymentId());
        authRequestMap.put("state", state); //The state we use later to retrieve some useful information about the OICD request.
        authRequestMap.put("oicdEndpoint", platformDeployment.getLtiOidc());  //We need this in the Thymeleaf template in case we decide to use the POST method. It is the endpoint where the LMS receives the OICD requests
        authRequestMap.put("oicdEndpointComplete", generateCompleteUrl(authRequestMap));  //This generates the URL to use in case we decide to use the GET method
        return authRequestMap;
    }

    private String generateCompleteUrl(Map<String, String> model) throws UnsupportedEncodingException {
        String getUrl = model.get("oicdEndpoint");

        getUrl = UrlTool.setParam(getUrl, "client_id", model.get(LTIConstants.LTI_PARAM_CLIENT_ID));
        getUrl = UrlTool.setParam(getUrl, "login_hint", model.get("login_hint"));
        getUrl = UrlTool.setParam(getUrl, "lti_message_hint", model.get("lti_message_hint"));
        getUrl = UrlTool.setParam(getUrl, "nonce", model.get("nonce_hash"));
        getUrl = UrlTool.setParam(getUrl, "prompt", model.get("prompt"));
        getUrl = UrlTool.setParam(getUrl, "redirect_uri", model.get("redirect_uri"));
        getUrl = UrlTool.setParam(getUrl, "response_mode", model.get("response_mode"));
        getUrl = UrlTool.setParam(getUrl, "response_type", model.get("response_type"));
        getUrl = UrlTool.setParam(getUrl, "scope", model.get("scope"));
        getUrl = UrlTool.setParam(getUrl, "state", model.get("state"));
        return getUrl;
    }

}
