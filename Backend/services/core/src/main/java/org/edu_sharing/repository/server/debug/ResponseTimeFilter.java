package org.edu_sharing.repository.server.debug;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

public class ResponseTimeFilter  implements javax.servlet.Filter {

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
