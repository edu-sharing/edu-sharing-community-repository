package org.edu_sharing.restservices;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class ApiOriginFilter implements jakarta.servlet.Filter {
	public static Logger logger=Logger.getLogger(ApiOriginFilter.class);
	/**
	 Endpoints that are allowed to be fetched with CORS wildcard
	 Note: Only OPTIONS + GET methods are allowed
	 */
	List<String> CORS_ALLOWED_ENDPOINTS = Arrays.asList(
			"/config/v1/values",
			"/config/v1/language",
			"/mds/v1/metadatasets"
	);
	public void doFilter(ServletRequest request, ServletResponse response,
						 FilterChain chain) throws IOException, ServletException {

		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse res = (HttpServletResponse) response;
		String origin = req.getHeader("Origin");
		String allowedOrigins = ApplicationInfoList.getHomeRepository().getString(ApplicationInfo.KEY_ALLOW_ORIGIN, "");
		boolean allow=false;
		if(allowedOrigins.isEmpty()){
			res.addHeader(
					"Access-Control-Allow-Origin",
					origin != null ? origin : "*");
			allow=true;
		}
		else if(origin!=null){
			if(Arrays.stream(allowedOrigins.split(",")).anyMatch((o)->o.trim().equals(origin))) {
				res.addHeader("Access-Control-Allow-Origin", origin);
				allow=true;
			}
			else{
				logger.info("client sent origin "+origin+", but it is not present in "+ApplicationInfo.KEY_ALLOW_ORIGIN+" in the home app, will not allow CORS request");
			}
		}
		else{
			// this is usually okay. When the client requests to the same domain, it will omit origin
			//logger.info("No origin was sent by client and "+ApplicationInfo.KEY_ALLOW_ORIGIN+" is set in home app, will not allow CORS request");
		}
		if(allow) {
			res.addHeader(
					"Access-Control-Allow-Methods",
					"OPTIONS, GET, POST, PUT, DELETE");

			res.addHeader(
					"Access-Control-Allow-Headers",
					req.getHeader("Access-Control-Request-Headers"));

			res.addHeader(
					"Access-Control-Allow-Credentials",
					"true");
		} else {
			String pathInfo = req.getPathInfo();
			// allow cors access for GET requests on uncritical endpoints
			// required for (external) web component usage
			if(Arrays.asList("OPTIONS", "GET").contains(req.getMethod()) &&
					CORS_ALLOWED_ENDPOINTS.stream().anyMatch(c -> pathInfo != null && pathInfo.startsWith(c))
			) {
				res.addHeader(
						"Access-Control-Allow-Credentials",
						"false");
				res.addHeader(
						"Access-Control-Allow-Origin", "*");
			}
		}
		chain.doFilter(request, response);
	}

	public void destroy() {}

	public void init(FilterConfig filterConfig) throws ServletException {}
}