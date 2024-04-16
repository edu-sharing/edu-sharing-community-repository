package org.edu_sharing.repository.server.authentication;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

public class ShibbolethSLOHeaderFix implements jakarta.servlet.Filter {

	@Override
	public void doFilter(final ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
		HttpServletRequestWrapper wrapper = new HttpServletRequestWrapper((HttpServletRequest)req) {
	        @Override
	        public String getHeader(String name) {
	        	
	        	if(name.equals("SOAPAction")){
	        		String soapActionHeaderValue = super.getHeader(name);
	        		System.out.println("Axis 1.4 requires SOAPAction not to be empty: "+soapActionHeaderValue);
	        		
	        		if(soapActionHeaderValue == null){
	        			return "";
	        		}else{
	        			return soapActionHeaderValue;
	        		}
	        	}else{
	        		return super.getHeader(name);
	        	}
	        	
	        	
	        }
	    };
	    chain.doFilter(wrapper, resp);
	}
	
	
	@Override
	public void destroy() {
		
	}
	
	@Override
	public void init(FilterConfig arg0) throws ServletException {
		
	}
	
}
