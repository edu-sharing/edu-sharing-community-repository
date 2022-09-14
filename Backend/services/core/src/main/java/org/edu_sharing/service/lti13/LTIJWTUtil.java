package org.edu_sharing.service.lti13;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.AsymmetricJWK;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import io.jsonwebtoken.*;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.security.Signing;
import org.edu_sharing.restservices.shared.Node;
import org.edu_sharing.restservices.shared.NodeRef;
import org.edu_sharing.service.lti13.model.LTISessionObject;
import org.edu_sharing.service.lti13.model.LoginInitiationDTO;
import org.edu_sharing.service.mime.MimeTypesV2;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.PublicKey;
import java.text.ParseException;
import java.util.*;

public class LTIJWTUtil {

    Logger logger = Logger.getLogger(LTIJWTUtil.class);

    ApplicationInfo platform = null;

    public String generateState(ApplicationInfo platformDeployment, Map<String, String> authRequestMap, LoginInitiationDTO loginInitiationDTO, String clientIdValue, String deploymentIdValue) throws GeneralSecurityException, IOException {

        Date date = new Date();
        Key issPrivateKey = new Signing().getPemPrivateKey(ApplicationInfoList.getHomeRepository().getPrivateKey(), CCConstants.SECURITY_KEY_ALGORITHM);
        String state = Jwts.builder()
                .setHeaderParam("kid", ApplicationInfoList.getHomeRepository().getAppId())  // The key id used to sign this
                .setHeaderParam("typ", "JWT") // The type
                .setIssuer("ltiStarter")  //This is our own identifier, to know that we are the issuer.
                .setSubject(platformDeployment.getLtiIss()) // We store here the platform issuer to check that matches with the issuer received later
                .setAudience(platformDeployment.getLtiClientId())  //We send here the clientId to check it later.
                .setExpiration(DateUtils.addSeconds(date, 3600)) //a java.util.Date
                .setNotBefore(date) //a java.util.Date
                .setIssuedAt(date) // for example, now
                .setId(authRequestMap.get(LTIConstants.LTI_NONCE)) //just a nonce... we don't use it by the moment, but it could be good if we store information about the requests in DB.
                .claim("original_iss", loginInitiationDTO.getIss())  //All this claims are the information received in the OIDC initiation and some other useful things.
                .claim("loginHint", loginInitiationDTO.getLoginHint())
                .claim("ltiMessageHint", loginInitiationDTO.getLtiMessageHint())
                .claim("targetLinkUri", loginInitiationDTO.getTargetLinkUri())
                .claim("clientId", clientIdValue)
                .claim("ltiDeploymentId", deploymentIdValue)
                .claim("controller", "/oidc/login_initiations")
                .signWith(SignatureAlgorithm.RS256, issPrivateKey)  //We sign it
                .compact();
        logger.info("State:" + state);
        return state;
    }

    public Jws<Claims> validateState(String state) {
        return Jwts.parser().setSigningKeyResolver(new SigningKeyResolverAdapter() {
            // This is done because each state is signed with a different key based on the issuer... so
            // we don't know the key and we need to check it pre-extracting the claims and finding the kid
            @Override
            public Key resolveSigningKey(JwsHeader header, Claims claims) {
                PublicKey toolPublicKey;
                try {
                    // We are dealing with RS256 encryption, so we have some Oauth utils to manage the keys and
                    // convert them to keys from the string stored in DB. There are for sure other ways to manage this.
                    toolPublicKey = new Signing().getPemPublicKey(
                            ApplicationInfoList.getHomeRepository().getPublicKey(),
                            CCConstants.SECURITY_KEY_ALGORITHM);
                } catch (GeneralSecurityException ex) {
                    logger.error("Error validating the state. Error generating the tool public key", ex);
                    return null;
                }
                return toolPublicKey;
            }
        }).parseClaimsJws(state);
        // If we are on this point, then the state signature has been validated. We can start other tasks now.
    }

    public Jws<Claims> validateJWT(String jwt){
        return validateJWT(jwt,null,null);
    }

