package org.edu_sharing.spring.security.basic;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.edu_sharing.spring.security.oauth2.SilentLoginAuthorizationRequestResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/**
 *  redirects to /shibboleth after successfull auth
 *
 *  notice:
 *  SavedRequestAwareAuthenticationSuccessHandler is default which always redircts to /samllogout url with GET which is not vailable
 *      * after logout/login again
 */
@Component
public class EduAuthSuccsessHandler extends SimpleUrlAuthenticationSuccessHandler {
    public EduAuthSuccsessHandler(){
        super("/shibboleth");
    }

    @Override
    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response) {
        return super.determineTargetUrl(request, response);
    }
}
