package org.edu_sharing.service.nodeservice;

import org.edu_sharing.alfresco.repository.server.authentication.Context;

public class CallSourceHelper {
    enum CallSource {
        Search,
        Render,
        Workspace,
        Unknown
    }

    public static CallSource getCallSource(){
        if(Context.getCurrentInstance() == null || Context.getCurrentInstance().getRequest() == null){
            return CallSource.Unknown;
        }
        String requestURI = Context.getCurrentInstance().getRequest().getRequestURI();
        if(requestURI.contains("rest/search")){
            return CallSource.Search;
        }else if(requestURI.contains("components/render")
                || requestURI.contains("rest/rendering")
                || requestURI.contains("eduservlet/render")
                || requestURI.contains("/content")){
            return CallSource.Render;
        }else{
            return CallSource.Workspace;
        }
    }
}
