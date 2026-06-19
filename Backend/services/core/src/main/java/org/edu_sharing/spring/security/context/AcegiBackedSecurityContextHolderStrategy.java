package org.edu_sharing.spring.security.context;

import lombok.extern.slf4j.Slf4j;
import net.sf.acegisecurity.Authentication;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.edu_sharing.spring.security.basic.EduSharingPrincipal;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.core.context.SecurityContextImpl;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class AcegiBackedSecurityContextHolderStrategy implements SecurityContextHolderStrategy {


    @Override
    public void clearContext() {
        //AuthenticationUtil.clearCurrentSecurityContext();
    }

    @Override
    public SecurityContext getContext() {
        Authentication acegiAuth = AuthenticationUtil.getRunAsAuthentication();
        SecurityContext ctx = createEmptyContext();
        if(acegiAuth == null) {
            return ctx;
        }

        net.sf.acegisecurity.providers.dao.User alfrescoPrincipal = (net.sf.acegisecurity.providers.dao.User) acegiAuth.getPrincipal();
        List<GrantedAuthority> springAuthorities =
                acegiAuth.getAuthorities() == null
                        ? List.of()
                        : Arrays.stream(acegiAuth.getAuthorities())
                          .map(net.sf.acegisecurity.GrantedAuthority::getAuthority)
                          .map(SimpleGrantedAuthority::new)
                          .collect(Collectors.toList());

        EduSharingPrincipal principal = new EduSharingPrincipal(alfrescoPrincipal);
        Object credentials = acegiAuth.getCredentials();
        UsernamePasswordAuthenticationToken springAuth = new UsernamePasswordAuthenticationToken(principal, credentials, springAuthorities);
        springAuth.setDetails(principal);
        if(!acegiAuth.isAuthenticated()) {
            // we can only unset authenticated here because it is set to true in the constructor
            springAuth.setAuthenticated(acegiAuth.isAuthenticated());
        }
        ctx.setAuthentication(springAuth);
        return ctx;
    }

    @Override
    public void setContext(SecurityContext context) {
        log.debug("Setting security context is not supported: {}", context);
        //throw new UnsupportedOperationException("Setting security context is not supported");
    }

    @Override
    public SecurityContext createEmptyContext() {
        return new SecurityContextImpl();
    }
}
