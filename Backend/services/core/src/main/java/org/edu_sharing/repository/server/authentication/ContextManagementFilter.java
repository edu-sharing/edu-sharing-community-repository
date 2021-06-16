package org.edu_sharing.repository.server.authentication;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.authentication.HttpContext;
import org.edu_sharing.alfresco.authentication.subsystems.SubsystemChainingAuthenticationService;
import org.edu_sharing.alfresco.policy.NodeCustomizationPolicies;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.alfresco.workspace_administration.NodeServiceInterceptor;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.metadataset.v2.QueryUtils;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.security.SignatureVerifier;
import org.edu_sharing.restservices.RepositoryDao;
import org.edu_sharing.service.authentication.ScopeAuthenticationServiceFactory;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.config.ConfigServiceFactory;
import org.edu_sharing.alfresco.service.config.model.AvailableMds;
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

			try{
				// Run as System because there is yet no session opened
				Map<String, Serializable> info = AuthenticationUtil.runAsSystem(() ->
						AuthorityServiceFactory.getLocalService().getUserInfo(
						(String) ((HttpServletRequest) req).getSession().getAttribute(CCConstants.AUTH_USERNAME))
				);
				QueryUtils.setUserInfo(info);
			}catch(Exception e){
				logger.info("Could not set user info: "+e.getMessage());
			}

			((HttpServletResponse)res).setHeader("Access-Control-Expose-Headers","X-Edu-Scope");
			((HttpServletResponse)res).setHeader("X-Edu-Scope", NodeServiceInterceptor.getEduSharingScope());
			SubsystemChainingAuthenticationService.setCurrentPath(((HttpServletRequest) req).getContextPath() + ((HttpServletRequest) req).getServletPath());

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
			QueryUtils.setUserInfo(null);
			SubsystemChainingAuthenticationService.setSuccessFullAuthenticationMethod((String)null);
			SubsystemChainingAuthenticationService.setCurrentPath((String)null);

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

		String appId = httpReq.getHeader("X-Edu-App-Id");
		if(appId != null) {
			SignatureVerifier.Result result = new SignatureVerifier().verifyAppSignature(httpReq);
			if(result.getStatuscode() != 200){
				logger.debug("application request could not be verified:" + appId + " "+result.getMessage());
			}else{
				ApplicationInfo appInfo = result.getAppInfo();
				accessToolType.set(appInfo.getType());
			}
		}
	}

}