    public Jws<Claims> validateJWT(String jwt, String clientId, String deploymentId) {

        return Jwts.parser().setSigningKeyResolver(new SigningKeyResolverAdapter() {

            // This is done because each state is signed with a different key based on the issuer... so
            // we don't know the key and we need to check it pre-extracting the claims and finding the kid
            @Override
            public Key resolveSigningKey(JwsHeader header, Claims claims) {

                String tmpClientId = clientId;
                String tmpDeploymentId = deploymentId;
                try {

                    /**
                     * @TODO: why this fallback is used
                     * clientId vs audience (" audience of a token is the intended recipient of the token.")
                     */
                    if(tmpClientId == null) tmpClientId = claims.getAudience();
                    if(tmpDeploymentId == null) tmpDeploymentId = claims.get(LTIConstants.LTI_DEPLOYMENT_ID, String.class);
                    // We are dealing with RS256 encryption, so we have some Oauth utils to manage the keys and
                    // convert them to keys from the string stored in DB. There are for sure other ways to manage this.
                    platform = new RepoTools().getApplicationInfo(claims.getIssuer(),tmpClientId,tmpDeploymentId);
                } catch (LTIException ex) {
                    logger.error("no platform with " + claims.getIssuer() +" " + clientId + " registered", ex);
                    return null;
                }
                // If the platform has a JWK Set endpoint... we try that.
                if (StringUtils.isNoneEmpty(platform.getPublicKey())) {
                    try {
                        JWKSet publicKeys = JWKSet.load(new URL(platform.getLtiKeysetUrl()));
                        JWK jwk = publicKeys.getKeyByKeyId(header.getKeyId());
                        return ((AsymmetricJWK) jwk).toPublicKey();
                        /*return new Signing().getPemPublicKey(
                                platform.getPublicKey(),
                                CCConstants.SECURITY_KEY_ALGORITHM); */
                    } catch (Exception e) {
                        logger.error("Error getting the iss public key", e);
                        return null;
                    }
                } else { // If not, we get the key stored in our configuration
                    logger.error("The platform configuration must contain a valid Public Key");
                    return null;
                }

            }
        }).parseClaimsJws(jwt);
    }

    public String getDeepLinkingResponseJwt(LTISessionObject ltiSessionObject, String nodeId, String title) throws GeneralSecurityException{
        Node n = new Node();
        n.setRef(new NodeRef(ApplicationInfoList.getHomeRepository().getAppId(),nodeId));
        n.setTitle(title);
        return getDeepLinkingResponseJwt(ltiSessionObject,new Node[]{n});
    }



    public String getDeepLinkingResponseJwt(LTISessionObject ltiSessionObject, Node[] nodes) throws GeneralSecurityException{
        String appId = ltiSessionObject.getEduSharingAppId();
        ApplicationInfo homeApp = ApplicationInfoList.getHomeRepository();
        Key toolPrivateKey = new Signing().getPemPrivateKey(homeApp.getPrivateKey(), CCConstants.SECURITY_KEY_ALGORITHM);

        ApplicationInfo appInfo = ApplicationInfoList.getApplicationInfos().get(appId);
        if(appInfo == null){
            logger.error("no appinfo found for appId:" + appId);
            return null;
        }

        Date date = new Date();
        String jwt = Jwts.builder()
                .setHeaderParam(LTIConstants.TYP, LTIConstants.JWT)

                .setHeaderParam(LTIConstants.KID, homeApp.getLtiKid())
                .setHeaderParam(LTIConstants.ALG, LTIConstants.RS256)
                .setIssuer(appInfo.getLtiClientId())  //Client ID
                .setAudience(ltiSessionObject.getIss())
                .setExpiration(DateUtils.addSeconds(date, 3600)) //a java.util.Date
                .setIssuedAt(date) // for example, now
                .claim(LTIConstants.LTI_NONCE, ltiSessionObject.getNonce())
                .claim(LTIConstants.LTI_AZP, ltiSessionObject.getIss())
                .claim(LTIConstants.LTI_DEPLOYMENT_ID, ltiSessionObject.getDeploymentId())
                    .claim(LTIConstants.LTI_MESSAGE_TYPE, LTIConstants.LTI_MESSAGE_TYPE_DEEP_LINKING_RESPONSE)
                .claim(LTIConstants.LTI_VERSION, LTIConstants.LTI_VERSION_3)
                .claim(LTIConstants.LTI_DATA, ltiSessionObject.getDeepLinkingSettings().get(LTIConstants.DEEP_LINK_DATA))
                .claim(LTIConstants.LTI_CONTENT_ITEMS, generateContentItems(nodes))
                .signWith(SignatureAlgorithm.RS256, toolPrivateKey)  //We sign it
                .compact();
        return jwt;
    }

