package org.edu_sharing.spring.security.openid;

import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
        log.info("authentication failure", exception);
        try {
            if(SilentLoginModeRedirect.processError(request,response)){
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(exception instanceof OAuth2AuthenticationException && ((OAuth2AuthenticationException) exception).getError() != null) {
            if(Objects.equals("authorization_request_not_found", ((OAuth2AuthenticationException) exception).getError().getErrorCode())) {
                log.warn("oauth authorization_request_not_found can't find request in session. redirect to /edu-sharing/sso");
                response.sendRedirect("/edu-sharing/sso");
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
