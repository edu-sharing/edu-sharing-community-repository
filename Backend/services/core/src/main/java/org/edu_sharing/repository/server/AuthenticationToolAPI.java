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
package org.edu_sharing.repository.server;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.acegisecurity.AuthenticationCredentialsNotFoundException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.alfresco.repo.security.authentication.AuthenticationException;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.security.authentication.RepositoryAuthenticatedUser;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.PersonService;
import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.NotAnAdminException;
import org.edu_sharing.service.authority.AuthorityServiceHelper;
import org.edu_sharing.service.toolpermission.ToolPermissionService;
import org.edu_sharing.service.toolpermission.ToolPermissionServiceFactory;
import org.edu_sharing.spring.ApplicationContextFactory;
import org.edu_sharing.spring.security.basic.EduSharingPrincipal;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Primary
@Service
@RequiredArgsConstructor
public class AuthenticationToolAPI extends AuthenticationToolAbstract {

    private final AuthenticationService authenticationService;
    private final PersonService personService;
    private final NodeService nodeService;

    private final AuthenticationComponent authenticationComponent;

    @Deprecated
    public static AuthenticationToolAPI getInstance() {
        ApplicationContext applicationContext = ApplicationContextFactory.getApplicationContext();

        // DIRTY HACK!!! The context isn't set on startup, so we need to return null
        if (applicationContext == null) {
            ApplicationContext alfAppContext = AlfAppContextGate.getApplicationContext();
            ServiceRegistry serviceRegistry = alfAppContext.getBean(ServiceRegistry.class);
            return new AuthenticationToolAPI(serviceRegistry.getAuthenticationService(),
                    serviceRegistry.getPersonService(),
                    serviceRegistry.getNodeService(),
                    alfAppContext.getBean("authenticationComponent", AuthenticationComponent.class));
        }

        return applicationContext.getBean(AuthenticationToolAPI.class);
    }

    //for RepoFactory getAuthenticationToolInstance will ignore AppId cause it works only with homerepo

    public Map<String, String> createNewSession(String userName, String password) throws Exception {
        if (password != null) {
            authenticationService.authenticate(userName, password.toCharArray());
        } else {
            authenticationComponent.setCurrentUser(userName);
        }
        Map<String, String> returnval = new HashMap<>();
        returnval.put(CCConstants.AUTH_USERNAME, authenticationService.getCurrentUserName());
        returnval.put(CCConstants.AUTH_TICKET, authenticationService.getCurrentTicket());

        addClientUserInfo(returnval);

        ToolPermissionService toolPermissionService = ToolPermissionServiceFactory.getInstance();
        // DIRTY HACK!!! The context isn't set on startup, so we need to return null
        if (toolPermissionService != null) {
            toolPermissionService.invalidateSessionCache();
        }
        return returnval;
    }

