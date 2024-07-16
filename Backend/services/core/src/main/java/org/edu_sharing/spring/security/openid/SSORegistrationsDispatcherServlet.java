package org.edu_sharing.spring.security.openid;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import org.edu_sharing.spring.ApplicationContextFactory;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

import java.io.IOException;
import java.util.Iterator;

public class SSORegistrationsDispatcherServlet extends HttpServlet {

    Logger logger = Logger.getLogger(SSORegistrationsDispatcherServlet.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ClientRegistrationRepository clientRegistrationRepository = (ClientRegistrationRepository)
                ApplicationContextFactory.getApplicationContext().getBean("clientRegistrationRepository");
        Iterator<ClientRegistration> iterator = ((InMemoryClientRegistrationRepository) clientRegistrationRepository).iterator();
        while (iterator.hasNext()) {
            ClientRegistration clientRegistration = iterator.next();
            if(!iterator.hasNext()){
                String redirectPath = "/edu-sharing/oauth2/authorization/" + clientRegistration.getRegistrationId();
                resp.sendRedirect(redirectPath);
                return;
            }
        }
        logger.error("no registration found");
    }
}
