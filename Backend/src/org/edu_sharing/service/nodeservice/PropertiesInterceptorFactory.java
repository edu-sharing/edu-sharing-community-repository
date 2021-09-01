package org.edu_sharing.service.nodeservice;

import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;

public class PropertiesInterceptorFactory {


    static PropertiesInterceptor propertiesInterceptor = null;

    public static PropertiesInterceptor getPropertiesInterceptor(){
        if(propertiesInterceptor == null){
            synchronized (PropertiesInterceptorFactory.class){
                try{
                    String className = LightbendConfigLoader.get().getString("propertiesInterceptor");
                    Class clazz = Class.forName(className);
                    propertiesInterceptor =  (PropertiesInterceptor) clazz.newInstance();
                }catch(Throwable t) {
                    throw new RuntimeException(t);
                }
            }
        }
        return propertiesInterceptor;
    }
}
