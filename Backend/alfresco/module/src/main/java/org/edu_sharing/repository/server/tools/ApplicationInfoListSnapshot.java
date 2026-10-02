package org.edu_sharing.repository.server.tools;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable state of all application infos. It is always built completely and then published as a whole by
 * {@link ApplicationInfoList}, so readers never see a partially filled state.
 */
record ApplicationInfoListSnapshot(Map<String, ApplicationInfo> appInfos,
                                   ApplicationInfo appInfoHome,
                                   ApplicationInfo appInfoRenderingService2,
                                   List<ApplicationInfo> appInfosLtiTool) {

    private static final Log logger = LogFactory.getLog(ApplicationInfoListSnapshot.class);

    static final ApplicationInfoListSnapshot EMPTY = new ApplicationInfoListSnapshot(Map.of(), null, null, List.of());

    ApplicationInfoListSnapshot {
        appInfos = Map.copyOf(appInfos);
        appInfosLtiTool = List.copyOf(appInfosLtiTool);
    }

    /**
     * reads the repository registry and all application files listed in it
     *
     * @return the new snapshot or null if the registry could not be read or no application could be loaded
     */
    static ApplicationInfoListSnapshot build() {
        String[] appFileArray;
        try {
            ApplicationInfo registry = new ApplicationInfo("ccapp-registry.properties.xml");
            String repStr = registry.getString("applicationfiles", null, false);
            if (repStr == null || repStr.trim().isEmpty()) {
                logger.error("Repository Registry config is undefined or empty");
                return null;
            }
            appFileArray = repStr.split(",");
            if (appFileArray.length == 0) {
                logger.error("Repository Registry config is empty");
                return null;
            }
        } catch (Exception e) {
            logger.error("Could not find Repository Registry", e);
            return null;
        }

        Map<String, ApplicationInfo> appInfos = new HashMap<>();
        ApplicationInfo appInfoHome = null;
        ApplicationInfo appInfoRenderingService2 = null;
        List<ApplicationInfo> appInfosLtiTool = new ArrayList<>();
        for (String appFile : appFileArray) {
            logger.debug("appFile:" + appFile);
            appFile = appFile.trim();
            if (appFile.isEmpty()) {
                logger.error("found empty value in Repository Registry");
                continue;
            }

            try {
                ApplicationInfo repInfo = new ApplicationInfo(appFile);
                if (repInfo.getAppId() == null) {
                    logger.error("application file " + appFile + " has no appid, skipping it");
                    continue;
                }
                logger.debug("put:" + appFile + " " + repInfo);
                appInfos.put(repInfo.getAppId(), repInfo);
                if (repInfo.ishomeNode()) {
                    appInfoHome = repInfo;
                }
                if (ApplicationInfo.TYPE_RENDERSERVICE_2.equals(repInfo.getType())) {
                    appInfoRenderingService2 = repInfo;
                }
                if (repInfo.isLtiTool()) {
                    appInfosLtiTool.add(repInfo);
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }

        if (appInfos.isEmpty()) {
            logger.error("Repository Registry lists no loadable application file");
            return null;
        }
        return new ApplicationInfoListSnapshot(appInfos, appInfoHome, appInfoRenderingService2, appInfosLtiTool);
    }
}
