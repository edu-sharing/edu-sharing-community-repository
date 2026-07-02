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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.alfresco.repo.security.authentication.AuthenticationException;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.PersonService;
import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.alfresco.authentication.subsystems.SubsystemChainingAuthenticationService;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.Map;

@Slf4j
public class EduAuthenticationComponent {


    /**
     * -- SETTER --
     * IOC
     *
     * @param ccAuthMethod
     */
    private Map<Class<? extends AuthMethodInterface>, AuthMethodInterface> ccAuthMethodMap;

    public void setCcAuthMethod(List<AuthMethodInterface> ccAuthMethod) {
        this.ccAuthMethodMap = ccAuthMethod
                .stream()
                .collect(Collectors.toMap(AuthMethodInterface::getClass, x -> x));
    }

    private AuthenticationComponent authenticationComponent;
    private PersonService personService;
    private NodeService nodeService;

    public void init() {
        ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
        this.authenticationComponent = (AuthenticationComponent) applicationContext.getBean("authenticationComponent");
        ServiceRegistry sr = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
        personService = sr.getPersonService();
        nodeService = sr.getNodeService();
    }

    /**
     * @param authClass
     * @param params
     * @TODO dont allow shadow users to authenticate in the standard way or create an random password
     * <p>
     * the authclass must be declared in the property ccAuthMethod List in the file authentication-services-context.xml
     * <p>
     * This Method isnt part of the alfresco AbstractAuthenticationComponent Class
     * it will be called by special Methods of the campuscontent implementation of AuthenticationService
     */
    public String authenticate(Class<? extends AuthMethodInterface> authClass, Map<String, String> params) throws AuthenticationException {

        //only allow classes that are in ccAuthMethod List
        AuthMethodInterface authenticator = ccAuthMethodMap.get(authClass);
        if(authenticator == null) {
            log.error("authClass:{} not found in ccAuthMethodList", authClass);
            throw new AuthenticationException(AuthenticationExceptionMessages.AUTHENTICATION_FAILED);
        }

        log.info("authenticator:{}", authenticator);
        String username = authenticator.authenticate(params);

        if (StringUtils.isBlank(username)) {
            log.info("Auth failed for class:{}", authClass);
            throw new AuthenticationException(AuthenticationExceptionMessages.AUTHENTICATION_FAILED);
        } else {

            String repoUsername = AuthenticationUtil.runAsSystem(() -> {
                // TODO Auto-generated method stub
                NodeRef personNodeRef = personService.getPersonOrNull(username);
                if (personNodeRef == null) {
                    log.error("person does not exist:{}", username);
                    throw new AuthenticationException(AuthenticationExceptionMessages.USERNOTFOUND);
                }

                return (String) nodeService.getProperty(personNodeRef, ContentModel.PROP_USERNAME);
            });

            //inform Alfresco that the following user authenticated successfully
            authenticationComponent.setCurrentUser(repoUsername);

            //set last login
            Object alfAuthService = AlfAppContextGate.getApplicationContext().getBean("authenticationService");
            if (alfAuthService instanceof SubsystemChainingAuthenticationService scAuthService) {
                scAuthService.setLoginTimestampToNow(username, CCConstants.PROP_USER_ESFIRSTLOGIN);
                scAuthService.setLoginTimestampToNow(username, CCConstants.PROP_USER_ESLASTLOGIN);
                SubsystemChainingAuthenticationService.callLoginInterceptors(repoUsername);
            }
        }
        return username;
    }

}
