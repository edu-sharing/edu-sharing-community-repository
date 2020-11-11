package org.edu_sharing.service.oai;

import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.repository.server.exporter.OAILOMExporter;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class OAIExporterFactory {

    static Logger logger = Logger.getLogger(OAIExporterFactory.class);

    public static OAILOMExporter getOAILOMExporter(){
        OAILOMExporter exporter = null;
        String oaiClass = LightbendConfigLoader.get().getString("exporter.oai.lom.class");
        if(oaiClass == null || oaiClass.trim().equals("")){
            logger.error("no class defined in config");
            oaiClass = "org.edu_sharing.repository.server.exporter.OAILOMExporter";
        }

        Class<?> cl = null;
        try {
            cl = Class.forName(oaiClass);
            Constructor<?> cons = cl.getConstructor();
            exporter = (OAILOMExporter)cons.newInstance();
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            logger.error(e.getMessage(),e);
        }
        return exporter;
    }
}
