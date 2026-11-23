package org.edu_sharing.service.statusreport.model.v1;

import lombok.Builder;
import lombok.Data;

/**
 * a single versioned part of the installation: the repository itself, a plugin or a connected system
 * fields that are unknown are sent as null
 */
@Data
@Builder
public class Component {
    public static final String TYPE_REPOSITORY = "repository";
    public static final String TYPE_PLUGIN = "plugin";
    public static final String TYPE_CONNECTOR = "connector";

    /**
     * repository, plugin or connector
     */
    private String type;
    /**
     * connectors only: type of the application info, e.g. RENDERINGSERVICE_2, SERVICE, LMS, CONNECTOR, ltitool
     */
    private String subtype;
    /**
     * repository / plugin: name of the version file, connector: app id
     */
    private String name;
    private String version;
    private String imageTag;
    private String commit;
}
