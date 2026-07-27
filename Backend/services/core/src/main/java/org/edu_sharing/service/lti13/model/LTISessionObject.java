package org.edu_sharing.service.lti13.model;

import java.io.Serializable;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

/**
 * contains the information needed by later processes (deeplink, resourcelink)
 * keep in session
 */
@Getter
@Setter
public class LTISessionObject implements Serializable {

    private String eduSharingAppId;

    private String messageType;

    private Map deepLinkingSettings;

    private String nonce;

    private String iss;

    private String deploymentId;

    /**
     * i.e. lms courseId
     */
    private String contextId;

    /**
     * i.e. lms course title (from the LTI context claim)
     */
    private String contextTitle;
}
