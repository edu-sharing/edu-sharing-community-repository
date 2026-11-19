package org.edu_sharing.service.share;

import com.typesafe.config.Config;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.alfresco.policy.NodeCustomizationPolicies;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.springframework.stereotype.Component;

/**
 * Provides the default actions (view in browser / download) of a link share for the current
 * edu-sharing context. They apply to new shares and to shares without a stored choice. The defaults live in {@code repository.sharing.link}; a context may override
 * single values via {@code repository.context.<id>.repository.sharing.link}.
 */
@Component
@RequiredArgsConstructor
public class ShareLinkConfigProvider {

    public static final String SHARING_LINK_CONFIG_PATH = "repository.sharing.link";
    public static final String CONTEXT_CONFIG_PATH = "repository.context";
    public static final String VIEW = "view";
    public static final String DOWNLOAD = "download";

    /**
     * Default actions of a link share
     */
    public record Defaults(boolean view, boolean download) {
    }

    private final LightbendConfigLoader configLoader;

    public Defaults getCurrentDefaults() {
        return getDefaults(NodeCustomizationPolicies.getEduSharingContext());
    }

    public Defaults getDefaults(String context) {
        Config rootConfig = configLoader.getConfig();
        boolean view = readBoolean(rootConfig, SHARING_LINK_CONFIG_PATH, VIEW, true);
        boolean download = readBoolean(rootConfig, SHARING_LINK_CONFIG_PATH, DOWNLOAD, true);
        if (StringUtils.isNotBlank(context) && !CCConstants.EDUCONTEXT_DEFAULT.equals(context)) {
            String contextPath = getContextConfigPath(context);
            view = readBoolean(rootConfig, contextPath, VIEW, view);
            download = readBoolean(rootConfig, contextPath, DOWNLOAD, download);
        }
        if (!view && !download) {
            // an unusable link makes no sense, the misconfiguration must not lock out the users
            return new Defaults(false, true);
        }
        return new Defaults(view, download);
    }

    private static boolean readBoolean(Config rootConfig, String path, String key, boolean fallback) {
        String fullPath = path + "." + key;
        return rootConfig.hasPath(fullPath) ? rootConfig.getBoolean(fullPath) : fallback;
    }

    private static String getContextConfigPath(String context) {
        return String.join(".",
                CONTEXT_CONFIG_PATH,
                context.contains(".") ? String.format("\"%s\"", context) : context,
                SHARING_LINK_CONFIG_PATH);
    }
}
