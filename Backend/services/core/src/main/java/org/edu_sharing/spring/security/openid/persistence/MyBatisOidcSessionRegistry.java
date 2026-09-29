package org.edu_sharing.spring.security.openid.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.oidc.session.OidcSessionInformation;
import org.springframework.security.oauth2.client.oidc.session.OidcSessionRegistry;
import org.springframework.security.oauth2.client.oidc.authentication.logout.OidcLogoutToken;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class MyBatisOidcSessionRegistry implements OidcSessionRegistry {

    private final OidcUserSessionMapper mapper;

    @Override
    public void saveSessionInformation(OidcSessionInformation info) {
        OidcSessionInformationDto dto = OidcSessionInformationDto.from(info);
        log.debug("adding session information: {} , idp sid: {}, issuedAt: {}. expiresAt: {}",
                info.getSessionId(),
                info.getPrincipal().getClaims().get("sid"),
                info.getPrincipal().getIssuedAt(),
                info.getPrincipal().getExpiresAt());
        mapper.save(info.getSessionId(), dto);
    }

    @Override
    public OidcSessionInformation removeSessionInformation(String clientSessionId) {
        OidcUserSessionRecord record = mapper.findBySessionId(clientSessionId);
        if (record != null) {
            log.debug("Removing session information for sessionId: {}", record.getSessionId());
            mapper.deleteBySessionId(clientSessionId);
            return record.getSessionInformation().toDomain(record.getSessionInformation().getAuthorities().entrySet()
                    .stream()
                    .map(e -> (org.springframework.security.core.GrantedAuthority)
                            (org.springframework.security.core.authority.SimpleGrantedAuthority)
                                    new org.springframework.security.core.authority.SimpleGrantedAuthority(e.getKey()))
                    .collect(Collectors.toSet()));
        }
        return null;
    }

    @Override
    public Iterable<OidcSessionInformation> removeSessionInformation(OidcLogoutToken token) {
        log.debug("cleanup idp session: {},subject: {}", token.getSessionId(), token.getSubject());

        /*
         * only the matching rows are read. reading the whole table and filtering in java does not scale:
         * every logout would materialize the complete registry in the heap, once per request thread.
         */
        List<OidcUserSessionRecord> matches = (token.getSessionId() != null)
                ? mapper.findBySid(token.getSessionId())
                : mapper.findBySubject(token.getSubject());

        List<OidcSessionInformation> removed = new ArrayList<>();
        for (OidcUserSessionRecord record : matches) {
            log.debug("Removing session information: {} , idp sid: {}, issuedAt: {}. expiresAt: {}",
                    record.getSessionId(),
                    record.getSessionInformation().getClaims().get("sid"),
                    record.getSessionInformation().getIssuedAt(),
                    record.getSessionInformation().getExpiresAt());
            mapper.deleteBySessionId(record.getSessionId());
            removed.add(record.getSessionInformation()
                    .toDomain(Collections.emptySet())); // you can enrich authorities if needed
        }
        return removed;
    }
}
