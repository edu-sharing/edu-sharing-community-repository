package org.edu_sharing.repository.server.debug;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

public class ResponseTimeFilter  implements jakarta.servlet.Filter {

	Logger logger = Logger.getLogger(ResponseTimeFilter.class);
	@Override
	public void init(FilterConfig init) throws ServletException {
		
	}
	
	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
		
		HttpServletRequest request = (HttpServletRequest)req;
		long time = System.currentTimeMillis();
		chain.doFilter(req,res);
		
		
		time = System.currentTimeMillis() - time;
		
		if(time > 200){
			logger.debug(request.getRequestURI() + " " + " needed:" + time);
		}
		
	}
	
	
	@Override
	public void destroy() {
		
	}
	
}
