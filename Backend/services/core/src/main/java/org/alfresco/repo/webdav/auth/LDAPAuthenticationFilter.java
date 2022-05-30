package org.alfresco.repo.webdav.auth;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.naming.CommunicationException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.UserTransaction;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.alfresco.repo.security.authentication.AuthenticationException;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.NoSuchPersonException;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.transaction.TransactionService;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.springframework.context.ApplicationContext;


/**
 * Servlet Filter implementation class CockpitAuthenticationFilter
 */
public class LDAPAuthenticationFilter implements Filter {

	private static final String CONTEXT = LDAPAuthenticationFilter.class.getCanonicalName();
	private static final String RELEASE = "$Revision: 1422 $";

	private static final String WELCOME = "WebDAV Server";

	// init params

	private static final String INIT_LDAP_URI      = "ldap.uri";
	private static final String INIT_LDAP_BASE     = "ldap.base";
	private static final String INIT_LDAP_SEC_AUTH = "ldap.sec.auth";
	private static final String INIT_LDAP_SEC_USER = "ldap.sec.user";
	private static final String INIT_LDAP_SEC_PWD  = "ldap.sec.pwd";

	private static final String INIT_LDAP_FROM     = "ldap.from";
	private static final String INIT_LDAP_TO       = "ldap.to";
	private static final String INIT_LDAP_UID 	   = "ldap.uid";
	

	// Allow an authentication ticket to be passed as part of a request to bypass authentication

	private static final String ARG_TICKET = "ticket";
	private static final String PPT_EXTN = ".ppt";
	private static final String VTI_IGNORE = "&vtiIgnore";


	private static final Log logger = LogFactory.getLog(LDAPAuthenticationFilter.CONTEXT);

	// Servlet context

	private ServletContext m_context;

	// Various services required by NTLM authenticator

	private AuthenticationService m_authService;
	private PersonService m_personService;
	private NodeService m_nodeService;
	private TransactionService m_transactionService;

	private DirContext jndi;

	private String ldapFrom;
	private String ldapTo;

	/**
	 * edu-sharing customization
	 */
	
	private static final String INIT_USE_ALFRESCO_AUTHENTICATION_COMPONENT = "alfresco.auth.comp";
	private boolean useAlfrescoAuthenticationConponent = true;
	private String ldapBase = null;
	
	//rember the env global
	private Properties env = null;
	private String ldapUidProp = null;
	private String ldapUrl = null;
	
	
	/**
	 * edu-sharing fix from 4.2.f
	 * 
	 * 
	 * ALF-13621: Due to browser inconsistencies we have to try a fallback path of encodings
	 */
	/** The password encodings to try in priority order **/
    private static final String[] ENCODINGS = new String[] {
        "UTF-8", 
        System.getProperty("file.encoding"),
        "ISO-8859-1"
    };
    
    /** Corresponding array of CharsetDecoders with CodingErrorAction.REPORT. Duplicates removed. */
    private static final CharsetDecoder[] DECODERS;
    
