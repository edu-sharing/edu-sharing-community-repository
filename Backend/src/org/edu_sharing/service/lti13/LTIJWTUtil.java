package org.edu_sharing.service.lti13;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.AsymmetricJWK;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import io.jsonwebtoken.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.security.Signing;
import org.edu_sharing.service.lti13.model.LTISessionObject;
import org.edu_sharing.service.lti13.model.LoginInitiationDTO;

import java.io.IOException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.PublicKey;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class LTIJWTUtil {

    Logger logger = Logger.getLogger(LTIJWTUtil.class);

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

    public Jws<Claims> validateJWT(String jwt, String clientId, String deploymentId) {

        return Jwts.parser().setSigningKeyResolver(new SigningKeyResolverAdapter() {

            // This is done because each state is signed with a different key based on the issuer... so
            // we don't know the key and we need to check it pre-extracting the claims and finding the kid
            @Override
            public Key resolveSigningKey(JwsHeader header, Claims claims) {
                ApplicationInfo platform;
                try {
                    // We are dealing with RS256 encryption, so we have some Oauth utils to manage the keys and
                    // convert them to keys from the string stored in DB. There are for sure other ways to manage this.
                    platform = new RepoTools().getApplicationInfo(claims.getIssuer(),clientId,deploymentId);
                } catch (LTIException ex) {
                    logger.error("no platform with " + claims.getIssuer() +" " + clientId + " registered", ex);
                    return null;
                }
                // If the platform has a JWK Set endpoint... we try that.
                if (StringUtils.isNoneEmpty(platform.getPublicKey())) {
                    try {
                        //JWKSet publicKeys = JWKSet.load(new URL(platformDeployment.getJwksEndpoint()));
                        //JWK jwk = publicKeys.getKeyByKeyId(header.getKeyId());
                        //return ((AsymmetricJWK) jwk).toPublicKey();
                        return new Signing().getPemPublicKey(
                                platform.getPublicKey(),
                                CCConstants.SECURITY_KEY_ALGORITHM);
                    } catch (GeneralSecurityException e) {
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

    public String getDeepLinkingResponseJwt(String appId, LTISessionObject ltiSessionObject) throws GeneralSecurityException{
        Key toolPrivateKey = new Signing().getPemPrivateKey(ApplicationInfoList.getHomeRepository().getPrivateKey(), CCConstants.SECURITY_KEY_ALGORITHM);

        ApplicationInfo appInfo = ApplicationInfoList.getApplicationInfos().get(appId);
        if(appInfo == null){
            logger.error("no appinfo found for appId:" + appId);
            return null;
        }

        Date date = new Date();
        String jwt = Jwts.builder()
                .setHeaderParam(LTIConstants.TYP, LTIConstants.JWT)

                /**
                 *  better leave out?
                 */
                //.setHeaderParam(LTIConstants.KID, TextConstants.DEFAULT_KID)
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
                .claim(LTIConstants.LTI_CONTENT_ITEMS, new HashMap<String, Object>())
                .signWith(SignatureAlgorithm.RS256, toolPrivateKey)  //We sign it
                .compact();
        return jwt;
    }
}
