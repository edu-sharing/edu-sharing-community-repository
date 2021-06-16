package org.edu_sharing.service.authority;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.springframework.context.ApplicationContext;

import java.util.Set;

public class AuthorityServiceHelper {

    /**
     * take the current runAs alfresco user and check if it is an admin normally
     */
    public static boolean isAdmin() {
        return isAdmin(AuthenticationUtil.getRunAsUser());
    }

    public static boolean isAdmin(String username) {
        try {
            ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
            ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
            Set<String> testUsetAuthorities = serviceRegistry.getAuthorityService().getAuthoritiesForUser(username);
            for (String testAuth : testUsetAuthorities) {

                if (testAuth.equals(CCConstants.AUTHORITY_GROUP_ALFRESCO_ADMINISTRATORS)) {
                    return true;
                }
            }
        } catch (org.alfresco.repo.security.permissions.AccessDeniedException e) {
        }
        return false;
    }

}
