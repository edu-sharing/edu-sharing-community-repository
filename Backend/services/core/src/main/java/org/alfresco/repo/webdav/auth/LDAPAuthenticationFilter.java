package org.alfresco.repo.webdav.auth;

import com.typesafe.config.Config;
import jakarta.servlet.ServletException;
import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.alfresco.repo.security.authentication.AuthenticationException;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.spring.conditions.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.naming.CommunicationException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import java.util.Properties;

/**
 * WebDAV authentication filter that validates credentials against an LDAP directory.
 *
 * <p>All the request handling that is not LDAP specific lives in
 * {@link AbstractWebDAVAuthenticationFilter}; this class only contains the directory lookup and
 * the LDAP bind.</p>
 */
public class LDAPAuthenticationFilter extends AbstractWebDAVAuthenticationFilter {

    // init params
    private static final String INIT_LDAP_URI = INIT_CONFIG_BASE + "ldap.uri";
    private static final String INIT_LDAP_BASE = INIT_CONFIG_BASE + "ldap.base";
    private static final String INIT_LDAP_SEC_AUTH = INIT_CONFIG_BASE + "ldap.sec.auth";
    private static final String INIT_LDAP_SEC_USER = INIT_CONFIG_BASE + "ldap.sec.user";
    private static final String INIT_LDAP_SEC_PWD = INIT_CONFIG_BASE + "ldap.sec.pwd";

    private static final String INIT_LDAP_FROM = INIT_CONFIG_BASE + "ldap.from";
    private static final String INIT_LDAP_TO = INIT_CONFIG_BASE + "ldap.to";
    private static final String INIT_LDAP_UID = INIT_CONFIG_BASE + "ldap.uid";

    /**
     * edu-sharing customization
     */
    private static final String INIT_USE_ALFRESCO_AUTHENTICATION_COMPONENT = INIT_CONFIG_BASE + "ldap.alfrescoAuthComponent";

    private DirContext jndi;

    private String ldapFrom;
    private String ldapTo;

    private boolean useAlfrescoAuthenticationComponent = false;
    private String ldapBase = null;

    //rember the env global
    private Properties env = null;
    private String ldapUidProp = null;
    private String ldapUrl = null;

    @Override
    protected void initBackend(Config eduConfig) throws ServletException {
        this.ldapBase = eduConfig.getString(INIT_LDAP_BASE);
        this.ldapUrl = eduConfig.getString(INIT_LDAP_URI);
        this.ldapFrom = eduConfig.getString(INIT_LDAP_FROM);
        this.ldapTo = eduConfig.getString(INIT_LDAP_TO);
        this.ldapUidProp = eduConfig.getString(INIT_LDAP_UID);
        if (this.ldapUidProp == null || this.ldapUidProp.trim().isEmpty()) this.ldapUidProp = "uid";

        env = new Properties();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        //edu-sharing
        useAlfrescoAuthenticationComponent = eduConfig.getBoolean(INIT_USE_ALFRESCO_AUTHENTICATION_COMPONENT);
        env.put(Context.PROVIDER_URL, this.ldapUrl + "/" + this.ldapBase);
        env.put(Context.SECURITY_AUTHENTICATION, eduConfig.getString(INIT_LDAP_SEC_AUTH));
        env.put(Context.SECURITY_PRINCIPAL, eduConfig.getString(INIT_LDAP_SEC_USER));
        env.put(Context.SECURITY_CREDENTIALS, eduConfig.getString(INIT_LDAP_SEC_PWD));
    }

    @Override
    public void destroy() {
        if (this.jndi != null) {
            try {
                this.jndi.close();
            } catch (NamingException e) {
                // ignore
            }
        }
    }

    @Override
    protected void recoverFromCommunicationFailure() throws CommunicationException {
        try {
            this.jndi = new InitialDirContext(env);
        } catch (CommunicationException ce) {
            throw ce;
        } catch (NamingException ne) {
            logger.error(ne.getMessage(), ne);
        }
    }

