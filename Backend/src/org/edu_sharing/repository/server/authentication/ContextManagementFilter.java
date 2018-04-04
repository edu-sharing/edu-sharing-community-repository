package org.edu_sharing.repository.server.authentication;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.authentication.HttpContext;
import org.edu_sharing.alfresco.authentication.subsystems.SubsystemChainingAuthenticationService;
import org.edu_sharing.alfresco.workspace_administration.NodeServiceInterceptor;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.restservices.RepositoryDao;
import org.edu_sharing.service.authentication.ScopeAuthenticationServiceFactory;
import org.edu_sharing.service.config.ConfigServiceFactory;
import org.edu_sharing.service.config.model.AvailableMds;
import org.edu_sharing.webservices.util.AuthenticationUtils;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

public class ContextManagementFilter implements javax.servlet.Filter {
	
	Logger log = Logger.getLogger(ContextManagementFilter.class);
	
	@Override
	public void destroy() {
	}
	
	@Override
	public void init(FilterConfig config) throws ServletException {
	}
	
	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
		
		log.debug("thread:"+Thread.currentThread().getId() +" "+((HttpServletRequest)req).getServletPath()+" starting");
				
		try {
			
			Context.newInstance((HttpServletRequest)req , (HttpServletResponse)res);
			
			ScopeAuthenticationServiceFactory.getScopeAuthenticationService().setScopeForCurrentThread();
			((HttpServletResponse)res).setHeader("Access-Control-Expose-Headers","X-Edu-Scope");
			((HttpServletResponse)res).setHeader("X-Edu-Scope", NodeServiceInterceptor.getEduSharingScope());
			
			try {
				
				AvailableMds[] availableMdss = ConfigServiceFactory.getCurrentConfig().values.availableMds;
				if(availableMdss != null) {
					for(AvailableMds availableMds : availableMdss) {
						if(RepositoryDao.HOME.equals(availableMds.repository) 
								|| ApplicationInfoList.getHomeRepository().getAppId().equals(availableMds.repository)){
							HttpContext.setCurrentMetadataSet(availableMds.mds[0]);
						}
					}
				}
				
			}catch(Exception e) {
				log.debug(e.getMessage());
			}
			chain.doFilter(req,res);

		} finally {
			
			log.debug("thread:"+Thread.currentThread().getId() +" "+((HttpServletRequest)req).getServletPath()+" cleaning up");
			
			NodeServiceInterceptor.setEduSharingScope((String)null);
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
				log.debug("thread:"+Thread.currentThread().getId() +" "+((HttpServletRequest)req).getServletPath()+ " there was nothing to clean up in native api");
			}
			
			//for soap api
			AuthenticationUtils.setAuthenticationDetails(null);
				
		}
		
	}
	
}
