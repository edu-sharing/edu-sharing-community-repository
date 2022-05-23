package org.edu_sharing.alfresco.workspace_administration;

import java.lang.reflect.Parameter;

import org.alfresco.service.cmr.search.SearchParameters;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.log4j.Logger;

public class SearchServiceInterceptor implements MethodInterceptor{

	
	Logger logger = Logger.getLogger(SearchServiceInterceptor.class);
	
	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		String methodName = invocation.getMethod().getName();
		logger.debug("SearchServiceInterceptor: " + methodName );
		
		for(Parameter param : invocation.getMethod().getParameters()){
			logger.debug("param" + param.getName());
			
		}
		
		for(Object argument : invocation.getArguments()){
			if(argument == null) continue;
			logger.debug("argument:" + argument.getClass().getName());
			if(argument instanceof org.alfresco.service.cmr.search.SearchParameters){
				SearchParameters sps = ((org.alfresco.service.cmr.search.SearchParameters)argument);
				String query = sps.getQuery();
				
				if(NodeServiceInterceptor.getEduSharingScope() == null){
					query = "("+query+") AND ISNULL:\"ccm:eduscopename\"";
					
				}else{
					query = "("+query+") AND @ccm\\:eduscopename:" + NodeServiceInterceptor.getEduSharingScope();
				}
				
				logger.debug("new Query:" + query);
				
				sps.setQuery(query);
			}
		}
		
		return invocation.proceed();
	}
}
