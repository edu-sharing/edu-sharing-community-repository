package org.edu_sharing.spring.security.oauth2;

import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.savedrequest.DefaultSavedRequest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class CustomErrorHandler implements AuthenticationFailureHandler {
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        log.info("authentication failure. url: {}", request.getRequestURI(), exception);
        try {
            if(SilentLoginModeRedirect.processError(request,response)){
                return;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        if(exception instanceof OAuth2AuthenticationException && ((OAuth2AuthenticationException) exception).getError() != null) {
            String error = ((OAuth2AuthenticationException) exception).getError().getErrorCode();
            log.warn("oauth error {}.", error);
            HttpSession session = request.getSession(false);
            if(session != null) {
                if(log.isDebugEnabled()) {
                    session.getAttributeNames().asIterator().forEachRemaining(name -> log.debug("session contains attribute {} value: {}", name, session.getAttribute(name)));
                    DefaultSavedRequest defaultSavedRequest = (DefaultSavedRequest)session.getAttribute("SPRING_SECURITY_SAVED_REQUEST");
                    if(defaultSavedRequest != null) {
                        log.debug("defaultSavedRequest redirect url: " + defaultSavedRequest.getRedirectUrl());
                    }
                }
            }
            if(Objects.equals("authorization_request_not_found",error)) {
                String redirect = "/edu-sharing";
                log.warn("redirect to {}", redirect);
                if(session != null) {
                    log.warn("invalidating incomplete session before redirect");
                    session.invalidate();
                }
                response.sendRedirect(redirect);
                return;
            }
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");

        Map<String, String> data = new HashMap<>();
        data.put("error", "login_required");
        data.put("message", "Login is required to access this resource.");
        response.getOutputStream().println(new Gson().toJson(data));
    }
}
