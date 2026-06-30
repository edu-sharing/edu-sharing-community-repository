package org.alfresco.repo.webdav.auth;

import com.typesafe.config.Config;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.UserTransaction;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationException;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.web.filter.beans.DependencyInjectedFilter;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.NoSuchPersonException;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.security.HMac;
import org.springframework.context.ApplicationContext;

import javax.naming.CommunicationException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Common base for the edu-sharing WebDAV authentication filters.
 *
 * <p>This class contains everything that is <b>not</b> specific to a concrete authentication
 * backend: the HTTP request handling (session lookup, BASIC authorization header decoding with the
 * ALF-13621 encoding fallback, ticket based authentication, the {@code WWW-Authenticate} challenge),
 * the edu-sharing person-active-status check and the construction of the {@link WebDAVUser}.</p>
 *
 * <p>A concrete backend only has to implement {@link #searchForUser(String, String)} (verify the
 * credentials and set the current Alfresco user) and may optionally hook into {@link #initBackend(Config)},
 * {@link #destroy()} and {@link #recoverFromCommunicationFailure()}.</p>
 *
 * @see LDAPAuthenticationFilter
 * @see IDMRestAuthenticationFilter
 */
public abstract class AbstractWebDAVAuthenticationFilter implements Filter, DependencyInjectedFilter {

    private static final String WELCOME = "WebDAV Server";

    /** Common prefix of every lightbend config key read by a WebDAV authentication filter. */
    protected static final String INIT_CONFIG_BASE = "repository.webdav.authentication.";

    // Allow an authentication ticket to be passed as part of a request to bypass authentication
    private static final String ARG_TICKET = "ticket";
    private static final String PPT_EXTN = ".ppt";
    private static final String VTI_IGNORE = "&vtiIgnore";

    protected final Log logger = LogFactory.getLog(getClass());

    protected final Config eduConfig = LightbendConfigLoader.get();

    // Servlet context
    private ServletContext m_context;

    // Various services
    protected AuthenticationService m_authService;
    protected PersonService m_personService;
    protected NodeService m_nodeService;
    protected TransactionService m_transactionService;

    protected HMac hMac = null;

    /**
     * edu-sharing fix from 4.2.f
     *
     * ALF-13621: Due to browser inconsistencies we have to try a fallback path of encodings
     */
    /** The password encodings to try in priority order **/
    private static final String[] ENCODINGS = new String[]{
            "UTF-8",
            System.getProperty("file.encoding"),
            "ISO-8859-1"
    };

    /** Corresponding array of CharsetDecoders with CodingErrorAction.REPORT. Duplicates removed. */
    private static final CharsetDecoder[] DECODERS;

    static {
        Map<String, CharsetDecoder> decoders = new LinkedHashMap<>(ENCODINGS.length * 2);
        for (String encoding : ENCODINGS) {
            if (!decoders.containsKey(encoding)) {
                decoders.put(encoding, Charset.forName(encoding).newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT));
            }
        }
        DECODERS = new CharsetDecoder[decoders.size()];
        decoders.values().toArray(DECODERS);
    }

    /**
     * Initialize the filter.
     *
     * @param config FilterConfig
     * @throws ServletException on initialization failure
     */
    @Override
    public void init(FilterConfig config) throws ServletException {
        logger.info(getClass().getCanonicalName());

        // Save the context
        this.m_context = config.getServletContext();

        // Setup the authentication context
        ApplicationContext context = AlfAppContextGate.getApplicationContext();
        ServiceRegistry serviceRegistry = (ServiceRegistry) context.getBean(ServiceRegistry.SERVICE_REGISTRY);

        this.m_nodeService = serviceRegistry.getNodeService();
        this.m_authService = serviceRegistry.getAuthenticationService();
        this.m_transactionService = serviceRegistry.getTransactionService();
        this.m_personService = (PersonService) context.getBean("PersonService");   // transactional and permission-checked

        this.hMac = HMac.getInstance();

        // backend specific initialization
        initBackend(eduConfig);
    }

    /**
     * Backend specific initialization, called at the end of {@link #init(FilterConfig)}.
     * The common services and {@link #hMac} are already set up at this point.
     *
     * @param eduConfig the resolved lightbend config
     * @throws ServletException on initialization failure
     */
    protected abstract void initBackend(Config eduConfig) throws ServletException;

    @Override
    public void destroy() {
        // default: nothing to clean up, backends may override
    }

    @Override
    public void doFilter(ServletContext servletContext, ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        doFilter(servletRequest, servletResponse, filterChain);
    }

    /**
     * Run the authentication filter.
     *
     * @param req   ServletRequest
     * @param resp  ServletResponse
     * @param chain FilterChain
     * @throws ServletException on processing failure
     * @throws IOException      on I/O failure
     */
    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
        // Assume it's an HTTP request
        HttpServletRequest httpReq = (HttpServletRequest) req;
        HttpServletResponse httpResp = (HttpServletResponse) resp;

        // Get the user details object from the session
        WebDAVUser user = (WebDAVUser) httpReq.getSession().getAttribute(BaseAuthenticationFilter.AUTHENTICATION_USER);

        if (user == null) {
            // Get the authorization header
            String authHdr = httpReq.getHeader("Authorization");
            logger.debug("user == null authHdr:" + authHdr);
            if ((authHdr != null) && (authHdr.length() > 5) && authHdr.substring(0, 5).equalsIgnoreCase("BASIC")) {
                user = authenticateWithBasicAuth(httpReq, authHdr);
            } else {
                user = authenticateWithTicket(httpReq, req);
            }

            // Check if the user is authenticated, if not then prompt again
            if (user == null) {
                // No user/ticket, force the client to prompt for logon details
                challenge(httpResp);
                return;
            }
        } else {
            logger.debug("user != null :" + user.getTicket());
            try {
                // Setup the authentication context
                this.m_authService.validate(user.getTicket());
            } catch (Exception ex) {
                // No user/ticket, force the client to prompt for logon details
                challenge(httpResp);
                return;
            }
        }

        // Chain other filters
        chain.doFilter(req, resp);
    }

    /**
     * Decode the BASIC authorization header and authenticate against the backend.
     *
     * <p>ALF-13621: Due to browser inconsistencies we have to try a fallback path of encodings.</p>
     */
    private WebDAVUser authenticateWithBasicAuth(HttpServletRequest httpReq, String authHdr) {
        if (logger.isDebugEnabled())
            logger.debug("Basic authentication details present in the header.");
        byte[] encodedString = java.util.Base64.getDecoder().decode(authHdr.substring(5).trim().getBytes());

        WebDAVUser user = null;
        Set<String> attemptedAuths = new HashSet<>(DECODERS.length * 2);
        for (CharsetDecoder decoder : DECODERS) {
            try {
                // Attempt to decode using this charset
                String basicAuth = decoder.decode(ByteBuffer.wrap(encodedString)).toString();

                // It decoded OK but we may already have tried this string.
                if (!attemptedAuths.add(basicAuth)) {
                    // Already tried - no need to try again
                    continue;
                }

                // Split the username and password
                String username;
                String password;
                int pos = basicAuth.indexOf(":");
                if (pos != -1) {
                    username = basicAuth.substring(0, pos);
                    password = basicAuth.substring(pos + 1);
                } else {
                    username = basicAuth;
                    password = "";
                }

                // Authenticate the user
                try {
                    logger.info("webdav authentication: starting. loginName:" + hMac.calculateHmac(username));
                    user = searchForUser(username, password);
                } catch (CommunicationException e) {
                    logger.error(e.getMessage() + " Will recover the backend connection and retry.");
                    try {
                        recoverFromCommunicationFailure();
                        user = searchForUser(username, password);
                    } catch (CommunicationException ce) {
                        logger.error(e.getMessage() + " still occurs will give up. maybe restart alfresco.");
                    }
                } catch (NoSuchPersonException e) {
                    logger.error("person does not exist in alfresco");
                }

                if (user != null) {
                    httpReq.getSession().setAttribute(BaseAuthenticationFilter.AUTHENTICATION_USER, user);
                    // Success so break out
                    break;
                }
            } catch (CharacterCodingException e) {
                if (logger.isDebugEnabled())
                    logger.debug("Didn't decode using " + decoder.getClass().getName(), e);
            } catch (AuthenticationException ex) {
                if (logger.isDebugEnabled())
                    logger.debug("Authentication error ", ex);
            } catch (NoSuchPersonException e) {
                if (logger.isDebugEnabled())
                    logger.debug("There is no such person error ", e);
            }
        }
        return user;
    }

    /**
     * Authenticate via an Alfresco authentication ticket passed as a request parameter.
     */
    private WebDAVUser authenticateWithTicket(HttpServletRequest httpReq, ServletRequest req) {
        String ticket = req.getParameter(ARG_TICKET);
        logger.debug("auth by ticket:" + ticket);

        if (ticket == null || ticket.isEmpty()) {
            return null;
        }

        // PowerPoint bug fix
        if (ticket.endsWith(PPT_EXTN)) {
            ticket = ticket.substring(0, ticket.length() - PPT_EXTN.length());
        }

        // vtiIgnore argument may find its way onto the ticket due to a double-encoding issue with Office
        if (ticket.endsWith(VTI_IGNORE)) {
            ticket = ticket.substring(0, ticket.length() - VTI_IGNORE.length());
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Logon via ticket from " + req.getRemoteHost() + " (" +
                    req.getRemoteAddr() + ":" + req.getRemotePort() + ")" + " ticket=" + ticket);
        }

        WebDAVUser user = null;
        UserTransaction tx = null;
        try {
            // Validate the ticket
            this.m_authService.validate(ticket);

            // Need to create the User instance if not already available
            String currentUsername = this.m_authService.getCurrentUserName();

            // Start a transaction
            tx = this.m_transactionService.getUserTransaction();
            tx.begin();

            NodeRef personRef = this.m_personService.getPerson(currentUsername);
            user = new WebDAVUser(currentUsername, this.m_authService.getCurrentTicket(), personRef);
            NodeRef homeRef = (NodeRef) this.m_nodeService.getProperty(personRef, ContentModel.PROP_HOMEFOLDER);

            // Check that the home space node exists - else Login cannot proceed
            if (!this.m_nodeService.exists(homeRef)) {
                throw new InvalidNodeRefException(homeRef);
            }
            user.setHomeNode(homeRef);

            tx.commit();
            tx = null;

            // Store the User object in the Session - the authentication servlet will then proceed
            httpReq.getSession().setAttribute(BaseAuthenticationFilter.AUTHENTICATION_USER, user);
        } catch (Throwable e) {
            // Clear the user object to signal authentication failure
            user = null;
        } finally {
            try {
                if (tx != null) {
                    tx.rollback();
                }
            } catch (Exception tex) {
                // ignore
            }
        }
        return user;
    }

    /**
     * Send the BASIC {@code WWW-Authenticate} challenge to force the client to prompt for credentials.
     */
    private void challenge(HttpServletResponse httpResp) throws IOException {
        httpResp.setHeader("WWW-Authenticate", "BASIC realm=\"" + WELCOME + "\"");
        httpResp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        httpResp.flushBuffer();
    }

    /**
     * Verify the given credentials against the concrete backend and, on success, set the current
     * Alfresco user and return the resulting {@link WebDAVUser}. Returns {@code null} on failure.
     *
     * @param loginName the login name as sent by the client
     * @param password  the password as sent by the client
     * @return the authenticated {@link WebDAVUser} or {@code null} if authentication failed
     * @throws CommunicationException if the backend connection failed and a retry (after
     *                                {@link #recoverFromCommunicationFailure()}) should be attempted
     */
    protected abstract WebDAVUser searchForUser(String loginName, String password) throws CommunicationException;

    /**
     * Hook invoked after {@link #searchForUser(String, String)} threw a {@link CommunicationException},
     * before the single retry. Backends with a persistent connection (e.g. LDAP) re-establish it here.
     * The default implementation does nothing.
     *
     * @throws CommunicationException if recovery itself failed permanently
     */
    protected void recoverFromCommunicationFailure() throws CommunicationException {
        // default: stateless backend, nothing to recover
    }

    /**
     * edu-sharing customization: verify that the person is not blocked according to the
     * {@code repository.personActiveStatus} config.
     *
     * @param username  the resolved Alfresco authority name
     * @param loginName the original login name (only used for hashed logging)
     * @throws AuthenticationException if the person is blocked
     */
    protected void assertPersonActive(String username, String loginName) {
        boolean allowed = AuthenticationUtil.runAsSystem(() -> {
            NodeRef personRef = this.m_personService.getPerson(username, false);
            Config config = LightbendConfigLoader.get();
            if (!config.getIsNull("repository.personActiveStatus")) {
                String personActiveStatus = config.getString("repository.personActiveStatus");
                String personStatus = (String) this.m_nodeService.getProperty(personRef, QName.createQName(CCConstants.CM_PROP_PERSON_ESPERSONSTATUS));
                if (!personActiveStatus.equals(personStatus)) {
                    logger.info("personActiveStatus mismatch. " + personStatus + " vs " + personActiveStatus);
                    return false;
                }
            }
            return true;
        });
        if (!allowed) {
            throw new AuthenticationException("webdav authentication: USER_BLOCKED. loginName:" + hMac.calculateHmac(loginName) + " / userName:" + hMac.calculateHmac(username));
        }
    }

    /**
     * Build the {@link WebDAVUser} for the user that is currently set in the Alfresco security
     * context (i.e. after the backend has authenticated and set the current user).
     */
    protected WebDAVUser buildWebDAVUserForCurrentUser() {
        String username = this.m_authService.getCurrentUserName();
        NodeRef personNodeRef = this.m_personService.getPerson(username);
        NodeRef homeSpaceRef = (NodeRef) this.m_nodeService.getProperty(personNodeRef, ContentModel.PROP_HOMEFOLDER);
        return new WebDAVUser(username, this.m_authService.getCurrentTicket(), homeSpaceRef);
    }
}
