package org.edu_sharing.spring.security.saml2;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.extensions.surf.util.URLEncoder;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;

import java.io.IOException;

public class EduFailureHandler extends SimpleUrlAuthenticationFailureHandler {
    public EduFailureHandler() {
        this.setDefaultFailureUrl("/login?error");
    }
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        if(exception instanceof Saml2AuthenticationException && exception.getMessage() != null
                && exception.getMessage().contains("The response contained an InResponseTo attribute")
                && exception.getMessage().contains("but no saved authentication request was found")) {
            logger.warn("initial saml session is broken. doing redirect. The response contained an InResponseTo attribute ... but no saved authentication request was found");
            response.sendRedirect("/edu-sharing/shibboleth");
            return;
        }

        logger.error(exception.getMessage(), exception);
        String message = "SSO_UNKNOWN_ERROR";
        message = URLEncoder.encode(message.trim());
        response.sendRedirect("/edu-sharing/components/error/"+message+"/"+message);
    }
}
