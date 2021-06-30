package org.edu_sharing.alfresco.repository.server.authentication;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.edu_sharing.repository.client.tools.CCConstants;

public class Context {

    private static ThreadLocal<Context> instance = new ThreadLocal<Context>();
    private static ServletContext globalContext;

    private HttpServletRequest request;
    private HttpServletResponse response;

    private Context(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
    }

    public static Context getCurrentInstance() {
        return instance.get();
    }

    public static ServletContext getGlobalContext() {
        return globalContext;
    }
    public static Context setInstance(Context context){
        instance.set(context);
        return context;
    }
    public static Context newInstance(HttpServletRequest request, HttpServletResponse response, ServletContext servletContext) {
        Context context = new Context(request,response);
        if(globalContext==null)
            globalContext=servletContext;
        instance.set(context);
        return context;
    }

    public static void release() {
        instance.remove();
    }

    public HttpServletRequest getRequest() {
        return request;
    }
    
    public HttpServletResponse getResponse() {
		return response;
	}
    
    public String getSessionAttribute(String key){
    	String sessionAtt = null;
    	if(this.getRequest() != null 
    			&& this.getRequest().getSession() != null){
    		sessionAtt = (String)this.getRequest().getSession().getAttribute(key);
		}
    	return sessionAtt;
    }
    
    public String getLocale(){
    	return getSessionAttribute(CCConstants.AUTH_LOCALE);
    }
    
    public String getAuthType(){
    	return getSessionAttribute(CCConstants.AUTH_TYPE);
    }
    
    public String getAccessToken(){
    	return getSessionAttribute(CCConstants.AUTH_ACCESS_TOKEN);
    }

}

