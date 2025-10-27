/**
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */
package org.edu_sharing.service.authentication;

import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.security.authentication.AuthenticationException;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class EduAuthentication {
    private final EduAuthenticationComponent ccAuthenticationComponent;
    private final AuthenticationService authenticationService;

    public void authenticateByTrustedApp(Map<String, String> params) {
        ccAuthenticationComponent.authenticate(AuthMethodTrustedApplication.class, params);
    }

    /**
     * auth type as extra method param cause "params" comes unfiltered from client and they could set the authtype param
     *
     * @param params
     */
    public void authenticateBySSO(Map<String, String> params) {
        ccAuthenticationComponent.authenticate(AuthMethodSSO.class, params);
    }

    public String getCurrentTicket() {
        //alfresco34e update it seems that when authenticationComponent.setCurrentUser is called, there isn't an ticket for that user ready
        //so we can not use icketComponent.getCurrentTicket(authenticationService.getCurrentUserName(),false);
        //authenticationService.getCurrentTicket() delivers a new ticket
        return authenticationService.getCurrentTicket();
    }

    public String getCurrentUserName() throws AuthenticationException {
        return authenticationService.getCurrentUserName();
    }

    public void validate(String ticket) throws AuthenticationException {
        authenticationService.validate(ticket);
    }

    public void authenticate(String userName, char[] password) throws AuthenticationException {
        authenticationService.authenticate(userName, password);
    }
}
