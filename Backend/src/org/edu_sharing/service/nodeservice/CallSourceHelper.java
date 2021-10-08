package org.edu_sharing.service.nodeservice;

import org.apache.http.HttpHeaders;
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
        String requestURI = "";
        String referer = "";
        if(Context.getCurrentInstance() != null) {
            requestURI = Context.getCurrentInstance().getRequest().getRequestURI();
            referer =  Context.getCurrentInstance().getRequest().getHeader(HttpHeaders.REFERER);
            if(referer == null) referer = "";
        }

        if(requestURI.contains("rest/search")){
            return CallSource.Search;
        }else if(isRender(requestURI) || (isRender(referer))){
            return CallSource.Render;
        }else{
            return CallSource.Workspace;
        }
    }

    private static boolean isRender(String url){
        if(url.contains("edu-sharing/components/render")
                || url.contains("edu-sharing/rest/rendering")
                || url.contains("edu-sharing/eduservlet/render")
                || url.contains("edu-sharing/content")){
            return true;
        }
        return false;
    }
}