    /**
     * search for user encapsulated in a method to catch a potential CommunicationException and retry
     *
     * https://issues.apache.org/jira/browse/HADOOP-9125
     * https://issues.apache.org/jira/secure/attachment/12560771/HADOOP-9125.patch
     */
    @Override
    protected WebDAVUser searchForUser(String loginName, String password) throws CommunicationException {
        // lazily (re)create the directory context, mirroring the original behaviour
        if (this.jndi == null) {
            try {
                this.jndi = new InitialDirContext(env);
            } catch (CommunicationException ce) {
                throw ce;
            } catch (NamingException e) {
                logger.error(e.getMessage(), e);
                return null;
            }
        }

        String uid = null;
        String dn = null;
        String username = loginName;

        try {
            SearchControls ctls = new SearchControls();

            ctls.setSearchScope(SearchControls.ONELEVEL_SCOPE);
            ctls.setReturningAttributes(new String[]{this.ldapTo, this.ldapUidProp});

            String base = "";
            String query = "(" + this.ldapFrom + "=" + username + ")";
            logger.debug("query:" + query);
            NamingEnumeration<SearchResult> rs;

            rs = this.jndi.search(base, query, ctls);

            if (rs.hasMore()) {
                SearchResult r = rs.next();

                Attribute attr = r.getAttributes().get(this.ldapTo);
                if (attr != null) {
                    username = (String) attr.get();
                }

                Attribute uidAttr = r.getAttributes().get(this.ldapUidProp);
                if (uidAttr != null) {
                    uid = (String) uidAttr.get();
                }

                dn = r.getNameInNamespace();
            } else {
                throw new AuthenticationException("webdav ldap authentication: user not found in directory. loginName:" + hMac.calculateHmac(loginName));
            }
            rs.close();

            logger.debug("query:" + query + " new username:" + username);
            //edu-sharing customization
            if (username != null) {
                assertPersonActive(username, loginName);
            }

            if (useAlfrescoAuthenticationComponent) {
                this.m_authService.authenticate(username, password.toCharArray());
            } else {
                logger.debug("using ldap auth dn:" + dn + " uid:" + uid + " username:" + username);
                this.authenticate(dn, username, password, loginName);
            }

            // Setup User object and Home space ID etc.
            return buildWebDAVUserForCurrentUser();

        } catch (CommunicationException ce) {
            throw ce;
        } catch (NamingException e) {
            // Do nothing, user object will be null
            logger.error(e.getMessage(), e);
        } catch (AuthenticationException ex) {
            // Do nothing, user object will be null
            if (ex.getMessage() != null && ex.getMessage().contains("Invalid Credentials")) {
                logger.error("webdav ldap authentication: failed with Invalid Credentials. loginName:" + hMac.calculateHmac(loginName) + " / userName:" + hMac.calculateHmac(username));
            }
            if (ex.getMessage() != null && ex.getMessage().contains("DN with no password")) {
                logger.error("webdav ldap authentication: no password provided. loginName:" + hMac.calculateHmac(loginName) + " / userName:" + hMac.calculateHmac(username));
            } else {
                logger.error(ex.getMsgId());
                if (logger.isDebugEnabled()) {
                    logger.error(ex.getMessage(), ex);
                }
            }
        }

        return null;
    }

    /**
     * edu-sharing customization: try to authenticate at ldap directly
     *
     * @param username
     * @param password
     * @param loginName
     * @throws AuthenticationException
     */
    private void authenticate(String ldapUserDn, String username, String password, String loginName) throws AuthenticationException {
        if (env != null) {
            Properties authEnv = new Properties();
            authEnv.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            authEnv.put(Context.PROVIDER_URL, this.ldapUrl);
            authEnv.put(Context.SECURITY_PRINCIPAL, ldapUserDn);
            authEnv.put(Context.SECURITY_AUTHENTICATION, env.get(Context.SECURITY_AUTHENTICATION));
            authEnv.put(Context.SECURITY_CREDENTIALS, password);

            try {
                new InitialDirContext(authEnv);
                ApplicationContext context = AlfAppContextGate.getApplicationContext();
                AuthenticationComponent authComp = (AuthenticationComponent) context.getBean("authenticationComponent");
                authComp.setCurrentUser(username);
                logger.info("webdav ldap authentication: sucessfull. loginName:" + hMac.calculateHmac(loginName) + " / userName:" + hMac.calculateHmac(username));
                return;
            } catch (javax.naming.AuthenticationException e) {
                logger.debug(e.getMessage(), e);
                throw new AuthenticationException(e.getMessage());
            } catch (NamingException e) {
                logger.debug(e.getMessage(), e);
                throw new AuthenticationException(e.getMessage());
            }
        }
        throw new AuthenticationException("LDAPAuthenticationFilter env seems to be null");
    }
}
