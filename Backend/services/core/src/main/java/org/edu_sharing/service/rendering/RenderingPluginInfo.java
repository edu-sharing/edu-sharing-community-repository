package org.edu_sharing.service.rendering;

import lombok.RequiredArgsConstructor;
import org.edu_sharing.alfresco.lightbend.LightbendConfigCache;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.restservices.about.v1.model.PluginInfo;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RenderingPluginInfo implements PluginInfo {

    public static final String RENDERING_SERVICE_1 = "rendering-service-1";
    public static final String RENDERING_SERVICE_2 = "rendering-service-2";

    private final LightbendConfigLoader lightbendConfigLoader;

    @Override
    public String getId() {
        ApplicationInfo renderingService1 = ApplicationInfoList.getRenderService();
        boolean forceRenderingSerivce1 = lightbendConfigLoader.getConfig().getBoolean("rendering.forceRenderingService1");
        if(forceRenderingSerivce1 && renderingService1 != null) {
            return RENDERING_SERVICE_1;
        }

        ApplicationInfo renderingService2 = ApplicationInfoList.getRenderingService2();
        if(renderingService2 != null) {
            return RENDERING_SERVICE_2;
        }
        return RENDERING_SERVICE_1;
    }
}
