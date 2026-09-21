package org.edu_sharing.spring.security.basic;

import com.typesafe.config.Config;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.csrf.*;
import org.springframework.util.Assert;

public class CSRFConfig {

    static Config config = LightbendConfigLoader.get();

    public static EduSessionAuthenticationStrategy eduSessionAuthenticationStrategy;

    public static HttpSecurity config(HttpSecurity http) throws Exception{

        if(config.hasPath("security.sso.disableCsrf") && config.getBoolean("security.sso.disableCsrf")){
            http.csrf(AbstractHttpConfigurer::disable);
        }else {

            CookieCsrfTokenRepository tokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
            CsrfTokenRequestHandler requestHandler = new CsrfTokenRequestAttributeHandler();

            eduSessionAuthenticationStrategy = new EduSessionAuthenticationStrategy(tokenRepository,requestHandler);

            http.csrf((csrf) -> csrf
                    .sessionAuthenticationStrategy(eduSessionAuthenticationStrategy)
                    .csrfTokenRepository(tokenRepository)
                    .csrfTokenRequestHandler(requestHandler)
                    .ignoringRequestMatchers(
                            ApplicationInfoList::isInternalPortRequest,
                            // stateless clients (scripts, server-to-server, token clients) send an
                            // auth header but NO session cookie -> there is no ambient authority a
                            // CSRF attack could ride on, so CSRF protection is not needed.
                            request -> request.getHeader("Authorization") != null
                                    && request.getRequestedSessionId() == null));
        }
        return http;
    }

    public static void csrfInitCookie(HttpServletRequest request, HttpServletResponse response){
        if(eduSessionAuthenticationStrategy != null
                && config.hasPath("security.sso.disableCsrf")
                && !config.getBoolean("security.sso.disableCsrf")){
            eduSessionAuthenticationStrategy.onAuthentication(null,request,response);
        }
    }


    /**
     * XSRF-TOKEN cookie is never created, even when a initial post request is send to CsrfFilter.
     * The cookie is created in CsrfFilter v6.2.4 z120 but never comes to client.
     * Maybe the problem is the 403 of the post process kicks the cookie out somewhere
     * The 403 in CsrfFilter is cause of comparing tokenvalue (initial none) send by header or param
     * compared to the newly generated token (CsrfFilter v6.2.4 z121).
     *
     * this article describes a similar problem
     * https://medium.com/@thecodinganalyst/configure-spring-security-csrf-for-testing-on-swagger-e9e6461ee0c1
     *
     * inspired by
     *  org.springframework.security.web.csrf.CsrfAuthenticationStrategy
     */
    private static class EduSessionAuthenticationStrategy implements SessionAuthenticationStrategy{

        private final Log logger = LogFactory.getLog(getClass());

        private final CsrfTokenRepository tokenRepository;

        private CsrfTokenRequestHandler requestHandler;

        /**
         * Creates a new instance
         * @param tokenRepository the {@link CsrfTokenRepository} to use
         */
        public EduSessionAuthenticationStrategy(CsrfTokenRepository tokenRepository, CsrfTokenRequestHandler requestHandler ) {
            Assert.notNull(tokenRepository, "tokenRepository cannot be null");
            this.tokenRepository = tokenRepository;
            this.requestHandler = requestHandler;
        }

        @Override
        public void onAuthentication(Authentication authentication, HttpServletRequest request, HttpServletResponse response) throws SessionAuthenticationException {
            /*
             * Do NOT use the CsrfAuthenticationStrategy approach of
             * saveToken(null, ...) + loadDeferredToken(...): saveToken(null, ...) writes an EMPTY
             * XSRF-TOKEN cookie (max-age 0) and the deferred token that should replace it is lazy -
             * CsrfTokenRequestAttributeHandler#handle only stores a SupplierCsrfToken in the request
             * attributes, and nothing else in the request reads it. The response then carries only
             * the deletion cookie, the client ends up without an XSRF-TOKEN cookie, sends no
             * X-XSRF-TOKEN header on the next request and gets a 403.
             *
             * Instead generate the (rotated) token eagerly and save it, so the response always
             * carries exactly one cookie holding a usable token value.
             */
            CsrfToken token = this.tokenRepository.generateToken(request);
            this.tokenRepository.saveToken(token, request, response);
            this.requestHandler.handle(request, response, () -> token);
            this.logger.debug("Replaced CSRF Token");
        }
    }

}