    static
    {
        Map<String, CharsetDecoder> decoders = new LinkedHashMap<String, CharsetDecoder>(ENCODINGS.length * 2);
        for (String encoding : ENCODINGS)
        {
            if (!decoders.containsKey(encoding))
            {
                decoders.put(encoding, Charset.forName(encoding).newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT));
            }
        }
        DECODERS = new CharsetDecoder[decoders.size()];
        decoders.values().toArray(DECODERS);
    }
	
	
	/**
	 * Initialize the filter
	 * 
	 * @param config FitlerConfig
	 * @exception ServletException
	 */
	@Override
	public void init(FilterConfig config) throws ServletException
	{
		LDAPAuthenticationFilter.logger.info(LDAPAuthenticationFilter.CONTEXT + " (" + LDAPAuthenticationFilter.RELEASE + ")");

		// Save the context

		this.m_context = config.getServletContext();

		// Setup the authentication context

		//WebApplicationContext ctx = WebApplicationContextUtils.getRequiredWebApplicationContext(this.m_context);
		
		ApplicationContext context = AlfAppContextGate.getApplicationContext();
		ServiceRegistry serviceRegistry = (ServiceRegistry) context.getBean(ServiceRegistry.SERVICE_REGISTRY);

		//ServiceRegistry serviceRegistry = (ServiceRegistry) ctx.getBean(ServiceRegistry.SERVICE_REGISTRY);
		this.m_nodeService = serviceRegistry.getNodeService();
		this.m_authService = serviceRegistry.getAuthenticationService();
		this.m_transactionService = serviceRegistry.getTransactionService();
		//this.m_personService = (PersonService) ctx.getBean("PersonService");   // transactional and permission-checked
		this.m_personService = (PersonService) context.getBean("PersonService");   // transactional and permission-checked

		Properties properties = new Properties();
		try {
			properties.load(getClass().getClassLoader().getResourceAsStream("ldap.properties"));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		this.ldapBase = properties.getProperty(LDAPAuthenticationFilter.INIT_LDAP_BASE);
		
		env = new Properties();
		env.put(Context.INITIAL_CONTEXT_FACTORY,
				"com.sun.jndi.ldap.LdapCtxFactory");

		
		this.ldapUrl = properties.getProperty(LDAPAuthenticationFilter.INIT_LDAP_URI);
		env.put(Context.PROVIDER_URL, this.ldapUrl + "/" + this.ldapBase);
		env.put(Context.SECURITY_AUTHENTICATION, properties.getProperty(LDAPAuthenticationFilter.INIT_LDAP_SEC_AUTH));
		env.put(Context.SECURITY_PRINCIPAL, properties.getProperty(LDAPAuthenticationFilter.INIT_LDAP_SEC_USER));
		env.put(Context.SECURITY_CREDENTIALS, properties.getProperty(LDAPAuthenticationFilter.INIT_LDAP_SEC_PWD));
		
		//edu-sharing
		String useAlfAuthComp = properties.getProperty(LDAPAuthenticationFilter.INIT_USE_ALFRESCO_AUTHENTICATION_COMPONENT);
		if(useAlfAuthComp != null && !useAlfAuthComp.trim().equals("") ){
			useAlfrescoAuthenticationConponent = new Boolean(useAlfAuthComp);
		}

		try {

			this.jndi = new InitialDirContext(env);

		} catch (NamingException e) {
			logger.error(e.getMessage(), e);
			throw new ServletException(e);
		}

		this.ldapFrom = properties.getProperty(LDAPAuthenticationFilter.INIT_LDAP_FROM);
		this.ldapTo = properties.getProperty(LDAPAuthenticationFilter.INIT_LDAP_TO);
		this.ldapUidProp = properties.getProperty(LDAPAuthenticationFilter.INIT_LDAP_UID);
		if(this.ldapUidProp == null || this.ldapUidProp.trim().equals("")) this.ldapUidProp = "uid";
		
	}

	/**
	 * @see Filter#destroy()
	 */
	@Override
	public void destroy() {

		if (this.jndi != null) {

			try {

				this.jndi.close();

			} catch (NamingException e) {}

		}
	}

	/**
	 * Run the authentication filter
	 * 
	 * @param req ServletRequest
	 * @param resp ServletResponse
	 * @param chain FilterChain
	 * @exception ServletException
	 * @exception IOException
	 */
	@Override
	public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException,
	ServletException
	{
		
		logger.debug("called!!!");
		// Assume it's an HTTP request

		HttpServletRequest httpReq = (HttpServletRequest) req;
		HttpServletResponse httpResp = (HttpServletResponse) resp;

		// Get the user details object from the session

		WebDAVUser user = (WebDAVUser) httpReq.getSession().getAttribute(BaseAuthenticationFilter.AUTHENTICATION_USER);

		if (user == null)
		{
			// Get the authorization header

			
			String authHdr = httpReq.getHeader("Authorization");
			logger.debug("user == null authHdr:"+authHdr);
			if ( (authHdr != null) && (authHdr.length() > 5) && authHdr.substring(0,5).equalsIgnoreCase("BASIC"))
			{
				// Basic authentication details present
/*
				String basicAuth = new String(java.util.Base64.getDecoder().decode(authHdr.substring(5).getBytes()));

				// Split the username and password

				String username = null;
				String password = null;

				int pos = basicAuth.indexOf(":");
				if ( pos != -1)
				{
					username = basicAuth.substring(0, pos);
					password = basicAuth.substring(pos + 1);
				}
				else
				{
					username = basicAuth;
					password = "";
				}

				try{
					user = searchForUser(username,password);
				}catch(CommunicationException e){
					logger.error(e.getMessage() +" Will create new InitialDirContext and retry.");
					try{
						this.jndi = new InitialDirContext(env);
						user = searchForUser(username,password);
					}catch(CommunicationException ce){
						
						logger.error(e.getMessage() + " still occurs will give up. maybe restart alfresco.");
						
					}catch (NamingException ne) {
						logger.error(ne.getMessage(), ne);
					}
				}
				
				if(user != null){
					httpReq.getSession().setAttribute(BaseAuthenticationFilter.AUTHENTICATION_USER, user);
				}

				
*/
				
				/**
				 * edu-sharing fix
				 */
				 if (logger.isDebugEnabled())
	                    logger.debug("Basic authentication details present in the header.");
	                byte[] encodedString = java.util.Base64.getDecoder().decode(authHdr.substring(5).trim().getBytes());
	                
	                // ALF-13621: Due to browser inconsistencies we have to try a fallback path of encodings
	                Set<String> attemptedAuths = new HashSet<String>(DECODERS.length * 2);
	                for (CharsetDecoder decoder : DECODERS)
	                {                  
	                    try
	                    {
	                        // Attempt to decode using this charset 
	                        String basicAuth = decoder.decode(ByteBuffer.wrap(encodedString)).toString();
	                        
	                        
	                        
	                        // It decoded OK but we may already have tried this string.
	                        if (!attemptedAuths.add(basicAuth))
	                        {
	                            // Already tried - no need to try again
	                            continue;
	                        }
	                        
	                        
	                        String username = null;
	        				String password = null;
	                        
	                     
	                        // Split the username and password
	                        int pos = basicAuth.indexOf(":");
	                        if (pos != -1)
	                        {
	                            username = basicAuth.substring(0, pos);
	                            password = basicAuth.substring(pos + 1);
	                        }
	                        else
	                        {
	                            username = basicAuth;
	                            password = "";
	                        }
	    
	                        // Authenticate the user    
	                        try{
	        					user = searchForUser(username,password);
	        				}catch(CommunicationException e){
	        					logger.error(e.getMessage() +" Will create new InitialDirContext and retry.");
	        					try{
	        						this.jndi = new InitialDirContext(env);
	        						user = searchForUser(username,password);
	        					}catch(CommunicationException ce){
	        						
	        						logger.error(e.getMessage() + " still occurs will give up. maybe restart alfresco.");
	        						
	        					}catch (NamingException ne) {
	        						logger.error(ne.getMessage(), ne);
	        					}
	        				}
	        				
	        				if(user != null){
	        					httpReq.getSession().setAttribute(BaseAuthenticationFilter.AUTHENTICATION_USER, user);
	        					 // Success so break out
		                        break;
	        				}
	                        
	                        
	                        
	                       
	                    }
	                    catch (CharacterCodingException e)
	                    {
	                        if (logger.isDebugEnabled())
	                            logger.debug("Didn't decode using " + decoder.getClass().getName(), e);
	                    }
	                    catch (AuthenticationException ex)
	                    {
	                        if (logger.isDebugEnabled())
	                            logger.debug("Authentication error ", ex);
	                    }
	                    catch (NoSuchPersonException e)
	                    {
	                        if (logger.isDebugEnabled())
	                            logger.debug("There is no such person error ", e);
	                    }
	                }
				
				
				
				
			}
			else
			{
				// Check if the request includes an authentication ticket

				String ticket = req.getParameter( LDAPAuthenticationFilter.ARG_TICKET);
				
				logger.debug("auth by ticket:"+ticket);

				if ( (ticket != null) &&  (ticket.length() > 0))
				{
					// PowerPoint bug fix
					if (ticket.endsWith(LDAPAuthenticationFilter.PPT_EXTN))
					{
						ticket = ticket.substring(0, ticket.length() - LDAPAuthenticationFilter.PPT_EXTN.length());
					}

					// vtiIgnore argument may find its way onto the ticket due to a double-encoding issue with Office
					if (ticket.endsWith(LDAPAuthenticationFilter.VTI_IGNORE))
					{
						ticket = ticket.substring(0, ticket.length() - LDAPAuthenticationFilter.VTI_IGNORE.length());
					}

					// Debug

					if ( LDAPAuthenticationFilter.logger.isDebugEnabled()) {
						LDAPAuthenticationFilter.logger.debug("Logon via ticket from " + req.getRemoteHost() + " (" +
								req.getRemoteAddr() + ":" + req.getRemotePort() + ")" + " ticket=" + ticket);
					}

					UserTransaction tx = null;
					try
					{
						// Validate the ticket

						this.m_authService.validate(ticket);

						// Need to create the User instance if not already available

						String currentUsername = this.m_authService.getCurrentUserName();

						// Start a transaction

						tx = this.m_transactionService.getUserTransaction();
						tx.begin();

						NodeRef personRef = this.m_personService.getPerson(currentUsername);
						user = new WebDAVUser( currentUsername, this.m_authService.getCurrentTicket(), personRef);
						NodeRef homeRef = (NodeRef) this.m_nodeService.getProperty(personRef, ContentModel.PROP_HOMEFOLDER);

						// Check that the home space node exists - else Login cannot proceed

						if (this.m_nodeService.exists(homeRef) == false)
						{
							throw new InvalidNodeRefException(homeRef);
						}
						user.setHomeNode(homeRef);

						tx.commit();
						tx = null;

						// Store the User object in the Session - the authentication servlet will then proceed

						httpReq.getSession().setAttribute(BaseAuthenticationFilter.AUTHENTICATION_USER, user);
					}
					catch (AuthenticationException authErr)
					{
						// Clear the user object to signal authentication failure

						user = null;
					}
					catch (Throwable e)
					{
						// Clear the user object to signal authentication failure

						user = null;
					}
					finally
					{
						try
						{
							if (tx != null)
							{
								tx.rollback();
							}
						}
						catch (Exception tex)
						{
						}
					}
				}
			}

			// Check if the user is authenticated, if not then prompt again

			if ( user == null)
			{
				// No user/ticket, force the client to prompt for logon details

				httpResp.setHeader("WWW-Authenticate", "BASIC realm=\""+ LDAPAuthenticationFilter.WELCOME +"\"");
				httpResp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

				httpResp.flushBuffer();
				return;
			}
		}
		else
		{
			
			logger.debug("user != null :"+user.getTicket());
			try
			{
				// Setup the authentication context
				this.m_authService.validate(user.getTicket());

				// Set the current locale

				// I18NUtil.setLocale(Application.getLanguage(httpRequest.getSession()));
			}
			catch (Exception ex)
			{
				// No user/ticket, force the client to prompt for logon details

				httpResp.setHeader("WWW-Authenticate", "BASIC realm=\""+ LDAPAuthenticationFilter.WELCOME +"\"");
				httpResp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

				httpResp.flushBuffer();
				return;
			}
		}

		// Chain other filters

		chain.doFilter(req, resp);
	}
	
	/**
	 * search for user encapsulated in a method to catch a potential CommunicationException and retry
	 * 
	 *https://issues.apache.org/jira/browse/HADOOP-9125
	 *https://issues.apache.org/jira/secure/attachment/12560771/HADOOP-9125.patch
	 */
	WebDAVUser searchForUser(String username, String password) throws CommunicationException{
		
		
		String ldapUsername = username;
		
		String uid = null;
		
		try
		{
			SearchControls ctls = new SearchControls();

			ctls.setSearchScope(SearchControls.ONELEVEL_SCOPE);
			ctls.setReturningAttributes(new String[] {this.ldapTo,this.ldapUidProp});

			String base = "";
			String query = "(" +this.ldapFrom +"="+ username + ")";
			logger.debug("query:"+query);
			NamingEnumeration<SearchResult> rs;

			rs = this.jndi.search(base, query, ctls);

			if (rs.hasMore()) {

				SearchResult r = rs.next();

				Attribute attr = r.getAttributes().get(this.ldapTo);

				if (attr != null) {
					username = (String) attr.get();
				}
				
				Attribute uidAttr = r.getAttributes().get(this.ldapUidProp);
				if(uidAttr != null){
					uid = (String) uidAttr.get();
				}
			}
			rs.close();

			logger.debug("query:"+query + " new username:"+username);
			//edu-sharing customization
			if(useAlfrescoAuthenticationConponent){
				this.m_authService.authenticate(username, password.toCharArray());
			}else{
				
				logger.debug("using direct ldap auth ldapUsername:" + ldapUsername + " uid:" +uid +" username:" +username +" password:" +password);
				this.authenticate(ldapUsername, uid, username, password);
			}

			// Set the user name as stored by the back end
			username = this.m_authService.getCurrentUserName();

			// Get the user node and home folder

			NodeRef personNodeRef = this.m_personService.getPerson(username);
			NodeRef homeSpaceRef = (NodeRef) this.m_nodeService.getProperty(personNodeRef, ContentModel.PROP_HOMEFOLDER);

			// Setup User object and Home space ID etc.

			return new WebDAVUser(username, this.m_authService.getCurrentTicket(), homeSpaceRef);

			

		}catch(CommunicationException ce){
			throw ce;
		} catch (NamingException e) {
			// Do nothing, user object will be null
			logger.error(e.getMessage(),e);
		} catch (AuthenticationException ex) {
			// Do nothing, user object will be null
			logger.error(ex.getMessage(),ex);
		} catch (NoSuchPersonException e) {
			// Do nothing, user object will be null
			logger.error(e.getMessage(),e);
		}
		
		return null;
	}
	
	/**
	 * edu-sharing customization: try to authenticate at ldap directly 
	 *  
	 * @param ldapUsername
	 * @param username
	 * @param password
	 * @throws org.alfresco.repo.security.authentication.AuthenticationException
	 */
	private void authenticate(String ldapUsername, String ldapUid, String username, String password) throws  org.alfresco.repo.security.authentication.AuthenticationException{
		
		if(env != null){
			Properties authEnv = new Properties();
			authEnv.put(Context.INITIAL_CONTEXT_FACTORY,
			"com.sun.jndi.ldap.LdapCtxFactory");
			//authEnv.put(Context.PROVIDER_URL, env.get(Context.PROVIDER_URL));
			authEnv.put(Context.PROVIDER_URL,this.ldapUrl);
			//authEnv.put(Context.SECURITY_PRINCIPAL,"uid="+ldapUid);
			authEnv.put(Context.SECURITY_PRINCIPAL,"uid="+ldapUid+","+this.ldapBase);
			
			authEnv.put(Context.SECURITY_AUTHENTICATION,env.get(Context.SECURITY_AUTHENTICATION));
			authEnv.put(Context.SECURITY_CREDENTIALS,password);
			
			
			try {
				new InitialDirContext(authEnv);
				ApplicationContext context = AlfAppContextGate.getApplicationContext();
				AuthenticationComponent authComp = (AuthenticationComponent)context.getBean("authenticationComponent");
				authComp.setCurrentUser(username);
				logger.info("auth at ldap sucessfull with user:"+username);
				return;
			}catch(javax.naming.AuthenticationException e){
				logger.error(e.getMessage(), e);
				throw new org.alfresco.repo.security.authentication.AuthenticationException(e.getMessage());
			} catch (NamingException e) {
				logger.error(e.getMessage(), e);
				throw new org.alfresco.repo.security.authentication.AuthenticationException(e.getMessage());
			}
			
		}
		throw new org.alfresco.repo.security.authentication.AuthenticationException("LDAPAuthenticationFilter env seems to be null");
	}
}
