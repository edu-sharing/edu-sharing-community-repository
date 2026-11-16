package org.edu_sharing.repository.server;

import jakarta.servlet.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.authority.AuthorityServiceHelper;
import org.edu_sharing.service.nodeservice.CallSourceHelper;

import java.io.IOException;
import java.util.Map;


/**
 * Filter to add possible headers that should be included in every request
 */
@Slf4j
public class GlobalHeaderFilter implements Filter {
	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
		if(log.isDebugEnabled()) {
			HttpServletRequest request = (HttpServletRequest) req;
			HttpSession session = request.getSession(false);
			Cookie[] cookies = request.getCookies();
			if(cookies != null) {
				for(Cookie cookie : cookies) {
					log.debug("cookie {}: {}",cookie.getName(), cookie.getValue());
				}
			}
			if(session != null) {
				log.debug("found session: " + session.getId());
			}
		}

		if (LightbendConfigLoader.get().hasPath("security.headers")) {
			for (Map.Entry<String, Object> entry : LightbendConfigLoader.get().getObject("security.headers").unwrapped().entrySet()) {
				if (entry.getValue() != null) {
					((HttpServletResponse) res).addHeader(entry.getKey(), entry.getValue().toString());
				}
			}
		}
		String pathInfo = ((HttpServletRequest)req).getPathInfo();
		if(pathInfo == null) {
			String uri = ((HttpServletRequest)req).getRequestURI();
			pathInfo = uri == null ? null : uri.substring(CallSourceHelper.WEBAPP_BASE_PATH.length());
		}
		// the swagger ui follows the openapi access config (openapi.json itself is checked in ApiAuthenticationFilter)
		if (pathInfo != null && pathInfo.startsWith("/swagger") && !isAccessAllowed((HttpServletRequest) req, LightbendConfigLoader.get().getString("security.access.openapi"))) {
			((HttpServletResponse) res).setStatus(HttpServletResponse.SC_FORBIDDEN);
			res.flushBuffer();
			res.getWriter().print("This path is disabled via config");
			return;
		}
		if (LightbendConfigLoader.get().hasPath("security.access.paths")) {
			if(pathInfo != null) {
				for (Map.Entry<String, Object> entry : LightbendConfigLoader.get().getObject("security.access.paths").unwrapped().entrySet()) {
					if (pathInfo.startsWith(entry.getKey()) && !isAccessAllowed((HttpServletRequest) req, entry.getValue().toString())) {
						((HttpServletResponse) res).setStatus(HttpServletResponse.SC_FORBIDDEN);
						res.flushBuffer();
						res.getWriter().print("This path is disabled via config");
						return;
					}
				}
			}
        }
		chain.doFilter(req, res);
	}

	/**
	 * @param access "disabled", "admin" or "enabled"
	 */
	private static boolean isAccessAllowed(HttpServletRequest req, String access) {
		if ("disabled".equalsIgnoreCase(access)) {
			return false;
		}
		return !"admin".equalsIgnoreCase(access) || isAdmin(req);
	}

	/**
	 * static resources (e.g. /swagger) have no alfresco security context, so fall back to the user of the http session
	 */
	private static boolean isAdmin(HttpServletRequest req) {
		if (AuthenticationUtil.getFullyAuthenticatedUser() != null) {
			return AuthorityServiceHelper.isAdmin();
		}
		HttpSession session = req.getSession(false);
		String username = session == null ? null : (String) session.getAttribute(CCConstants.AUTH_USERNAME);
		return username != null && AuthorityServiceHelper.isAdmin(username);
	}

}
