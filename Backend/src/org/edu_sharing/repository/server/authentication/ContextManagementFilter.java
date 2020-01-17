package org.edu_sharing.repository.server.authentication;

import java.io.IOException;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.authentication.HttpContext;
import org.edu_sharing.alfresco.authentication.subsystems.SubsystemChainingAuthenticationService;
import org.edu_sharing.alfresco.policy.NodeCustomizationPolicies;
import org.edu_sharing.alfresco.workspace_administration.NodeServiceInterceptor;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.security.SignatureVerifier;
import org.edu_sharing.restservices.RepositoryDao;
import org.edu_sharing.service.authentication.ScopeAuthenticationServiceFactory;
import org.edu_sharing.service.config.ConfigServiceFactory;
import org.edu_sharing.service.config.model.AvailableMds;
import org.edu_sharing.webservices.util.AuthenticationUtils;

import net.sf.acegisecurity.AuthenticationCredentialsNotFoundException;


public class ContextManagementFilter implements javax.servlet.Filter {

	// stores the currently accessing tool type, e.g. CONNECTOR
	public static ThreadLocal<String> accessToolType = new ThreadLocal<>();


	Logger logger = Logger.getLogger(ContextManagementFilter.class);

	ServletContext context;

	@Override
	public void destroy() {
	}
	
	@Override
	public void init(FilterConfig config) throws ServletException {
		this.context=config.getServletContext();
	}
	
	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {

		logger.debug("thread:"+Thread.currentThread().getId() +" "+((HttpServletRequest)req).getServletPath()+" starting");
				
		try {
			
			Context.newInstance((HttpServletRequest)req , (HttpServletResponse)res, context);
			ScopeAuthenticationServiceFactory.getScopeAuthenticationService().setScopeForCurrentThread();
			((HttpServletResponse)res).setHeader("Access-Control-Expose-Headers","X-Edu-Scope");
			((HttpServletResponse)res).setHeader("X-Edu-Scope", NodeServiceInterceptor.getEduSharingScope());
			
			try {
				NodeCustomizationPolicies.setEduSharingContext(ConfigServiceFactory.getCurrentContextId((HttpServletRequest) req));
				AvailableMds[] availableMdss = ConfigServiceFactory.getCurrentConfig().values.availableMds;
				if(availableMdss != null) {
					for(AvailableMds availableMds : availableMdss) {
						if(RepositoryDao.HOME.equals(availableMds.repository) 
								|| ApplicationInfoList.getHomeRepository().getAppId().equals(availableMds.repository)){
							HttpContext.setCurrentMetadataSet(availableMds.mds[0]);
						}
					}
				}
				
			}catch(Throwable e) {
				logger.debug(e.getMessage());
			}

			handleAppSignature((HttpServletRequest)req);

			chain.doFilter(req,res);

		} finally {

			logger.debug("thread:"+Thread.currentThread().getId() +" "+((HttpServletRequest)req).getServletPath()+" cleaning up");

			NodeServiceInterceptor.setEduSharingScope((String)null);
			NodeCustomizationPolicies.setEduSharingContext(null);
			SubsystemChainingAuthenticationService.setSuccessFullAuthenticationMethod((String)null);
			
			HttpContext.setCurrentMetadataSet(null);
			
			/**
			 * OAuth kill Session
			 */
			HttpServletRequest request = (HttpServletRequest)req;
			
			
			Context.getCurrentInstance().release();
			
			//clean up alfresco security context
			
			//for native API
			ServiceRegistry serviceRegistry = (ServiceRegistry) AlfAppContextGate.getApplicationContext().getBean(ServiceRegistry.SERVICE_REGISTRY);
			AuthenticationService authservice = serviceRegistry.getAuthenticationService();
			try{
				//its not really necessary cause AuthenticationFilter -> AuthenticationTool calls alfresco authenticationservice.validate which
				//also calls clearCurrentSecurityContext()
				authservice.clearCurrentSecurityContext();
			}catch(AuthenticationCredentialsNotFoundException e){
				logger.debug("thread:"+Thread.currentThread().getId() +" "+((HttpServletRequest)req).getServletPath()+ " there was nothing to clean up in native api");
			}
			
			//for soap api
			AuthenticationUtils.setAuthenticationDetails(null);
				
		}
		
	}

	/**
	 * Checks if app headers and signature are present and sets the header accordingly
	 * @param httpReq
	 */
	private void handleAppSignature(HttpServletRequest httpReq) {
        accessToolType.set(null);
		if(httpReq.getHeader("X-Edu-App-Id")!=null){
			String appId=httpReq.getHeader("X-Edu-App-Id");
			String sig=httpReq.getHeader("X-Edu-App-Sig");
			String signed=httpReq.getHeader("X-Edu-App-Signed");
			String ts=httpReq.getHeader("X-Edu-App-Ts");
			ApplicationInfo app = ApplicationInfoList.getRepositoryInfoById(appId);
			if(app==null){
				logger.warn("X-Edu-App-Id header was sent but the tool "+appId+" was not found in the list of registered apps");
			}
			else{
				SignatureVerifier.Result result = new SignatureVerifier().verify(appId, sig, signed, ts);
				if(result.getStatuscode() == HttpServletResponse.SC_OK){
					accessToolType.set(app.getType());
					logger.debug("Connector request verified, setting accessToolType to "+accessToolType.get());
				}
				else{
					logger.warn("X-Edu-App-Id header was sent but signature check failed: "+result.getMessage());
				}
			}

		}
	}

}
