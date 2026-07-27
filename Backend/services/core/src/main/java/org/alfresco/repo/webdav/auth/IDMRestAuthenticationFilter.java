package org.alfresco.repo.webdav.auth;

import com.typesafe.config.Config;
import jakarta.servlet.ServletException;
import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.edu_sharing.alfresco.service.search.CMISSearchHelper;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.springframework.context.ApplicationContext;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * WebDAV authentication filter that validates credentials against the
 * <a href="file:openapi.yaml">logineo-idm-rest</a> service.
 *
 * <p>The actual HTTP exchange is delegated to {@link IDMRestClient}; this class only wires the
 * lightbend config, maps the {@link IDMRestClient.Result} to the WebDAV authentication flow and –
 * on success – resolves the principal to an Alfresco authority and sets the current user.</p>
 *
 * <p>The IDM-REST service only verifies the password; it does not map the principal to an Alfresco
 * authority. The principal name sent by the client is therefore used directly as the Alfresco
 * authority name, mirroring how {@link LDAPAuthenticationFilter} sets the current user.</p>
 *
 * <p>All request handling that is not IDM specific lives in {@link AbstractWebDAVAuthenticationFilter}.</p>
 */
public class IDMRestAuthenticationFilter extends AbstractWebDAVAuthenticationFilter {

    // init params
    private static final String INIT_IDM_URL = INIT_CONFIG_BASE + "idmRest.url";
    private static final String INIT_IDM_USER = INIT_CONFIG_BASE + "idmRest.user";
    private static final String INIT_IDM_PASSWORD = INIT_CONFIG_BASE + "idmRest.password";
    private static final String INIT_IDM_TIMEOUT = INIT_CONFIG_BASE + "idmRest.timeoutSeconds";

    private IDMRestClient idmRestClient;

    public IDMRestAuthenticationFilter() {
        System.out.println("TEST IDMRestAuthenticationFilter");
    }

    @Override
    protected void initBackend(Config eduConfig) throws ServletException {
        String url = eduConfig.getString(INIT_IDM_URL);
        String user = eduConfig.getString(INIT_IDM_USER);
        String password = eduConfig.getString(INIT_IDM_PASSWORD);
        long timeoutSeconds = eduConfig.getIsNull(INIT_IDM_TIMEOUT) ? 10 : eduConfig.getLong(INIT_IDM_TIMEOUT);

        this.idmRestClient = new IDMRestClient(url, user, password, Duration.ofSeconds(timeoutSeconds));
    }

    @Override
    protected WebDAVUser searchForUser(String loginName, String password) {
        IDMRestClient.Result result = idmRestClient.authenticate(loginName, password);
        switch (result) {
            case AUTHENTICATED:
                break;
            case REJECTED:
                // wrong password / blocked / unknown principal – deliberately indistinguishable
                logger.info("webdav idm-rest authentication: failed (422). loginName:" + hMac.calculateHmac(loginName));
                return null;
            case TECHNICAL_AUTH_FAILED:
                logger.error("webdav idm-rest authentication: technical user rejected (401), check " + INIT_IDM_USER + "/" + INIT_IDM_PASSWORD);
                return null;
            case BAD_REQUEST:
                logger.error("webdav idm-rest authentication: bad request (400). loginName:" + hMac.calculateHmac(loginName));
                return null;
            case ERROR:
            default:
                logger.error("webdav idm-rest authentication: request to " + idmRestClient.getAuthenticateUrl() + " failed. loginName:" + hMac.calculateHmac(loginName));
                return null;
        }

        // The IDM-REST service only verifies the password. The principal name email is mapped on alfresco username
        String username = AuthenticationUtil.runAsSystem(() -> {
            List<NodeRef> nodeRefs = CMISSearchHelper.fetchNodesByTypeAndFilters(CCConstants.CM_TYPE_PERSON, Map.of(CCConstants.CM_PROP_PERSON_EMAIL, loginName));
            if (nodeRefs == null || nodeRefs.isEmpty()) {
                logger.error("webdav idm-rest authentication: person does not exist in alfresco. loginName:" + hMac.calculateHmac(loginName));
                return null;
            }
            String u =  (String)this.m_nodeService.getProperty(nodeRefs.get(0), QName.createQName(CCConstants.CM_PROP_PERSON_USERNAME));
            boolean exists = AuthenticationUtil.runAsSystem(() -> this.m_personService.personExists(u));
            if (!exists) {
                logger.error("webdav idm-rest authentication: person does not exist in alfresco. loginName:" + hMac.calculateHmac(loginName));
                return null;
            }
            return u;
        });

        if(username == null) {
            logger.info("could not resolve userName for: " + hMac.calculateHmac(loginName));
            return null;
        }


        // edu-sharing customization: block check
        assertPersonActive(username, loginName);

        // set the current Alfresco user, mirroring LDAPAuthenticationFilter#authenticate
        ApplicationContext context = AlfAppContextGate.getApplicationContext();
        AuthenticationComponent authComp = (AuthenticationComponent) context.getBean("authenticationComponent");
        authComp.setCurrentUser(username);
        logger.info("webdav idm-rest authentication: sucessfull. loginName:" + hMac.calculateHmac(loginName) + " / userName:" + hMac.calculateHmac(username));

        // Setup User object and Home space ID etc.
        return buildWebDAVUserForCurrentUser();
    }
}
