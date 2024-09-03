package org.edu_sharing.repository.server;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.sf.acegisecurity.AuthenticationCredentialsNotFoundException;
import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.authentication.HttpContext;
import org.edu_sharing.alfresco.authentication.subsystems.SubsystemChainingAuthenticationService;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.alfresco.policy.NodeCustomizationPolicies;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.alfresco.service.config.model.AvailableMds;
import org.edu_sharing.alfresco.workspace_administration.NodeServiceInterceptor;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.metadataset.v2.QueryUtils;
import org.edu_sharing.repository.TrackingApplicationInfo;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.security.SignatureVerifier;
import org.edu_sharing.restservices.NodeDao;
import org.edu_sharing.restservices.RepositoryDao;
import org.edu_sharing.service.authentication.SSOAuthorityMapper;
import org.edu_sharing.service.authentication.ScopeAuthenticationServiceFactory;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.config.ConfigServiceFactory;
import org.edu_sharing.service.usage.Usage;
import org.edu_sharing.service.usage.Usage2Exception;
import org.edu_sharing.service.usage.Usage2Service;
import org.edu_sharing.webservices.util.AuthenticationUtils;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;


/**
 * Filter to add possible headers that should be included in every request
 */
public class GlobalHeaderFilter implements Filter {
	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
		if (LightbendConfigLoader.get().hasPath("security.headers")) {
			for (Map.Entry<String, Object> entry : LightbendConfigLoader.get().getObject("security.headers").unwrapped().entrySet()) {
				if (entry.getValue() != null) {
					((HttpServletResponse) res).addHeader(entry.getKey(), entry.getValue().toString());
				}
			}
		}
		chain.doFilter(req, res);
	}

}
