package org.edu_sharing.service.nodeservice;

import org.alfresco.service.cmr.repository.NodeRef;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.alfresco.repository.server.authentication.Context;

import java.util.List;
import java.util.Map;

public class PropertiesInterceptorFactory {


    static PropertiesInterceptor propertiesInterceptor = null;

    public static PropertiesInterceptor getPropertiesInterceptor(){
        if(propertiesInterceptor == null){
            synchronized (PropertiesInterceptorFactory.class){
                try{
                    String className = LightbendConfigLoader.get().getString("repository.interceptors.properties");
                    Class clazz = Class.forName(className);
                    propertiesInterceptor =  (PropertiesInterceptor) clazz.newInstance();
                }catch(Throwable t) {
                    throw new RuntimeException(t);
                }
            }
        }
        return propertiesInterceptor;
    }

    public static void refresh(){
        propertiesInterceptor = null;
    }

    public static PropertiesInterceptor.PropertiesContext getPropertiesContext(NodeRef nodeRef, Map<String,Object> properties, List<String> aspects){
        PropertiesInterceptor.PropertiesContext propertiesContext = new PropertiesInterceptor.PropertiesContext();
        propertiesContext.setProperties(properties);
        propertiesContext.setAspects(aspects);
        propertiesContext.setNodeRef(nodeRef);
        String requestURI = Context.getCurrentInstance().getRequest().getRequestURI();
        if(requestURI.contains("rest/search")){
            propertiesContext.setSource(PropertiesInterceptor.PropertiesCallSource.Search);
        }else if(requestURI.contains("components/render")
                || requestURI.contains("rest/rendering")
                || requestURI.contains("eduservlet/render")
                || requestURI.contains("/content")){
            propertiesContext.setSource(PropertiesInterceptor.PropertiesCallSource.Render);
        }else{
            propertiesContext.setSource(PropertiesInterceptor.PropertiesCallSource.Workspace);
        }
        return propertiesContext;
    }
}
