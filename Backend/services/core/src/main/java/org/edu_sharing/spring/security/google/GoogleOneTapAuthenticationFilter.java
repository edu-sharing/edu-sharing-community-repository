package org.edu_sharing.spring.security.google;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Slf4j
public class GoogleOneTapAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public GoogleOneTapAuthenticationFilter() {
        super("/login/google"); // Endpoint
    }

    @Override
    public GoogleOneTapAuthenticationToken attemptAuthentication(
            HttpServletRequest request, HttpServletResponse response) {

        try {
            JsonNode json = objectMapper.readTree(request.getInputStream());
            String credential = json.get("credential").asText();

            GoogleOneTapAuthenticationToken authRequest =
                    new GoogleOneTapAuthenticationToken(credential);

            return (GoogleOneTapAuthenticationToken)
                    this.getAuthenticationManager().authenticate(authRequest);

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new AuthenticationServiceException("Could not parse Google credential", e);
        }
    }
}

