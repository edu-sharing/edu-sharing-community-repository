package org.edu_sharing.alfresco.service.config.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlElement;
import java.io.Serializable;
import java.util.List;

public class Register implements Serializable {
    @Schema(description = "Whether local registration service is active (default: true)")
    @XmlElement public Boolean local;
    @Schema(description = "Whether local password recovery function is active")
    @XmlElement public Boolean recoverPassword;
    @Schema(description = "URL to custom registration page (used if local=false)")
    @XmlElement public String loginUrl;
    @Schema(description = "URL to custom password recovery page (used if local=false)")
    @XmlElement public String recoverUrl;
    @Schema(description = "Safe/alternative URL for password recovery")
    @XmlElement public String recoverUrlSafe;
    @Schema(description = "Required registration fields: firstName, lastName, organization")
    @XmlElement public List<String> requiredFields;

}
