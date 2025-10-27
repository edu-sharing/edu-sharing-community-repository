package org.edu_sharing.spring.security.openid.persistence;

/*
 * Copyright 2002-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.springframework.security.oauth2.client.oidc.session.OidcSessionInformation;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * A JSON-serializable representation of {@link OidcSessionInformation}, for use with
 * JDBC/MyBatis persistence.
 *
 * Since {@link OidcSessionInformation} itself cannot be deserialized by Jackson (no
 * default constructor, complex principal), this DTO flattens the fields into a
 * JSON-friendly structure.
 *
 */
@Getter
public final class OidcSessionInformationDto {

    private final String sessionId;

    private final Map<String, String> authorities;

    private final Map<String, Object> claims;

    private final String subject;

    private final String issuer;

    private final Instant issuedAt;

    private final Instant expiresAt;

    @JsonCreator
    public OidcSessionInformationDto(
            @JsonProperty("sessionId") String sessionId,
            @JsonProperty("authorities") Map<String, String> authorities,
            @JsonProperty("claims") Map<String, Object> claims,
            @JsonProperty("subject") String subject,
            @JsonProperty("issuer") String issuer,
            @JsonProperty("issuedAt") Instant issuedAt,
            @JsonProperty("expiresAt") Instant expiresAt) {

        this.sessionId = sessionId;
        this.authorities = (authorities != null) ? new LinkedHashMap<>(authorities) : Collections.emptyMap();
        this.claims = (claims != null) ? new LinkedHashMap<>(claims) : Collections.emptyMap();
        this.subject = subject;
        this.issuer = issuer;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
    }

    /**
     * Convert from a runtime {@link OidcSessionInformation} to a persistable DTO.
     */
    public static OidcSessionInformationDto from(OidcSessionInformation info) {
        OidcUser user = info.getPrincipal();
        OidcIdToken idToken = user.getIdToken();
        return new OidcSessionInformationDto(
                info.getSessionId(),
                info.getAuthorities(),
                user.getClaims(),
                user.getSubject(),
                idToken.getIssuer().toString(),
                idToken.getIssuedAt(),
                idToken.getExpiresAt()
        );
    }

    /**
     * Convert this DTO back to a runtime {@link OidcSessionInformation}.
     *
     * @param authorities the granted authorities (if needed to rebuild the {@link DefaultOidcUser})
     * @return the reconstructed {@link OidcSessionInformation}
     */
    public OidcSessionInformation toDomain(Set authorities) {
        // tokenvalue is not saved in database for security reasons
        OidcIdToken idToken = new OidcIdToken("dummy", this.issuedAt, this.expiresAt, this.claims);
        OidcUserInfo userInfo = new OidcUserInfo(this.claims);
        OidcUser user = new DefaultOidcUser(authorities, idToken, userInfo);
        return new OidcSessionInformation(this.sessionId, this.authorities, user);
    }

}