    public void addToSpringSecurityContext(HttpSession session) {
            net.sf.acegisecurity.Authentication acegiAuth = AuthenticationUtil.getFullAuthentication();
            if(acegiAuth == null) return;
            net.sf.acegisecurity.providers.dao.User alfrescoPrincipal = (net.sf.acegisecurity.providers.dao.User) acegiAuth.getPrincipal();
            if (SecurityContextHolder.getContext().getAuthentication() == null
                    || SecurityContextHolder.getContext().getAuthentication() instanceof AnonymousAuthenticationToken
                    || !SecurityContextHolder.getContext().getAuthentication().getName().equals(alfrescoPrincipal.getUsername())
            ) {
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
                SecurityContextHolder.getContext().setAuthentication(springAuth);

                // set Session-Attribut
                session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, SecurityContextHolder.getContext());
            }
        }

        @Override
        public Map<String, String> getUserInfo (String userName, String ticket) throws Exception {
            authenticationService.validate(ticket);
            Map<String, String> returnval = new HashMap<>();
            returnval.put(CCConstants.AUTH_USERNAME, authenticationService.getCurrentUserName());
            returnval.put(CCConstants.AUTH_TICKET, authenticationService.getCurrentTicket());

            addClientUserInfo(returnval);

            return returnval;
        }

        /**
         * Gets the current scope of the session, or null for the default workspace
         */
        public String getScope () {
            if (Context.getCurrentInstance() == null) return null;
            if (Context.getCurrentInstance().getRequest() == null) return null;
            HttpSession session = Context.getCurrentInstance().getRequest().getSession();
            if (session == null) return null;
            return (String) session.getAttribute(CCConstants.AUTH_SCOPE);
        }

        private void addClientUserInfo (Map < String, String > authInfo) throws Exception {

            MCAlfrescoAPIClient mcAlfrescoAPIClient = new MCAlfrescoAPIClient(authInfo);
            Map<String, String> repositoryUseInfo = mcAlfrescoAPIClient.getUserInfo(authenticationService.getCurrentUserName());
            String userNameCaption = repositoryUseInfo.get(CCConstants.CM_PROP_PERSON_EMAIL);

            if (StringUtils.isBlank(userNameCaption)) {
                userNameCaption = authInfo.get(CCConstants.AUTH_USERNAME);
            }
            authInfo.put(CCConstants.AUTH_USERNAME_CAPTION, userNameCaption);

            String homeFolderId = mcAlfrescoAPIClient.getHomeFolderID(authInfo.get(CCConstants.AUTH_USERNAME));
            authInfo.put(CCConstants.AUTH_USER_HOMEDIR, homeFolderId);

            boolean isAdmin = mcAlfrescoAPIClient.isAdmin(authInfo.get(CCConstants.AUTH_USERNAME));
            authInfo.put(CCConstants.AUTH_USER_ISADMIN, Boolean.valueOf(isAdmin).toString());
        }

        public String setUser (String authorityName){
            if (!AuthorityServiceHelper.isAdmin()) {
                throw new NotAnAdminException();
            }
            if (authenticationComponent.setCurrentUser(authorityName) != null) {
                return authenticationService.getCurrentTicket();
            }
            throw new RuntimeException("Authentication failed for " + authorityName);
        }

        @Override
        public void logout ( final String ticket){
            try {
                authenticationService.invalidateTicket(ticket);
                authenticationService.clearCurrentSecurityContext();
            } catch (AuthenticationCredentialsNotFoundException e) {
                log.debug("it seems there is a logout call with a ticket without a security context:");
                log.debug(e.getMessage());
                logoutWithoutSecurityContext(ticket);
            }
        }

        public void logoutWithoutSecurityContext ( final String ticket){

            RunAsWork<Void> ra = () -> {
                authenticationService.invalidateTicket(ticket);
                authenticationService.clearCurrentSecurityContext();
                log.debug("none security context ticket invalidation done");
                return null;
            };

            AuthenticationUtil.runAs(ra, "admin");
        }

        /**
         * - tries to find a alfresco ticket in session
         * - if there is one it will be validated
         * - when it's valid the corresponding user will be determined
         * - ticket and username will be returned
         *
         * @return null when no valid ticket was found else user name / ticket as Map<String,String>
         */
        public Map<String, String> validateAuthentication (HttpSession session){
            Map<String, String> result = null;
            String currentTicket = (String) session.getAttribute(CCConstants.AUTH_TICKET);
            log.debug("session id{} ticketId:{}", session.getId(), currentTicket);
            if (currentTicket != null) {
                try {
                    authenticationService.validate(currentTicket);
                    result = new HashMap<>();
                    result.put(CCConstants.AUTH_USERNAME, authenticationService.getCurrentUserName());
                    result.put(CCConstants.AUTH_TICKET, currentTicket);
                } catch (AuthenticationException e) {
                    log.warn(e.getMessage());
                }
            }
            return result;
        }

        @Override
        public boolean validateTicket (String ticket){
            try {
                if (ticket == null) {
                    return false;
                }
                authenticationService.validate(ticket);

                log.info("User logged in: {}, ticket: {}", authenticationService.getCurrentUserName(), ticket);
                return true;
            } catch (AuthenticationException e) {
                log.info("{}, ticket: {}", e.getMessage(), ticket);
            }
            return false;
        }

        /**
         * ignores user name param and takes the one it gets from authentication service
         */
        @Override
        public void storeAuthInfoInSession (String username, String ticket, String authType, HttpSession session){

            authenticationService.validate(ticket);
            super.storeAuthInfoInSession(authenticationService.getCurrentUserName(), ticket, authType, session);

            //validate a second time cause super.storeAuthInfoInSession makes a logout when another tickets is in session
            //i.e jession with ticket + basic auth in ApiAuthenticationFilter
            authenticationService.validate(ticket);

            try {
                Map<String, String> userInfo = getUserInfo(authenticationService.getCurrentUserName(), ticket);
                session.setAttribute(CCConstants.AUTH_USERNAME_CAPTION, userInfo.get(CCConstants.AUTH_USERNAME_CAPTION));
            } catch (Exception ignored) {

            }

            String locale = (String) session.getAttribute(CCConstants.AUTH_LOCALE);
            if (locale == null) {
                Object localeObj = nodeService.getProperty(personService.getPerson(authenticationService.getCurrentUserName()), ContentModel.PROP_LOCALE);
                if (localeObj != null) {
                    session.setAttribute(CCConstants.AUTH_LOCALE, localeObj.toString());
                }
            }

            addToSpringSecurityContext(session);
        }

        public void authenticateUser(String username, HttpSession session,String authType){
            authenticationComponent.setCurrentUser(username);
            storeAuthInfoInSession(username, authenticationService.getCurrentTicket(), authType, session);
        }

    }