    public static String sign(String string, ApplicationInfo appInfo){
        Key privateKey = null;
        try {
            privateKey = new Signing().getPemPrivateKey(appInfo.getPrivateKey(), CCConstants.SECURITY_KEY_ALGORITHM);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }



        return Jwts.builder().setPayload(string).signWith(privateKey).compact();
    }

    public static Jws<Claims> validateJWT(String jwt, ApplicationInfo appInfo){
        return Jwts.parser().setSigningKeyResolver(new SigningKeyResolverAdapter(){
            @Override
            public Key resolveSigningKey(JwsHeader header, Claims claims) {
                try {
                    /*return new Signing().getPemPublicKey(
                            appInfo.getPublicKey(),
                            CCConstants.SECURITY_KEY_ALGORITHM);*/
                    JWKSet publicKeys = JWKSet.load(new URL(appInfo.getLtiKeysetUrl()));
                    String keyId = header.getKeyId();
                    if(keyId == null) throw new RuntimeException("missing keyid");
                    JWK jwk = publicKeys.getKeyByKeyId(keyId);
                    if(jwk == null) throw new RuntimeException("no public key found for key: "+keyId);
                    return ((AsymmetricJWK) jwk).toPublicKey();
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                } catch (JOSEException e) {
                    throw new RuntimeException(e);
                }
            }
        }).parseClaimsJws(jwt);
    }



    private List<Map<String, Object>> generateContentItems(Node[] nodes){
        List<Map<String, Object>> deepLinks = new ArrayList<>();
        for(Node node : nodes){
            Map<String, Object> deepLink = new HashMap<>();

            deepLink.put(LTIConstants.DEEP_LINK_TYPE, LTIConstants.DEEP_LINK_LTIRESOURCELINK);
            deepLink.put(LTIConstants.DEEP_LINK_TITLE, node.getTitle() != null ? node.getTitle() : node.getName());
            //deepLink.put(LTIConstants.DEEP_LINK_URL, ApplicationInfoList.getHomeRepository().getClientBaseUrl() + "/components/render/"+node.getRef().getId()+"?closeOnBack=true");
            deepLink.put(LTIConstants.DEEP_LINK_URL, ApplicationInfoList.getHomeRepository().getClientBaseUrl() + "/rest/lti/v13/"+LTIConstants.LTI_TOOL_REDIRECTURL_PATH+"/"+node.getRef().getId());

            try {
                HashMap<String,String> thumbnail = new HashMap<>();
                //thumbnail.put("url",node.getPreview().getUrl());
                thumbnail.put("url",new MimeTypesV2().getIcon(node.getType(),NodeServiceFactory.getLocalService().getProperties(StoreRef.PROTOCOL_WORKSPACE,StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier() ,node.getRef().getId()) ,node.getAspects()));
                thumbnail.put("width",""+node.getPreview().getWidth());
                thumbnail.put("height",""+node.getPreview().getHeight());
                deepLink.put("icon",thumbnail);
            } catch (Throwable e) {
                logger.error(e.getMessage());
            }

            /**
             * @TODO if h5p add scoreMaximum stuff
             */
            //deepLink.put("lineItem", lineItem());


            /**
             * @TODO check if needed
             */
            /*
            Map<String, String> availableDates = new HashMap<>();
            Map<String, String> submissionDates = new HashMap<>();
            Map<String, String> custom = new HashMap<>();
            availableDates.put("startDateTime", "2018-03-07T20:00:03Z");
            availableDates.put("endDateTime", "2022-03-07T20:00:03Z");
            submissionDates.put("startDateTime", "2019-03-07T20:00:03Z");
            submissionDates.put("endDateTime", "2021-08-07T20:00:03Z");
            custom.put("dueDate", "$Resource.submission.endDateTime");
            custom.put("controlValue", "This is whatever I want to write here");
            deepLink.put("available", availableDates);
            deepLink.put("submission", submissionDates);
            deepLink.put("custom", custom);*/
            deepLinks.add(deepLink);
        }
        return deepLinks;
    }

    Map<String, Object> lineItem() {
        Map<String, Object> deepLink = new HashMap<>();

        deepLink.put("scoreMaximum", 87);
        deepLink.put("label", "LTI 1234 Quiz");
        deepLink.put("resourceId", "1234");
        deepLink.put("tag", "myquiztest");
        return deepLink;
    }

    public ApplicationInfo getApplicationInfo() {
        return platform;
    }
}
