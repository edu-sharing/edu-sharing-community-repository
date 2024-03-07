package org.edu_sharing.spring.security.basic;

import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

/**
 *  redirects to /shibboleth after successfull auth
 *
 *  notice:
 *  SavedRequestAwareAuthenticationSuccessHandler is default which always redircts to /samllogout url with GET which is not vailable
 *      * after logout/login again
 */
public class EduAuthSuccsessHandler extends SimpleUrlAuthenticationSuccessHandler {
    public EduAuthSuccsessHandler(){
        super("/shibboleth");
    }
}