package org.edu_sharing.spring.security.openid;

import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

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
            e.printStackTrace();
        }

        if(exception instanceof OAuth2AuthenticationException && ((OAuth2AuthenticationException) exception).getError() != null) {
            String error = ((OAuth2AuthenticationException) exception).getError().getErrorCode();
            if(Objects.equals("authorization_request_not_found",error)) {

                String redirect = "/edu-sharing";
                HttpSession session = request.getSession(false);
                log.warn("oauth error {} can't find request in session. redirect to {}", error,redirect);
                if(session != null) {
                    if(log.isDebugEnabled()) {
                        session.getAttributeNames().asIterator().forEachRemaining(name -> log.debug("session contains attribute {} value: {}", name, session.getAttribute(name)));
                    }
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
