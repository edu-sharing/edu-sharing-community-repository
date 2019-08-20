package org.edu_sharing.metadataset.v2;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MetadataQueryPreprocessor {
    /**
     * runs a preprocessor defined in the paramter for a given value
     * if the parameter does not require any preoprocessor, the value will be returned
     */
    public static String run(MetadataQueryParameter parameter,String valueIn) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        if(parameter.getPreprocessor()==null)
            return valueIn;
        Method method=MetadataQueryPreprocessor.class.getDeclaredMethod(parameter.getPreprocessor(),String.class);
        return (String) method.invoke(null,valueIn);
    }

    private static String node_path(String value){
        return AuthenticationUtil.runAsSystem(()-> {
            ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
            ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
            return serviceRegistry.getNodeService().getPath(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, value)).toPrefixString(serviceRegistry.getNamespaceService());
        });
    }
}
