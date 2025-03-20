package org.edu_sharing.service.nodeservice;


import com.typesafe.config.Config;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.repository.client.tools.CCConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PropertiesGetInterceptorAllowLinkDownload extends PropertiesGetInterceptorDefault {

    private static String configPath = "importer.replicationSourceDownloadAllowed";

    Logger logger = Logger.getLogger(PropertiesGetInterceptorAllowLinkDownload.class);
    List<String> replicationSourceDownloadAllowed = new ArrayList<>();
    public PropertiesGetInterceptorAllowLinkDownload() {
        Config config = LightbendConfigLoader.get();
        if(config.hasPath(configPath)){
           replicationSourceDownloadAllowed = config.getStringList(configPath);
        }
        if(replicationSourceDownloadAllowed.isEmpty()){
            logger.warn("no replicationsource's allowed.");
        }
    }

    @Override
    public Map<String, Object> beforeDeliverProperties(PropertiesContext context) {
        String replicationSource = (String)context.getProperties().get(CCConstants.CCM_PROP_IO_REPLICATIONSOURCE);
        if(replicationSource != null &&
                replicationSourceDownloadAllowed.contains(replicationSource) &&
                context.getProperties().containsKey(CCConstants.DOWNLOADURL)){
            context.getProperties().put(CCConstants.VIRT_PROP_LINK_DOWNLOAD_ALLOWED,"true");
        }
        return context.getProperties();
    }
}
