package org.edu_sharing.spring.security.oauth2;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.edu_sharing.alfresco.policy.NodeCustomizationPolicies;
import org.edu_sharing.spring.security.oauth2.config.OAuth2ClientProperties;
import org.edu_sharing.spring.security.oauth2.config.OAuth2ConfigProvider;
import org.edu_sharing.spring.servlet.SpringHttpServlet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

import java.io.IOException;
import java.util.Iterator;

@Slf4j
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public class SSORegistrationsDispatcherServlet extends SpringHttpServlet {

    @Autowired
    private ClientRegistrationRepository clientRegistrationRepository;

    @Autowired
    private OAuth2ConfigProvider OAuth2ConfigProvider;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        String registrationId = req.getParameter("id");
        String eduSharingContext = NodeCustomizationPolicies.getEduSharingContext();

        if (eduSharingContext != null && registrationId != null && !registrationId.startsWith(eduSharingContext)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "id is not valid for context: " + eduSharingContext + ".");
            return;
        }

        if (registrationId == null) {
            OAuth2ClientProperties config = OAuth2ConfigProvider.getConfig(eduSharingContext);
            if (config.getRegistration().isEmpty()) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "no id found.");
                return;
            }

            if (config.getRegistration().size() > 1) {
                log.warn("id is required for multiple registrations. redirecting to components/login");
                //resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "id is required for multiple registrations.");
                resp.sendRedirect("/edu-sharing/components/login");
                return;
            }

            registrationId = config.getRegistrationId(config.getRegistration().keySet().iterator().next());
        }


        ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId(registrationId);
        if (clientRegistration == null) {
            String message = "Client registration not found:" + registrationId + ". check lightbend context config.";
            log.warn(message);

            InMemoryClientRegistrationRepository cr = ((InMemoryClientRegistrationRepository) clientRegistrationRepository);
            Iterator<ClientRegistration> iterator = cr.iterator();
            if (iterator.hasNext()) {
                clientRegistration = iterator.next();
                log.warn("using fallback: {}", clientRegistration.getRegistrationId());
            } else {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, message);
                return;
            }
        }

        String redirectPath = "/edu-sharing/oauth2/authorization/" + clientRegistration.getRegistrationId();
        resp.sendRedirect(redirectPath);
    }
}
