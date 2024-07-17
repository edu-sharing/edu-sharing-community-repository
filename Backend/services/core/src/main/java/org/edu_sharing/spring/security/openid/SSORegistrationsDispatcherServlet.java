package org.edu_sharing.spring.security.openid;

import io.opentelemetry.api.internal.StringUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.policy.NodeCustomizationPolicies;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.spring.ApplicationContextFactory;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

import java.io.IOException;

public class SSORegistrationsDispatcherServlet extends HttpServlet {

    Logger logger = Logger.getLogger(SSORegistrationsDispatcherServlet.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ClientRegistrationRepository clientRegistrationRepository = (ClientRegistrationRepository)
                ApplicationContextFactory.getApplicationContext().getBean("clientRegistrationRepository");

        String registrationId = NodeCustomizationPolicies.getEduSharingContext();
        if(StringUtils.isNullOrEmpty(registrationId) || registrationId.equals(CCConstants.EDUCONTEXT_DEFAULT)){
            registrationId = SecurityConfigurationOpenIdConnect.DEFAULT_REGISTRATION_ID;
        }

        ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId(registrationId);
        if(clientRegistration == null){
            String message = "Client registration not found";
            logger.error(message);
            throw new ServletException(message);
        }

        String redirectPath = "/edu-sharing/oauth2/authorization/" + clientRegistration.getRegistrationId();
        resp.sendRedirect(redirectPath);
    }
}
