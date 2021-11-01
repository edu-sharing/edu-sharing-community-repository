package org.edu_sharing.service.nodeservice;

import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class PropertiesInterceptorFactory {
    static Logger logger = Logger.getLogger(PropertiesInterceptorFactory.class);


    static List<PropertiesInterceptor> propertiesInterceptor = null;

    public static List<? extends PropertiesInterceptor> getPropertiesInterceptors(){
        if(propertiesInterceptor == null){
            synchronized (PropertiesInterceptorFactory.class){
                try{
                    List<String> className = new ArrayList<>(LightbendConfigLoader.get().getStringList("repository.interceptors.properties"));
                    if(className.size() == 0) {
                        logger.info("No interceptors for properties defined, will use default handling");
                    }
                    ArrayList<Class<? extends PropertiesInterceptor>> clazz = className.stream().map((String className1) -> {
                        try {
                            return Class.forName(className1);
                        } catch (ClassNotFoundException e) {
                            throw new RuntimeException(e);
                        }
                    }).collect(Collectors.toCollection((Supplier<ArrayList>) ArrayList::new));
                    clazz.add(0, PropertiesInterceptorPermissions.class);
                    propertiesInterceptor = clazz.stream().map((c) -> {
                        try {
                            return c.newInstance();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }).collect(Collectors.toList());
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
        propertiesContext.setSource(CallSourceHelper.getCallSource());
        return propertiesContext;
    }
}
