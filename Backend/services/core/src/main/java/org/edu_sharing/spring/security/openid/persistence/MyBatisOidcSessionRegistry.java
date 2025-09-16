package org.edu_sharing.spring.security.openid.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.oidc.session.OidcSessionInformation;
import org.springframework.security.oauth2.client.oidc.session.OidcSessionRegistry;
import org.springframework.security.oauth2.client.oidc.authentication.logout.OidcLogoutToken;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class MyBatisOidcSessionRegistry implements OidcSessionRegistry {

    private final OidcUserSessionMapper mapper;

    @Override
    public void saveSessionInformation(OidcSessionInformation info) {
        OidcSessionInformationDto dto = OidcSessionInformationDto.from(info);
        mapper.save(info.getSessionId(), dto);
    }

    @Override
    @Transactional
    public OidcSessionInformation removeSessionInformation(String clientSessionId) {
        OidcUserSessionRecord record = mapper.findBySessionId(clientSessionId);
        if (record != null) {
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
        List<OidcUserSessionRecord> all = mapper.findAll();
        Predicate<OidcSessionInformationDto> matcher = (token.getSessionId() != null)
                ? dto -> token.getSessionId().equals(dto.getClaims().get("sid"))
                : dto -> token.getSubject().equals(dto.getSubject());

        List<OidcSessionInformation> removed = new ArrayList<>();
        for (OidcUserSessionRecord record : all) {
            if (matcher.test(record.getSessionInformation())) {
                mapper.deleteBySessionId(record.getSessionId());
                removed.add(record.getSessionInformation()
                        .toDomain(Collections.emptySet())); // you can enrich authorities if needed
            }
        }
        return removed;
    }
}
