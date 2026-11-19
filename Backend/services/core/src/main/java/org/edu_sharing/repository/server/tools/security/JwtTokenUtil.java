package org.edu_sharing.repository.server.tools.security;

import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.edu_sharing.alfresco.service.guest.GuestService;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.restservices.shared.UserProfile;

import java.security.GeneralSecurityException;
import java.security.Key;
import java.util.Collection;
import java.util.Date;
import java.util.UUID;


@Slf4j
public class JwtTokenUtil {

    /**
     * Pseudo user of the tokens for link shares. Recipients of a link share are not logged in (and the
     * guest account may be disabled), so the token carries this fixed identity instead.
     */
    public static final String SHARE_LINK_USERNAME = "share-link";

    public static String generateToken(String username, String nodeId, Collection<String> permissions, String mimeType, String mediaType, String replicationSource, String resourceType, UserProfile userProfile) throws GeneralSecurityException {
        boolean guest = AlfAppContextGate.getApplicationContext().getBean(GuestService.class).isGuestUser(username);
        return generateToken(username, nodeId, permissions, mimeType, mediaType, replicationSource, resourceType, userProfile, guest, false);
    }

    /**
     * @param guest whether the token is issued for a guest
     * @param share whether the token is issued for a link share, the rendering service then
     *              authenticates its callbacks with the share instead of the user
     */
    public static String generateToken(String username, String nodeId, Collection<String> permissions, String mimeType, String mediaType, String replicationSource, String resourceType, UserProfile userProfile, boolean guest, boolean share) throws GeneralSecurityException {
        Key privateKey = new Signing().getPemPrivateKey(ApplicationInfoList.getHomeRepository().getPrivateKey(), CCConstants.SECURITY_KEY_ALGORITHM);

        Date date = new Date();
        String jwt = Jwts.builder()
                .setHeaderParam("kid", ApplicationInfoList.getHomeRepository().getAppId())  // The key id used to sign this
                .setHeaderParam("typ", "JWS") // The type
                .setHeaderParam("repoId", ApplicationInfoList.getHomeRepository().getAppId()) // repoId
                .setExpiration(DateUtils.addSeconds(date, 3600)) //a java.util.Date
                .setNotBefore(date) //a java.util.Date
                .setIssuedAt(date) // for example, now
                .setIssuer(username)
                .setId(UUID.randomUUID().toString())
                .setSubject("node permissions")
                .claim("repoId", ApplicationInfoList.getHomeRepository().getAppId())
                .claim("node", nodeId)
                .claim("permissions", permissions)
                .claim("mimeType", mimeType)
                .claim("mediaType", mediaType)
                .claim("replicationSource", replicationSource)
                .claim("resourceType", resourceType)
                .claim("firstName", userProfile.getFirstName())
                .claim("lastName", userProfile.getLastName())
                .claim("userEmail", userProfile.getEmail())
                .claim("primaryAffiliation", userProfile.getPrimaryAffiliation())
                .claim("isGuest", guest)
                .claim("share", share)
                .signWith(privateKey)
                .compact();

        // never log the token: it is a credential for the rendering service and holds name, e-mail and affiliation of the user
        log.debug("rendering JWT created for node {} (user {})", nodeId, username);
        return jwt;
    }
}
