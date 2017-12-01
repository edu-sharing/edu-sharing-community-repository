package org.edu_sharing.restservices;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ApiOriginFilter implements javax.servlet.Filter {

	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		
		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse res = (HttpServletResponse) response;
		
		String origin = req.getHeader("Origin");
		
		res.addHeader(
				"Access-Control-Allow-Origin", 		
				origin != null ? origin : "*");
		
		res.addHeader(
				"Access-Control-Allow-Methods", 		
				"OPTIONS, GET, POST, PUT, DELETE");
		
		res.addHeader(
				"Access-Control-Allow-Headers", 		
				req.getHeader("Access-Control-Request-Headers"));
		
		res.addHeader(
				"Access-Control-Allow-Credentials", 	
				"true");
		
		chain.doFilter(request, response);
	}

	public void destroy() {}

	public void init(FilterConfig filterConfig) throws ServletException {}
}