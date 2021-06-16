package org.edu_sharing.repository.server.authentication;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.toolpermission.ToolPermissionServiceFactory;

public class SafeScopeToolPermissionFilter implements javax.servlet.Filter {
	
	@Override
	public void init(FilterConfig arg0) throws ServletException {
	}
	
	@Override
	public void destroy() {
	}
	
	@Override
	public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
			throws IOException, ServletException {
		
		HttpServletRequest httpReq = ((HttpServletRequest)req);
		HttpServletResponse httpResp = ((HttpServletResponse)resp);
		
		HttpSession httpSession = httpReq.getSession(false);
		
		if(httpSession == null){
			httpResp.sendRedirect("/edu-sharing");
			return;
		}
		
		try {
			if (!ToolPermissionServiceFactory.getInstance().hasToolPermission(CCConstants.CCM_VALUE_TOOLPERMISSION_CONFIDENTAL)) {
				httpResp.sendRedirect("/edu-sharing?noSafeToolPermission=true");
				return;
			}
		} catch(net.sf.acegisecurity.AuthenticationCredentialsNotFoundException e) {
			httpResp.sendRedirect("/edu-sharing");
			return;
		}
		
		chain.doFilter(req, resp);
		
	}
	
}
